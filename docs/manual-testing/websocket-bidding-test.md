# WebSocket Bidding Test — Two Browsers, Two Buyers, Real-Time Outbidding

**Tool:** Playwright MCP (`@playwright/mcp`)
**Browsers:** Google Chrome (Buyer 1) + Mozilla Firefox (Buyer 2)
**Purpose:** Verify that two buyers in different browsers can outbid each other in real time, that WebSocket messages are correct, and that the Vue UI reacts properly to each incoming bid.

---

## Architecture Under Test

```
Buyer 1 (Chrome)                                              Buyer 2 (Firefox)
     │                                                              │
     │  POST /auctions/{id}/bids ──────────┐   ┌── POST /bids ─────│
     ▼                                     ▼   ▼                    ▼
                          ┌─────────────────────────┐
                          │     Auction-Engine       │
                          │  event-sourced bidding   │
                          └─────────┬───────────────┘
                                    │ outbox → NATS
                          ┌─────────▼───────────────┐
                          │   Gateway (WebSocket)    │
                          │  BidEventForwarder       │
                          └────┬────────────────┬───┘
                  ws broadcast │                │ ws broadcast
                               ▼                ▼
                          Buyer 1            Buyer 2
                         (Chrome)           (Firefox)
                       UI updates          UI updates
```

---

## Prerequisites

### 1. Infrastructure Running

```bash
docker compose -f docker/compose/docker-compose-infrastructure.yaml --env-file docker/compose/.env ps
```

Required healthy: `gateway-service`, `auction-engine`, `catalog-service`, `nats`, `redis`, `postgres`, `keycloak`.

### 2. Frontend Running

```bash
cd frontend/buyer-web && npm run dev   # Port 3000
```

### 3. Playwright MCP Active

The `@playwright/mcp` server must be running. Verify:

```
mcp__playwright__browser_tabs  action=list
```

### 4. Gateway BidEventForwarder Healthy

```bash
docker logs auction-platform-gateway-service 2>&1 | grep "BidEventForwarder"
```

Must show `Starting BidEventForwarder` with **no** `JetStreamApiException` after it.

If broken, fix:
```bash
docker run --rm --network auction-platform-network natsio/nats-box:0.14.5 \
  nats consumer rm AUCTION gateway-auction-consumer -s nats://nats:4222 --force
docker restart auction-platform-gateway-service
```

---

## Test Data Setup (bash)

Run these in a terminal before starting the browser test.

### Step 1 — Tokens

```bash
SELLER_TOKEN=$(curl -s -X POST "http://localhost:8180/realms/auction-platform/protocol/openid-connect/token" \
  -d "client_id=buyer-web" -d "grant_type=password" \
  -d "username=seller@test.com" -d "password=password123" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

ADMIN_TOKEN=$(curl -s -X POST "http://localhost:8180/realms/auction-platform/protocol/openid-connect/token" \
  -d "client_id=buyer-web" -d "grant_type=password" \
  -d "username=admin@test.com" -d "password=password123" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

### Step 2 — Create, Submit & Approve Lot

```bash
LOT_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/v1/lots" \
  -H "Authorization: Bearer $SELLER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "WS Test - CNC Milling Machine",
    "description": "Two-buyer WebSocket bidding test",
    "brand": "troostwijk",
    "categoryId": "10000000-0000-0000-0000-000000000001",
    "condition": "GOOD",
    "locationCity": "Amsterdam",
    "locationCountry": "NL",
    "estimatedValue": 10000.00,
    "currency": "EUR"
  }')
LOT_ID=$(echo "$LOT_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

curl -s -X POST "http://localhost:8080/api/v1/lots/$LOT_ID/submit" \
  -H "Authorization: Bearer $SELLER_TOKEN" > /dev/null

curl -s -X POST "http://localhost:8080/api/v1/lots/$LOT_ID/approve" \
  -H "Authorization: Bearer $ADMIN_TOKEN" > /dev/null

echo "LOT_ID=$LOT_ID"
```

### Step 3 — Create Active Auction

```bash
SELLER_ID="00000000-0000-0000-0000-000000000002"
START=$(date -u -d "+1 minute" +%Y-%m-%dT%H:%M:%SZ)
END=$(date -u -d "+30 minutes" +%Y-%m-%dT%H:%M:%SZ)

AUCTION_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/v1/auctions" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"lotId\": \"$LOT_ID\",
    \"brand\": \"troostwijk\",
    \"sellerId\": \"$SELLER_ID\",
    \"startingBid\": 1000.00,
    \"currency\": \"EUR\",
    \"reservePrice\": 5000.00,
    \"startTime\": \"$START\",
    \"endTime\": \"$END\"
  }")
