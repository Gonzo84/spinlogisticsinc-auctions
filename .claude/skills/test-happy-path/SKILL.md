---
name: test-happy-path
description: Run happy path tests for a given role (buyer/seller/admin/broker/demo/all) using Playwright MCP, following happy path docs
user-invocable: true
disable-model-invocation: false
---

# Happy Path Testing Skill

Run browser-based happy path tests for the SPC Auction Platform. Uses Playwright MCP for browser roles and Bash curl for the broker API-only path.

**Argument:** `$ARGUMENTS` — one of `buyer`, `seller`, `admin`, `broker`, `demo`, or `all` (default: `all`)

**Happy path documents location:** `docs/happy-paths/`
- `docs/happy-paths/HAPPY_PATHS.md` — role-based tests (buyer, seller, admin, broker)
- `docs/happy-paths/HAPPY_PATH_DEMO.md` — SPC pitch demo flow (cross-role, 8 steps: homepage, seller, admin, buyer, competitive bidding & anti-sniping, post-auction settlement & payment, compliance, CO2)

---

## Step 1: Parse Arguments

Determine which role(s) to test from `$ARGUMENTS`:

- `buyer` — run only the Buyer Happy Path (Section 1 of `docs/happy-paths/HAPPY_PATHS.md`)
- `seller` — run only the Seller Happy Path (Section 2 of `docs/happy-paths/HAPPY_PATHS.md`)
- `admin` — run only the Admin Happy Path (Section 3 of `docs/happy-paths/HAPPY_PATHS.md`)
- `broker` — run only the Broker Happy Path (Section 4 of `docs/happy-paths/HAPPY_PATHS.md`, API-only via curl)
- `demo` — run the SPC Pitch Demo Flow (`docs/happy-paths/HAPPY_PATH_DEMO.md`), steps 1-8 cross-role
- `all` or empty — run all four role-based paths in order: buyer -> seller -> admin -> broker

Store the list of roles to test. For each role, you will produce a separate report file.

When `demo` is selected, follow `docs/happy-paths/HAPPY_PATH_DEMO.md` instead of `HAPPY_PATHS.md`. The demo flow tests across all 3 portals in a single sequential flow (8 steps), producing a single report file named `tests/demo-happy-path-report-<YYYY-MM-DD>.md`.

---

## Step 2: Pre-flight — Start the Full Stack

Use the `/run-full-stack` skill to start all infrastructure, backend services, and frontends. This ensures everything is running before testing.

Then verify the required services are reachable. Use **Bash curl** to check each URL:

| Component | URL | Required For |
|-----------|-----|--------------|
| Buyer Web | http://localhost:3000 | buyer |
| Seller Portal | http://localhost:5174 | seller |
| Admin Dashboard | http://localhost:5175 | admin |
| Gateway API | http://localhost:8080/q/health | buyer, seller, admin, broker |
| Keycloak | http://localhost:8180/realms/auction-platform | all |

```bash
# Example health check
curl -sf -o /dev/null -w "%{http_code}" http://localhost:3000
```

If a required service is **not running** after `/run-full-stack`, report it and **skip that role** (mark all steps as SKIP with reason "Service not running"). Continue with other roles.

Create the screenshots output directory:
```bash
mkdir -p tests/test-screenshots
```

---

## Step 3: Read the Canonical Test Plan

Read the appropriate test plan from `docs/happy-paths/`:

- **Role-based testing** (`buyer`, `seller`, `admin`, `broker`, `all`): Read `docs/happy-paths/HAPPY_PATHS.md`. Each section (1-4) maps to a role. Follow the numbered steps and **Verify** checkpoints exactly as written.
- **Demo flow testing** (`demo`): Read `docs/happy-paths/HAPPY_PATH_DEMO.md`. Follow Steps 1-8 sequentially across all 3 portals. This tests the exact SPC pitch demo flow with SPC-specific products (containers, climate equipment, construction machinery). Steps 5-6 involve competitive bidding, anti-sniping, and post-auction payment/settlement — these use a mix of browser UI and API calls (curl).

---

## Step 4: Execute Tests Per Role

### Context Management (CRITICAL)

To prevent context window overflow during long test runs:
- **Save snapshots to files** using the `filename` parameter on `browser_snapshot` — do NOT let them render inline
- **Save screenshots to files** using the `filename` parameter on `browser_take_screenshot`
- **Only read snapshot files when needed** (e.g., to find a specific element `ref`) — don't read full snapshots for every step
- **Keep console/network checks brief** — only fetch full details for errors