AUCTION_ID=$(echo "$AUCTION_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['auctionId'])")
echo "AUCTION_ID=$AUCTION_ID"
```

Verify: `"status": "ACTIVE"` in the response.

---

## Browser Setup

We use Playwright MCP's primary Chromium as **Buyer 1 (Chrome)** and launch a
separate Firefox browser context via `browser_run_code` as **Buyer 2 (Firefox)**.

### Step 4 — Launch Firefox Context for Buyer 2

This creates a persistent Firefox browser and page that we interact with
throughout the test via `browser_run_code`. The Firefox page object is stored
in a global so subsequent calls can reuse it.

```
mcp__playwright__browser_run_code
  code: async (page) => {
    const { firefox } = await import('playwright');
    const browser = await firefox.launch({ headless: false });
    const context = await browser.newContext();
    const firefoxPage = await context.newPage();
    // Store globally so later run_code calls can access it
    globalThis.__firefoxPage = firefoxPage;
    globalThis.__firefoxBrowser = browser;
    await firefoxPage.goto('http://localhost:3000');
    return { browser: 'firefox', url: firefoxPage.url() };
  }
```

> **Note:** If `import('playwright')` fails, Firefox may not be installed.
> Run `npx playwright install firefox` first. If the MCP sandbox blocks
> dynamic imports, use two separate browser contexts in the same Chromium
> instance instead — one per buyer (see Fallback section at the bottom).

**Fallback — Two Chromium Contexts (if Firefox unavailable):**

```
mcp__playwright__browser_run_code
  code: async (page) => {
    const browser = page.context().browser();
    const ctx2 = await browser.newContext();
    const page2 = await ctx2.newPage();
    globalThis.__buyer2Page = page2;
    await page2.goto('http://localhost:3000');
    return { browser: 'chromium-context-2', url: page2.url() };
  }
```

Each browser context has isolated cookies/storage — equivalent to separate browsers.

### Step 5 — Log In Buyer 1 (Chrome / Playwright primary)

```
mcp__playwright__browser_navigate  url=http://localhost:3000
mcp__playwright__browser_snapshot
```

Click "Log In", fill Keycloak form:

```
mcp__playwright__browser_click  ref=<log_in_ref>  element="Log In button"
mcp__playwright__browser_wait_for  text="Sign in to your account"
mcp__playwright__browser_snapshot
mcp__playwright__browser_fill_form  fields=[
  {"name":"Email","type":"textbox","ref":"<email_ref>","value":"buyer@test.com"},
  {"name":"Password","type":"textbox","ref":"<password_ref>","value":"password123"}
]
mcp__playwright__browser_click  ref=<sign_in_ref>  element="Sign In button"
mcp__playwright__browser_wait_for  text="Test Buyer"
```

### Step 6 — Log In Buyer 2 (Firefox / run_code)

```
mcp__playwright__browser_run_code
  code: async (page) => {
    const fp = globalThis.__firefoxPage || globalThis.__buyer2Page;

    // Click Log In
    await fp.getByRole('button', { name: 'Log In' }).click();
    await fp.waitForURL(/keycloak/);

    // Fill Keycloak form
    await fp.locator('#username').fill('buyer2@test.com');
    await fp.locator('#password').fill('password123');
    await fp.getByRole('button', { name: 'Sign In' }).click();

    // Wait for redirect
    await fp.waitForURL(/localhost:3000/);
    await fp.waitForTimeout(2000);

    // Check logged in
    const text = await fp.textContent('body');
    return {
      loggedIn: text.includes('Test Buyer 2') || text.includes('buyer2'),
      url: fp.url()
    };
  }