### Playwright MCP Tool Reference

| Action | Tool | Key Parameters |
|--------|------|----------------|
| Navigate | `mcp__playwright__browser_navigate` | `url` |
| Accessibility snapshot | `mcp__playwright__browser_snapshot` | `filename` (optional, saves to file) |
| Screenshot | `mcp__playwright__browser_take_screenshot` | `filename`, `type` ("png"/"jpeg"), `fullPage` |
| Click element | `mcp__playwright__browser_click` | `ref` (from snapshot), `element` (description) |
| Type into input | `mcp__playwright__browser_type` | `ref`, `text`, `submit` (optional) |
| Fill form fields | `mcp__playwright__browser_fill_form` | `fields` array of `{name, type, ref, value}` |
| Select dropdown | `mcp__playwright__browser_select_option` | `ref`, `values` array |
| Press key | `mcp__playwright__browser_press_key` | `key` (e.g., "Enter", "Tab") |
| Wait for text | `mcp__playwright__browser_wait_for` | `text`, `textGone`, or `time` |
| Hover | `mcp__playwright__browser_hover` | `ref`, `element` |
| Console messages | `mcp__playwright__browser_console_messages` | `level` ("error"/"warning"/"info"/"debug") |
| Network requests | `mcp__playwright__browser_network_requests` | `includeStatic` (bool) |
| Run JS code | `mcp__playwright__browser_evaluate` | `function` (JS string) |
| Tab management | `mcp__playwright__browser_tabs` | `action` ("list"/"new"/"close"/"select"), `index` |

**Element identification:** Playwright MCP uses `ref` attributes from `browser_snapshot` output to identify elements. Always take a snapshot first, find the element's `ref`, then use it in click/type/fill actions.

### General Testing Protocol

For **each step** in the happy path:

1. **Execute** the action using the appropriate tool:
   - Browser roles (buyer/seller/admin): Use Playwright MCP tools (`browser_navigate`, `browser_snapshot`, `browser_click`, `browser_type`, `browser_fill_form`, `browser_wait_for`)
   - Broker: Use Bash curl commands as specified in `docs/happy-paths/HAPPY_PATHS.md` Section 4

2. **At every "Verify" checkpoint:**
   - Take a page snapshot via `browser_snapshot` with `filename: "tests/test-screenshots/<role>-<step>-snapshot.md"` — then Read the file to check for expected elements. This keeps raw snapshot data out of the conversation context.
   - Check console for errors via `browser_console_messages` with `level: "error"`
   - Check network for failed requests via `browser_network_requests` with `includeStatic: false` — look for 4xx/5xx status codes
   - Take a screenshot via `browser_take_screenshot` with `filename: "tests/test-screenshots/<role>-<step>-<short-name>.png"`
     - Example: `tests/test-screenshots/buyer-1.1-landing-page.png`

3. **Record the result** for each step: **PASS**, **FAIL**, **PARTIAL**, or **SKIP**
   - **PASS**: All verify checkpoints met, no console errors, no network failures
   - **PARTIAL**: Main functionality works but has console errors, network failures, or minor visual issues
   - **FAIL**: Verify checkpoint not met, page crashes, or critical functionality broken
   - **SKIP**: Precondition not met (service down, depends on failed prior step)

### ID Consistency Verification (Cross-Cutting)

> **Background:** The platform has two UUID identity spaces — catalog `lotId` and auction-engine `auctionId`. Confusing them is a recurring source of bugs. See CONVENTIONS.md section 2.4 and `docs/LOT_AUCTION_ID_AUDIT.md`.

During testing, apply these additional checks at every navigation and API interaction:

**Navigation links (buyer-web):**
- When clicking any lot card, HeroCarousel item, or notification link, verify the URL path `/lots/{id}` uses the **catalog lotId** (not the auction-engine auctionId). The lot detail page should load without 404.
- Use `browser_evaluate` to extract the current URL after navigation and compare with the lot's `catalogLotId` field if available.

**API requests (all portals):**
- When checking network requests via `browser_network_requests`, verify:
  - Bid-related requests (`/auctions/{id}/bids`, `/auctions/{id}/auto-bids`) use the **auctionId**
  - Lot-related requests (`/lots/{id}`, `/lots/{id}/submit`) use the **catalog lotId**
  - WebSocket connections use `/ws/auctions/{auctionId}` (not lotId)