```

### Step 7 — Both Buyers Navigate to Lot Detail Page

**Buyer 1 (Chrome):**
```
mcp__playwright__browser_navigate  url=http://localhost:3000/lots/<LOT_ID>
mcp__playwright__browser_wait_for  text="Current bid"
mcp__playwright__browser_take_screenshot
  filename=tests/test-screenshots/ws-buyer1-before.png
  type=png
```

**Buyer 2 (Firefox):**
```
mcp__playwright__browser_run_code
  code: async (page) => {
    const fp = globalThis.__firefoxPage || globalThis.__buyer2Page;
    await fp.goto('http://localhost:3000/lots/<LOT_ID>');
    await fp.waitForSelector('text=/Current bid|Trenutna ponudba/');
    await fp.screenshot({ path: 'tests/test-screenshots/ws-buyer2-before.png' });
    return { url: fp.url(), title: await fp.title() };
  }
```

### Step 8 — Verify Both WebSocket Connections

```bash
docker logs auction-platform-gateway-service 2>&1 \
  | grep "WebSocket session opened" | tail -5
```

**Expected:** Two sessions for the same `auctionId`:
```
WebSocket session opened [sessionId=..., userId=...0001, auctionId=<AUCTION_ID>]
WebSocket session opened [sessionId=..., userId=...0007, auctionId=<AUCTION_ID>]
```

Also check the session count:
```bash
docker logs auction-platform-gateway-service 2>&1 \
  | grep "Broadcasting\|Active:" | tail -3
```

**Expected:** `Active: users=..., auctions=1, total sessions=...` with at least 2 sessions.

---

## Bidding War — Outbidding Each Other

### Round 1 — Buyer 1 Bids First (Chrome)

**Record pre-bid state on both browsers:**

```
mcp__playwright__browser_evaluate
  function: () => {
    const app = document.querySelector('#__nuxt')?.__vue_app__;
    const s = app?.config?.globalProperties?.$pinia?._s?.get('auction');
    return { currentBid: s?.currentAuction?.currentBid, bidCount: s?.currentAuction?.bidCount };
  }
```

**Place bid via the UI — click the bid button on Buyer 1's page:**

```
mcp__playwright__browser_snapshot
mcp__playwright__browser_click  ref=<place_bid_button_ref>  element="Place bid button"
mcp__playwright__browser_wait_for  time=3
```

**Verify Buyer 2 (Firefox) received the bid via WebSocket — NO reload:**

```
mcp__playwright__browser_run_code
  code: async (page) => {
    const fp = globalThis.__firefoxPage || globalThis.__buyer2Page;
    // Read Pinia store to see if bid arrived via WebSocket
    const result = await fp.evaluate(() => {
      const app = document.querySelector('#__nuxt')?.__vue_app__;
      const s = app?.config?.globalProperties?.$pinia?._s?.get('auction');
      return {
        currentBid: s?.currentAuction?.currentBid,
        bidCount: s?.currentAuction?.bidCount,
        latestBid: s?.bids?.[0],
      };
    });
    return result;
  }
```

**Pass criteria:**
- `currentBid` updated to Buyer 1's bid amount
- `latestBid.bidderId` is masked (`0000****0001`)
- `latestBid.amount` > 0 (not null/0)

**Screenshot Buyer 2 to confirm UI reacted:**

```
mcp__playwright__browser_run_code
  code: async (page) => {
    const fp = globalThis.__firefoxPage || globalThis.__buyer2Page;
    await fp.screenshot({ path: 'tests/test-screenshots/ws-round1-buyer2-after.png' });
    return 'screenshot saved';
  }
```

### Round 2 — Buyer 2 Outbids (Firefox)

**Place bid from Firefox via UI:**

```
mcp__playwright__browser_run_code
  code: async (page) => {
    const fp = globalThis.__firefoxPage || globalThis.__buyer2Page;

    // Click the place-bid button
    const bidButton = fp.locator('button:has-text("Place bid"), button:has-text("Oddaj ponudbo")');
    await bidButton.click();
    await fp.waitForTimeout(3000);

    // Read store state after bidding
    const result = await fp.evaluate(() => {
      const app = document.querySelector('#__nuxt')?.__vue_app__;
      const s = app?.config?.globalProperties?.$pinia?._s?.get('auction');
      return {
        currentBid: s?.currentAuction?.currentBid,
        bidCount: s?.currentAuction?.bidCount,
      };
    });
    await fp.screenshot({ path: 'tests/test-screenshots/ws-round2-buyer2-bid.png' });
    return result;
  }
```

**Verify Buyer 1 (Chrome) received the bid — NO reload:**

```
mcp__playwright__browser_evaluate
  function: () => {
    const app = document.querySelector('#__nuxt')?.__vue_app__;
    const s = app?.config?.globalProperties?.$pinia?._s?.get('auction');
    return {
      currentBid: s?.currentAuction?.currentBid,
      bidCount: s?.currentAuction?.bidCount,
      latestBid: s?.bids?.[0],
    };
  }
```

```
mcp__playwright__browser_take_screenshot
  filename=tests/test-screenshots/ws-round2-buyer1-after.png
  type=png
```

**Pass criteria:**
- `currentBid` on Buyer 1 matches Buyer 2's bid
- `latestBid.bidderId` shows masked buyer2 ID (`0000****0007`)

### Round 3 — Buyer 1 Outbids Back (Chrome)

```
mcp__playwright__browser_snapshot
mcp__playwright__browser_click  ref=<place_bid_button_ref>  element="Place bid button"
mcp__playwright__browser_wait_for  time=3
```

Verify on Firefox:

```
mcp__playwright__browser_run_code
  code: async (page) => {
    const fp = globalThis.__firefoxPage || globalThis.__buyer2Page;
    const result = await fp.evaluate(() => {
      const app = document.querySelector('#__nuxt')?.__vue_app__;
      const s = app?.config?.globalProperties?.$pinia?._s?.get('auction');
      return {
        currentBid: s?.currentAuction?.currentBid,
        bidCount: s?.currentAuction?.bidCount,
        allBids: s?.bids?.map(b => ({ amount: b.amount, bidderId: b.bidderId })),
      };
    });
    await fp.screenshot({ path: 'tests/test-screenshots/ws-round3-buyer2-after.png' });
    return result;
  }
```

### Round 4+ — Continue alternating until satisfied.

---

## WebSocket Message Inspection

### Inspect Raw WS Messages on Chrome (Buyer 1)

Install a WebSocket interceptor to capture all raw messages:

```
mcp__playwright__browser_evaluate
  function: () => {
    window.__wsMessages = [];
    const OrigWS = window.WebSocket;
    window.WebSocket = function(url, protocols) {
      const ws = protocols ? new OrigWS(url, protocols) : new OrigWS(url);
      ws.addEventListener('message', (e) => {
        try {
          const parsed = JSON.parse(e.data);
          window.__wsMessages.push({
            time: new Date().toISOString(),
            type: parsed.type || parsed.event,
            auctionId: parsed.auctionId,
            data: parsed.data,
            raw: e.data.substring(0, 500)
          });
        } catch { window.__wsMessages.push({ time: new Date().toISOString(), raw: e.data?.substring(0, 200) }); }
      });
      ws.addEventListener('open', () => window.__wsMessages.push({ time: new Date().toISOString(), type: 'WS_OPEN', url }));
      ws.addEventListener('close', (e) => window.__wsMessages.push({ time: new Date().toISOString(), type: 'WS_CLOSE', code: e.code, reason: e.reason }));
      return ws;
    };
    window.WebSocket.prototype = OrigWS.prototype;
    Object.assign(window.WebSocket, OrigWS);
    return 'interceptor installed - reload page to capture from connection start';
  }
```

After reloading the page and waiting for some bids, read captured messages:

```
mcp__playwright__browser_evaluate
  function: () => window.__wsMessages || []
```

### Inspect Raw WS Messages on Firefox (Buyer 2)

```
mcp__playwright__browser_run_code
  code: async (page) => {
    const fp = globalThis.__firefoxPage || globalThis.__buyer2Page;
    const messages = await fp.evaluate(() => window.__wsMessages || []);
    return messages;
  }