- A 404 on `/auctions/{uuid}` or `/lots/{uuid}` often means the wrong ID type was used — report as a **Critical** ID consistency bug.

**Seller portal bid history:**
- When testing seller lot detail (step 2.5 or equivalent), verify the bid history API call uses `auctionId` (resolved via lot's `auctionId` field or `/auctions/by-lot/{lotId}` lookup), not the catalog `lotId` directly.

**Post-auction flow (demo steps 6.x):**
- After checkout, verify the payment record has the correct `sellerId` (a user UUID, not a lot or auction UUID)
- After settlement, verify the seller can see the settlement in their portal (wrong `sellerId` would cause it to be invisible)

**Bug severity for ID confusion:**
- Any bug where `auctionId` is used where `catalogLotId` is expected (or vice versa) is **Critical** severity
- Any silent fallback that substitutes one ID type for another is **Critical** severity

### 4a: Buyer Happy Path (Browser — http://localhost:3000)

**Test credentials:** buyer@test.com / password123

Follow `docs/happy-paths/HAPPY_PATHS.md` Section 1, steps 1.1 through 1.7:

| Step | Name | Actions |
|------|------|---------|
| 1.1 | Landing Page | Navigate to localhost:3000. Verify hero, search bar, featured lots (SPC products), navbar, footer |
| 1.2 | Authentication | Click Login -> Keycloak -> enter credentials -> verify redirect back, navbar shows user menu |
| 1.3 | Browse & Search | Use search bar (try "kontejner" or "container") -> verify results page with SPC lots, lot cards, filters |
| 1.4 | View Lot Detail | Click an SPC lot card -> verify detail page fields, bid panel |
| 1.5 | Place a Bid | Enter bid amount on an ACTIVE lot, click Place Bid -> verify confirmation |
| 1.6 | Navigation Pages | Visit My Bids, Watchlist, Profile from user menu |
| 1.7 | Logout | Click avatar -> Logout -> verify redirect, Login button reappears |

### 4b: Seller Happy Path (Browser — http://localhost:5174)

**Test credentials:** seller@test.com / password123

Follow `docs/happy-paths/HAPPY_PATHS.md` Section 2, steps 2.1 through 2.11:

| Step | Name | Actions |
|------|------|---------|
| 2.1 | Authentication | Navigate to localhost:5174 -> Keycloak login -> verify dashboard |
| 2.2 | Dashboard | Verify KPI cards, recent activity, quick actions, status overview |
| 2.3 | Create a New Lot | Fill lot form with an SPC-relevant product (e.g., "Skladiscni kontejner 10ft", "Razvlazilec Master DH 26", or any container/equipment), Location: Ljubljana SI -> verify success |
| 2.4 | Submit for Review | Click Submit for Review -> verify status changes to PENDING_REVIEW |
| 2.5 | My Lots Page | Verify paginated list, lot details, pagination |
| 2.6 | Edit a Lot | Edit a field, save -> verify changes reflected |
| 2.7 | Settlements | Navigate to Settlements -> verify page loads |
| 2.8 | Analytics | Navigate to Analytics -> verify page loads |
| 2.9 | CO2 Report | Navigate to CO2 Report -> verify page loads |
| 2.10 | Profile | Navigate to Profile -> verify form tabs |
| 2.11 | Logout | Logout -> verify session ended |

### 4c: Admin Happy Path (Browser — http://localhost:5175)

**Test credentials:** admin@test.com / password123

Follow `docs/happy-paths/HAPPY_PATHS.md` Section 3, steps 3.1 through 3.10:

| Step | Name | Actions |
|------|------|---------|
| 3.1 | Authentication | Navigate to localhost:5175 -> Keycloak login -> verify dashboard |
| 3.2 | Dashboard Overview | Verify sidebar navigation links |
| 3.3 | Lot Approval | Navigate to Lot Approval -> approve a pending lot -> verify status change |
| 3.3b | Create Auction | Navigate to Auctions -> Create Auction -> select approved lot -> set times -> submit -> verify lot becomes ACTIVE |
| 3.4 | Auction Management | Navigate to Auctions -> verify table with created auction, filters, Create Auction button |
| 3.5 | User Management | Navigate to Users -> verify page loads |
| 3.6 | Payment Management | Navigate to Payments -> verify page loads |
| 3.7 | Fraud Detection | Navigate to Fraud Detection -> verify table and filters |
| 3.8 | GDPR Compliance | Navigate to GDPR -> verify table and action buttons |
| 3.9 | Analytics | Navigate to Analytics -> verify KPIs and charts |
| 3.10 | System Health | Navigate to System Health -> verify service health cards |

### 4d: Broker Happy Path (API-only — via curl)

**Test credentials:** broker@test.com / password123

Follow `docs/happy-paths/HAPPY_PATHS.md` Section 4, steps 4.1 through 4.4. Use Bash curl commands:

```bash
# 4.1 Get token
TOKEN=$(curl -s -X POST "http://localhost:8180/realms/auction-platform/protocol/openid-connect/token" \
  -d "client_id=broker-app" \
  -d "grant_type=password" \
  -d "username=broker@test.com" \
  -d "password=password123" | jq -r '.access_token')

# 4.2 Dashboard
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/brokers/me/dashboard

# 4.3 Lead Management
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/brokers/leads

# 4.4 Lot Intake (if test data available)
# curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
#   http://localhost:8080/api/v1/brokers/lots/intake -d '{...}'
```

Verify HTTP status codes and response shapes match `docs/happy-paths/HAPPY_PATHS.md` expectations.

### 4e: Demo Flow — Competitive Bidding & Anti-Sniping (Steps 5-6)

When running the `demo` mode, Steps 5-6 of `HAPPY_PATH_DEMO.md` require special handling:

**Step 5 — Competitive Bidding & Anti-Sniping:**

This step combines browser UI testing with API calls. The flow is:

1. **Setup (5.0):** Use Bash curl to create a short-duration auction (~4 minutes). Store the `AUCTION_ID`, `BUYER1_TOKEN`, and `BUYER2_TOKEN` as shell variables for subsequent steps.

2. **Multi-buyer simulation:** Since Playwright MCP uses a single browser profile, simulate the second buyer (buyer2@test.com) via API calls using curl. Buyer 1 interacts via the browser UI, Buyer 2 bids via curl.

3. **Anti-sniping verification (5.3):** This requires waiting until the auction's countdown timer shows < 2:00 remaining. Use `browser_evaluate` to read the timer value, or calculate the wait time from the auction's end time. Then:
   - Place a bid via curl as Buyer 2
   - Check the API response for `"extensionApplied": true`
   - Refresh the browser page and verify the timer has been extended

4. **Auction close (5.5):** Wait for the auction's end time to pass (the `AuctionClosingScheduler` auto-closes every 1s). Poll the auction status via curl until it shows "CLOSED". Timeout after 5 minutes.

   ```bash
   # Poll for auction close (max 5 min)
   for i in $(seq 1 300); do
     STATUS=$(curl -sf -H "Authorization: Bearer $ADMIN_TOKEN" \
       "http://localhost:8080/api/v1/auctions/$AUCTION_ID" | jq -r '.data.status')
     if [ "$STATUS" = "CLOSED" ]; then break; fi
     sleep 1
   done
   ```

**Step 6 — Post-Auction Settlement & Payment:**

This step is primarily API-driven via Bash curl:

1. **Award (6.1):** `POST /api/v1/auctions/{id}/award` — if the endpoint doesn't exist, report as a bug with root cause "missing REST endpoint" and file `services/auction-engine/src/main/kotlin/.../AuctionResource.kt`.

2. **Checkout (6.2):** `POST /api/v1/payments/checkout` with the winning buyer's token.

3. **Pay (6.3):** `POST /api/v1/payments/checkout/{id}/pay` — submit payment.

4. **Admin payments page (6.4):** Switch to admin-dashboard browser and navigate to Payments page — verify via Playwright MCP.

5. **Settle (6.5):** `PATCH /api/v1/payments/{id}/settle` via curl.

6. **Seller settlements page (6.6):** Switch to seller-portal browser and navigate to Settlements page — verify via Playwright MCP.

For API-only steps, record curl responses as evidence and verify HTTP status codes and response body shapes. A 404 or 500 on any of these endpoints is a bug.

**buyer2@test.com prerequisite:** If buyer2@test.com doesn't exist in Keycloak and the token request fails, mark Step 5 as PARTIAL with note "Second buyer user not configured in Keycloak". Fall back to single-buyer anti-sniping testing (buyer places bids via UI, check if timer extends when bid is within the last 2 minutes).

---

## Step 5: Compile Bug Report

For **each failure or partial pass**, create a bug entry with the following format (matching existing reports in `tests/`):

### Bug Entry Template

```markdown
### BUG-<ROLE><NUMBER>: <Short title>

- **Step:** <step reference, e.g., "2.3 Create a New Lot">
- **Severity:** <Critical | Major | Minor | Info>
- **Status:** OPEN
- **Description:** <What happened vs what was expected>
- **Root Cause:** <Analysis of why it failed — check source code if possible>
- **File:** <Affected source file(s) with path>
- **Console Error:** <Exact console error text, or "None">
- **Network:** <Failed request details: method, URL, status code, response body snippet>
```

**Severity definitions:**
- **Critical**: Blocks the happy path flow entirely (can't proceed to next step)
- **Major**: Incorrect behavior, data wrong, or significant functionality broken (flow continues)
- **Minor**: Cosmetic, UX, or non-blocking issue
- **Info**: Accessibility warnings, suggestions, or spec mismatches

---

## Step 6: Write the Report

Write the report to: `tests/<role>-happy-path-report-<YYYY-MM-DD>.md`

Use today's date. For `all` mode, write one report per role.

### Report Template

```markdown
# <Role> Happy Path — Test Report

**Date:** <YYYY-MM-DD>
**Tester:** Claude Code (automated via Playwright MCP)
**Application:** <App name> (<URL>)
**Environment:** <List running services and ports>

---

## Summary

| Severity | Count |
|----------|-------|
| Critical | <n> |
| Major | <n> |
| Minor | <n> |
| Info | <n> |
| **Total** | **<n>** |

**Overall Result:** <X of Y happy path steps pass. Brief summary.>

---

## Happy Path Step Results

| Step | Test | Result | Notes |
|------|------|--------|-------|
| <ref> | <name> | <PASS/FAIL/PARTIAL/SKIP> | <brief note> |
| ... | ... | ... | ... |

---

## Critical Bugs

<bug entries with severity=Critical>

## Major Bugs

<bug entries with severity=Major>

## Minor Bugs

<bug entries with severity=Minor>

## Info / Accessibility Issues

<bug entries with severity=Info>

---

## Network Request Failures Summary

| Endpoint | Status | Page(s) Affected |
|----------|--------|------------------|
| ... | ... | ... |

---

## Console Errors Summary

| Error | Page(s) | Count |
|-------|---------|-------|
| ... | ... | ... |

---

## Screenshots

All test screenshots saved to: `tests/test-screenshots/`

| File | Description |
|------|-------------|
| `<role>-<step>-<name>.png` | <description> |
| ... | ... |
```

---

## Step 7: Teardown

**CRITICAL: Close browser pages BEFORE killing processes.** This prevents stale browser windows from persisting after the subagent finishes.

### 7a: Close all Playwright MCP browser tabs
1. Call `mcp__playwright__browser_tabs` with `action: "list"` to get all open tabs
2. For each tab (except the last one), call `mcp__playwright__browser_tabs` with `action: "close"` and the tab `index`
3. For the last remaining tab, call `mcp__playwright__browser_navigate` with `url: "about:blank"`

This ensures the browser state is clean and no stale pages remain.

### 7b: Kill all services
Use the `/kill-all` skill to stop and remove all services, kill frontend dev servers, and close any browser. Do **not** pass the `volumes` argument — volumes should persist for future test runs.

---

## Important Notes

- **Do NOT fix bugs during the test run.** Only observe and report. Fixing is a separate task.
- **Take screenshots liberally.** At minimum: one per happy path step, plus one per bug found.
- **Check console and network after every navigation.** Many bugs only manifest as silent API failures.
- **If a step depends on a prior step that failed**, mark it as SKIP with the reason.
- **For the broker path**, save curl output as evidence instead of screenshots.
- **Cross-reference with known issues** in `docs/happy-paths/HAPPY_PATHS.md` "Known Issues" section — note if a bug is pre-existing vs new.
- **Keycloak login flow**: After navigating to the app, wait for Keycloak redirect. Take a `browser_snapshot`, find the username/password fields by `ref`, use `browser_fill_form` to enter credentials, then `browser_click` the Sign In button. Use `browser_wait_for` to confirm redirect back to the app.
- **SPC product context**: When creating lots during testing, use SPC-relevant products (containers, climate equipment, fencing, modular structures) rather than generic industrial equipment. Search queries should use terms like "kontejner", "container", "grelec" (heater), "ograja" (fence).