```

### What to Check in Each Message

**1. `connected` message (on page load):**
```json
{
  "type": "connected",
  "auctionId": "<AUCTION_ID>",
  "userId": "00000000-0000-0000-0000-00000000000X",
  "serverTime": "2026-...",
  "heartbeatIntervalMs": 30000
}
```
- Verify `auctionId` matches the auction you created
- Verify `userId` matches the logged-in buyer's UUID
- Verify `heartbeatIntervalMs` is 30000

**2. `bid_placed` message (on each bid):**
```json
{
  "type": "bid_placed",
  "auctionId": "<AUCTION_ID>",
  "data": {
    "bidId": "019ce...",
    "bidderId": "0000****0007",
    "amount": "1100.00",
    "currency": "EUR",
    "isProxy": false,
    "bidCount": 0,
    "timestamp": "2026-..."
  },
  "serverTime": "2026-..."
}
```
- `type` MUST be `"bid_placed"` (not `"bid.placed"` or other)
- `auctionId` matches the test auction
- `data.amount` is a non-null string representation of the bid (e.g. `"1100.00"`)
- `data.bidderId` is masked: format `XXXX****XXXX` (never full UUID)
- `data.currency` is `"EUR"`
- `data.timestamp` is an ISO-8601 UTC timestamp
- `serverTime` is the gateway's clock (for latency measurement)

**3. `heartbeat` message (every 30s):**
```json
{
  "type": "heartbeat",
  "serverTime": "2026-...",
  "auctionId": "<AUCTION_ID>",
  "activeSessions": 2
}
```
- `activeSessions` should be >= 2 when both buyers are connected

---

## UI Reactivity Checks

After each WebSocket `bid_placed` delivery, verify the following UI elements
update **without page reload**:

### Check 1 — Current Bid Display

```
mcp__playwright__browser_snapshot
```

Look for the "Current bid" / "Trenutna ponudba" section. The displayed amount
must match the bid from the WebSocket message.

### Check 2 — Bid Count

The "X bids" / "X ponudb" text must increment after each bid.

### Check 3 — Minimum Bid Recalculation

The bid input field's minimum value and the "Place bid €X" / "Oddaj ponudbo €X"
button text should update to `currentBid + minIncrement`.

Increment tiers:
| Current bid | Increment |
|-------------|-----------|
| < €100 | €1 |
| €100–499 | €5 |
| €500–999 | €10 |
| €1,000–4,999 | €25 |
| €5,000–9,999 | €50 |
| €10,000–49,999 | €100 |
| €50,000–99,999 | €250 |
| ≥ €100,000 | €500 |

### Check 4 — Bid History Accordion

Click "Bid history (N)" to expand. Each bid should appear with:
- Masked bidder label
- Amount
- Timestamp

```
mcp__playwright__browser_click  ref=<bid_history_ref>  element="Bid history accordion"
mcp__playwright__browser_snapshot
```

### Check 5 — Countdown Timer Still Running

The auction timer must continue counting down — it should NOT reset or freeze
after a WebSocket message.

### Check 6 — Cross-Browser Pinia Store Consistency

After 3+ rounds of outbidding, read both stores and compare:

**Chrome:**
```
mcp__playwright__browser_evaluate
  function: () => {
    const s = document.querySelector('#__nuxt')?.__vue_app__?.config?.globalProperties?.$pinia?._s?.get('auction');
    return { currentBid: s?.currentAuction?.currentBid, bidCount: s?.currentAuction?.bidCount, bids: s?.bids?.length };
  }
```

**Firefox:**
```
mcp__playwright__browser_run_code
  code: async (page) => {
    const fp = globalThis.__firefoxPage || globalThis.__buyer2Page;
    return await fp.evaluate(() => {
      const s = document.querySelector('#__nuxt')?.__vue_app__?.config?.globalProperties?.$pinia?._s?.get('auction');
      return { currentBid: s?.currentAuction?.currentBid, bidCount: s?.currentAuction?.bidCount, bids: s?.bids?.length };
    });
  }
```

**Both must show the same `currentBid`.** The `bids` array length may differ
(it only counts WS-delivered bids since page load, not historical ones).

---

## Gateway Log Verification

After the bidding war, check the full pipeline:

```bash
# 1. Outbox published events
docker logs auction-platform-auction-engine 2>&1 | grep "outbox\|published" | tail -10

# 2. NATS → Gateway forwarding
docker logs auction-platform-gateway-service 2>&1 | grep "Forwarded bid_placed" | tail -10

# 3. WebSocket broadcast
docker logs auction-platform-gateway-service 2>&1 | grep "Broadcasting to" | tail -10

# 4. Session count (should show 2 for both buyers)
docker logs auction-platform-gateway-service 2>&1 | grep "Active:" | tail -5
```

---

## Cleanup

Close Firefox:

```
mcp__playwright__browser_run_code
  code: async (page) => {
    if (globalThis.__firefoxBrowser) {
      await globalThis.__firefoxBrowser.close();
      globalThis.__firefoxBrowser = null;
      globalThis.__firefoxPage = null;
    }
    if (globalThis.__buyer2Page) {
      await globalThis.__buyer2Page.context().close();
      globalThis.__buyer2Page = null;
    }
    return 'cleaned up';
  }
```

---

## Verification Checklist

| # | Check | Where | Expected |
|---|-------|-------|----------|
| 1 | Both WS connections open | Gateway log: 2x `session opened` | Both userIds, same auctionId |
| 2 | `connected` message correct | WS interceptor | Has auctionId, userId, heartbeat |
| 3 | `bid_placed` has amount | WS interceptor `data.amount` | Non-null, matches bid |
| 4 | Bidder ID masked | WS interceptor `data.bidderId` | `XXXX****XXXX` format |
| 5 | Broadcast hits both | Gateway log `Broadcasting to 2` | N=2 |
| 6 | Chrome store updates | `evaluate` Pinia store | `currentBid` matches |
| 7 | Firefox store updates | `run_code` evaluate | `currentBid` matches |
| 8 | UI shows new bid (Chrome) | `take_screenshot` | Amount visible in bid panel |
| 9 | UI shows new bid (Firefox) | `run_code` screenshot | Amount visible in bid panel |
| 10 | Min bid recalculates | Snapshot: bid button text | `currentBid + increment` |
| 11 | Bid history populates | Expand accordion | Entries with masked IDs |
| 12 | Heartbeat keeps alive | Gateway log: `ping` messages | Every ~30s per session |
| 13 | Both stores consistent | Compare `currentBid` | Identical after same round |

---

## Troubleshooting

### Firefox launch fails in `browser_run_code`

Playwright's Firefox binary may not be installed. Fix:
```bash
npx playwright install firefox
```

If the MCP sandbox blocks `import('playwright')`, use the **fallback approach**:
two Chromium browser contexts via `browser.newContext()` (see Step 4 fallback).

### No WebSocket connection on page load

1. `useWebSocket.connect()` must receive the **auctionId** (not lotId)
2. Auth token must exist in `authStore.token`
3. Runtime config `wsBaseUrl` = `ws://localhost:8080/ws`

### WebSocket connects but no `bid_placed` events

1. Gateway's `BidEventForwarder` may have failed to start — check for
   `JetStreamApiException` in gateway logs
2. Stale NATS consumer — delete with `nats consumer rm AUCTION gateway-auction-consumer --force`
3. Outbox poller in auction-engine may be stuck — check `docker logs auction-platform-auction-engine`

### `bid_placed` arrives but `amount` is null/0

Gateway's `BidEventForwarder` must read `node.path("bidAmount")` — the domain
event field is `bidAmount`, not `amount`.

### Event handlers don't fire (store not updating)

Event name mapping between gateway and frontend:

| Gateway sends | Frontend expects |
|---------------|-----------------|
| `bid_placed` | `bid_placed` |
| `lot_extended` | `auction_extended` |
| `lot_closed` | `auction_closed` |
| `connected` | (ignored) |
| `heartbeat` | (ignored) |

The frontend reads `message.type` (not `message.event`).

### Keycloak SSO bleed between contexts

Keycloak shares SSO at the IdP level. If the second login auto-authenticates
as buyer1, either:
- Use the fallback context approach (separate `browser.newContext()`)
- Clear Keycloak cookies in the second context before login
- Log in buyer1 first, then buyer2 — order matters
