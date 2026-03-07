# Happy Path Testing — SPC Pitch Demo Flow

End-to-end test script that mirrors the live demo presentation flow for SPC (Storitveno Prodajni Center).
Run this before every rehearsal and on demo day to catch regressions.

**Prerequisite:** Run `bash scripts/seed-demo-data.sh` and `bash scripts/check-health.sh` before starting.

---

## Test Environment

| Component | URL |
|-----------|-----|
| Buyer Web (Nuxt SSR) | http://localhost:3000 |
| Seller Portal (Vite SPA) | http://localhost:5174 |
| Admin Dashboard (Vite SPA) | http://localhost:5175 |
| Gateway API | http://localhost:8080 |
| Keycloak | http://localhost:8180 |

### Test Credentials

| Role | Email | Password | Portal |
|------|-------|----------|--------|
| Buyer 1 | buyer@test.com | password123 | buyer-web |
| Buyer 2 | buyer2@test.com | password123 | buyer-web (second browser/tab) |
| Seller | seller@test.com | password123 | seller-portal |
| Admin | admin@test.com | password123 | admin-dashboard |

### Browser Tab Setup

Pre-login all portals in separate browser tabs before testing.

| Tab | URL | Logged in as |
|-----|-----|-------------|
| 1 | http://localhost:3000 | buyer@test.com (Buyer 1) |
| 2 | http://localhost:5174 | seller@test.com |
| 3 | http://localhost:5175 | admin@test.com |

> **Multi-buyer setup:** For Step 5 (competitive bidding), open a second buyer-web instance in a separate browser profile or incognito window, logged in as buyer2@test.com. Alternatively, use API calls (`curl`) with buyer2's token to simulate the second bidder.

---

## Step 1: Buyer Homepage & i18n (Tab 1 — buyer-web)

### 1.1 Homepage Layout

1. Navigate to http://localhost:3000
2. **Verify:** Hero section loads with heading and search bar
3. **Verify:** Navigation bar with Login/Register or user menu (if pre-logged-in)
4. **Verify:** Country flag icons visible — SPC target markets (SI, HR, AT, DE, IT or similar)
5. Scroll down the page
6. **Verify:** Category grid section with category cards matching SPC product lines (Containers, Climate Control, Construction Equipment, Modular Structures, or similar)
7. **Verify:** Featured auctions section with lot cards from SPC seed data
8. **Verify:** Each lot card shows: title (SPC product name), image placeholder, starting bid in EUR, location (Ljubljana SI or similar)

### 1.2 Language Switch

1. Find the language selector in the header
2. Click to switch language to German (DE)
3. **Verify:** Page content updates to German — headings, navigation labels, button text
4. Switch language to Croatian (HR) if available, otherwise try another available language
5. **Verify:** Page content updates to the selected language
6. Switch back to the default language (Slovenian or English)
7. **Verify:** Page returns to default language

### 1.3 CO2 Counter (if present on homepage)

1. Look for a CO2 savings counter or sustainability badge on the homepage
2. **Verify:** Displays a CO2 value (may be zero if no completed auctions yet) — page must not error

---

## Step 2: Seller Creates Lot (Tab 2 — seller-portal)

### 2.1 Seller Dashboard

1. Switch to Tab 2 (seller-portal, http://localhost:5174)
2. **Verify:** Dashboard loads without errors (no 500)
3. **Verify:** KPI cards visible: Active Lots, Total Revenue, or similar metrics
4. **Verify:** Seeded SPC lots appear in the lot list or recent activity (containers, climate equipment)

### 2.2 Create a New Lot

1. Click "Create Lot" or "New Lot" button
2. **Verify:** Lot creation form loads with all fields
3. Fill in the form with an SPC-relevant product:
   - **Title:** "Pisarniski kontejner SPC K6 — Office Container 6m"
   - **Brand:** "spc" (or select from dropdown)
   - **Category:** Select from dropdown (e.g., "Office Containers" or similar container category)
   - **Description:** "Insulated office container 6m with PVC windows, LED lighting, electrical system. Stackable up to 3 units."
   - **Starting Bid:** 4500
   - **Reserve Price:** 5200
   - **City:** "Ljubljana"
   - **Country:** "SI"
4. Click "Create Lot" / Submit
5. **Verify:** Success notification shown
6. **Verify:** Redirected to lot list or lot detail page
7. **Verify:** New lot appears with status "DRAFT"

### 2.3 Submit Lot for Review

1. Click on the newly created lot to open its detail page
2. **Verify:** Lot detail page loads with all entered information
3. **Verify:** Category displays as a human-readable name (NOT a UUID)
4. **Verify:** Location shows "Ljubljana, SI" (not raw ISO code or blank)
5. Click "Submit for Review"
6. **Verify:** Confirmation dialog appears
7. Confirm submission
8. **Verify:** Status changes to "PENDING_REVIEW" or "Pending Review"

---

## Step 3: Admin Approves Lot & Creates Auction (Tab 3 — admin-dashboard)

### 3.1 Lot Approval Queue

1. Switch to Tab 3 (admin-dashboard, http://localhost:5175)
2. Navigate to "Lot Approval" in the sidebar
3. **Verify:** Page loads without errors
4. **Verify:** Pending lots visible — at least 2 from SPC seed data + the one just created
5. **Verify:** Each pending lot shows: title (SPC product name), seller info, starting bid

### 3.2 Approve a Lot

1. Click "Approve" on one of the pending lots
2. **Verify:** Success notification shown
3. **Verify:** Lot removed from pending queue or status updates to "APPROVED"

### 3.3 Create Auction

1. Navigate to "Auctions" in the sidebar
2. Click "Create Auction"
3. **Verify:** Form loads with lot selector dropdown
4. Select the just-approved lot from the dropdown
5. Set start time to now or a time in the recent past
6. Set end time to 1 hour or more in the future
7. Click "Create Auction"
8. **Verify:** Success — auction appears in the auctions list
9. **Verify:** Auction shows status "ACTIVE" or "OPEN"

---

## Step 4: Buyer Searches & Places Bid (Tab 1 — buyer-web)

### 4.1 Search for Equipment

1. Switch to Tab 1 (buyer-web, http://localhost:3000)
2. Click the search bar
3. Type "kontejner" or "container"
4. **Verify:** Search results page loads with matching lots
5. **Verify:** At least one result from SPC seed data (e.g., "Pisarniski kontejner 6m" office container)
6. **Verify:** Each result card shows: title, starting bid, location (Ljubljana, SI), status

### 4.2 View Lot Detail

1. Click on an SPC container lot card
2. **Verify:** Lot detail page loads with:
   - Title: SPC product name (e.g., "Pisarniski kontejner 6m")
   - Image gallery area (placeholder if no images)
   - Specifications table (dimensions, insulation, features)
   - Location with country flag (Ljubljana, SI)
   - CO2 badge (if available — container reuse avoids ~8-12 tonnes CO2)
3. **Verify:** Bid panel visible on the right side with:
   - Current bid amount (from seed data bids)
   - Countdown timer
   - Bid input field
   - "Place Bid" button
   - Minimum bid increment displayed

### 4.3 Place a Bid

1. In the bid panel, enter a bid amount higher than current bid + minimum increment
2. Click "Place Bid"
3. **Verify:** Bid confirmation shown (success message or toast)
4. **Verify:** Current bid updates to the new amount
5. **Verify:** Bid history section updates with the new bid entry

### 4.4 Watchlist

1. On the lot detail page, click the heart icon
2. **Verify:** Heart icon turns red (filled, `text-red-500`)
3. Navigate to user menu -> "Watchlist" (or /my/watchlist)
4. **Verify:** Watchlist page loads
5. **Verify:** Watched lots appear with red heart icons
6. **Verify:** Each watchlist item shows: title, image, starting bid

---

## Step 5: Competitive Bidding & Anti-Sniping (Multi-buyer)

> This step demonstrates the core competitive bidding experience with anti-sniping timer extension. It requires a short-duration auction and two competing buyers.

### 5.0 Setup: Create Short Auction via API

Create a short-duration auction (ending in ~4 minutes) for the anti-sniping demo. Use API calls from the admin token:

```bash
# Get admin token
ADMIN_TOKEN=$(curl -sf -X POST "http://localhost:8180/realms/auction-platform/protocol/openid-connect/token" \
  -d "client_id=admin-dashboard" -d "grant_type=password" \
  -d "username=admin@test.com" -d "password=password123" | jq -r '.access_token')

# Get buyer1 token
BUYER1_TOKEN=$(curl -sf -X POST "http://localhost:8180/realms/auction-platform/protocol/openid-connect/token" \
  -d "client_id=buyer-web" -d "grant_type=password" \
  -d "username=buyer@test.com" -d "password=password123" | jq -r '.access_token')

# Get buyer2 token
BUYER2_TOKEN=$(curl -sf -X POST "http://localhost:8180/realms/auction-platform/protocol/openid-connect/token" \
  -d "client_id=buyer-web" -d "grant_type=password" \
  -d "username=buyer2@test.com" -d "password=password123" | jq -r '.access_token')

# Ensure buyer2 profile exists
curl -sf -H "Authorization: Bearer $BUYER2_TOKEN" http://localhost:8080/api/v1/users/me > /dev/null

# Pick an approved lot that has no auction yet (e.g., lot 5 "Dieselski grelec" from seed data)
# Or use any approved lot ID. Find one:
LOT_ID=$(curl -sf -H "Authorization: Bearer $ADMIN_TOKEN" \
  "http://localhost:8080/api/v1/lots?status=APPROVED&pageSize=10" | \
  jq -r '.data.items[0].id // .items[0].id // empty')

# Create auction ending in ~4 minutes
NOW_EPOCH=$(date +%s)
START_TIME=$(date -u -d "@$((NOW_EPOCH - 60))" +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || date -u -r "$((NOW_EPOCH - 60))" +"%Y-%m-%dT%H:%M:%SZ")
END_TIME=$(date -u -d "@$((NOW_EPOCH + 240))" +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || date -u -r "$((NOW_EPOCH + 240))" +"%Y-%m-%dT%H:%M:%SZ")

AUCTION_RESPONSE=$(curl -sf -X POST "http://localhost:8080/api/v1/auctions" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"lotId\": \"$LOT_ID\",
    \"brand\": \"spc\",
    \"sellerId\": \"00000000-0000-0000-0000-000000000002\",
    \"startTime\": \"$START_TIME\",
    \"endTime\": \"$END_TIME\",
    \"startingBid\": 650
  }")
AUCTION_ID=$(echo "$AUCTION_RESPONSE" | jq -r '.data.auctionId // .auctionId // empty')
echo "Short auction created: $AUCTION_ID (ends in ~4 min)"
```

**Verify:** Auction created with status ACTIVE and endTime approximately 4 minutes from now.

### 5.1 Buyer 1 Opens the Auction (Tab 1 — buyer-web)

1. In Tab 1 (buyer-web, buyer@test.com), navigate to the lot detail page for the short auction
2. **Verify:** Countdown timer is visible and counting down (shows ~3-4 minutes remaining)
3. **Verify:** Timer shows minutes:seconds format
4. **Verify:** Bid input field and "Place Bid" button are visible
5. **Verify:** Starting bid amount is displayed (EUR 650)

### 5.2 Opening Bids — Buyer 1 and Buyer 2 Trade Bids

Buyer 1 places the first bid via the UI:

1. Enter EUR 660 in the bid input field (starting bid 650 + minimum increment 10)
2. Click "Place Bid"
3. **Verify:** Success confirmation, current bid updates to EUR 660

Buyer 2 counter-bids via API (or second browser):

```bash
# Buyer 2 bids EUR 670
curl -sf -X POST "http://localhost:8080/api/v1/auctions/$AUCTION_ID/bids" \
  -H "Authorization: Bearer $BUYER2_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount": 670}'
```

4. **Verify (Tab 1):** After refreshing the page, current bid shows EUR 670 and bid count increased
5. **Verify:** Buyer 1 is no longer the high bidder

Buyer 1 counter-bids via UI:

6. Enter EUR 680, click "Place Bid"
7. **Verify:** Success, current bid EUR 680

Continue trading bids until the auction is within the last 2 minutes (the anti-sniping window).

### 5.3 Anti-Sniping: Bid in Final 2 Minutes

> **CRITICAL:** Wait until the countdown timer shows less than 2:00 remaining before placing this bid. The anti-sniping window is the final 2 minutes before the auction's scheduled end time.

1. When timer shows < 2:00 remaining, Buyer 2 places a bid via API:

```bash
# Buyer 2 bids within anti-sniping window
SNIPE_AMOUNT=710  # or whatever the next valid increment is
curl -sf -X POST "http://localhost:8080/api/v1/auctions/$AUCTION_ID/bids" \
  -H "Authorization: Bearer $BUYER2_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"amount\": $SNIPE_AMOUNT}"
```

2. **Verify:** The API response includes `"extensionApplied": true`
3. **Verify (Tab 1):** After refreshing, the countdown timer has been extended — it now shows more time than before (timer added ~2 minutes from the bid timestamp)
4. **Verify:** Auction status may show "CLOSING" (indicates anti-sniping is active)

Buyer 1 counter-bids within the new extended window:

5. Enter the next valid bid amount, click "Place Bid"
6. **Verify:** Timer extends again if still within 2 minutes of end time
7. **Verify:** The `extensionApplied` field in bid confirmation indicates whether extension occurred

### 5.4 Proxy Bidding (Optional)

If time allows, demonstrate proxy (auto) bidding:

1. Buyer 1 sets a proxy bid via UI (if toggle/input exists) or via API:

```bash
# Buyer 1 sets auto-bid with max EUR 800
curl -sf -X POST "http://localhost:8080/api/v1/auctions/$AUCTION_ID/auto-bids" \
  -H "Authorization: Bearer $BUYER1_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"maxAmount": 800}'
```

2. Buyer 2 bids EUR 730 (or next increment):

```bash
curl -sf -X POST "http://localhost:8080/api/v1/auctions/$AUCTION_ID/bids" \
  -H "Authorization: Bearer $BUYER2_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount": 730}'
```

3. **Verify:** Proxy bid auto-counters — the auction's current high bid should be EUR 740 (Buyer 1's proxy bid at minimum counter)
4. **Verify:** Bid history shows a proxy bid entry

### 5.5 Auction Closes Automatically

1. Wait for the countdown timer to reach 0:00 (or wait for the end time to pass)
2. The `AuctionClosingScheduler` runs every 1 second and will auto-close the auction
3. **Verify (API):** Check auction status:

```bash
curl -sf -H "Authorization: Bearer $ADMIN_TOKEN" \
  "http://localhost:8080/api/v1/auctions/$AUCTION_ID" | jq '.data.status'
# Expected: "CLOSED"
```

4. **Verify:** The auction detail shows a winner (the last high bidder) and final bid amount
5. **Verify (Tab 1):** Refreshing the lot detail page shows auction ended / bidding closed

---

## Step 6: Post-Auction — Award, Payment & Settlement

> This step tests the post-auction flow: lot award, buyer payment, and seller settlement. These operations are primarily API-driven since the frontend pages may not cover the full flow.

### 6.1 Award the Lot to the Winner

After the auction is CLOSED, award the lot via API:

```bash
# Award the lot (transitions CLOSED -> AWARDED, publishes LotAwardedEvent)
curl -sf -X POST "http://localhost:8080/api/v1/auctions/$AUCTION_ID/award" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json"
```

**Verify:** Response includes `winnerId` and `hammerPrice`.
**Verify:** Auction status is now "AWARDED":

```bash
curl -sf -H "Authorization: Bearer $ADMIN_TOKEN" \
  "http://localhost:8080/api/v1/auctions/$AUCTION_ID" | jq '.data.status'
# Expected: "AWARDED"
```

### 6.2 Buyer Initiates Checkout

The winning buyer initiates checkout for the won lot:

```bash
# Determine the winner token (whoever was the high bidder)
WINNER_TOKEN=$BUYER1_TOKEN  # or $BUYER2_TOKEN depending on who won

# Initiate checkout
CHECKOUT_RESPONSE=$(curl -sf -X POST "http://localhost:8080/api/v1/payments/checkout" \
  -H "Authorization: Bearer $WINNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"lotIds\": [\"$LOT_ID\"],
    \"currency\": \"EUR\",
    \"buyerCountry\": \"SI\",
    \"buyerType\": \"BUSINESS\",
    \"buyerVatId\": \"SI12345678\"
  }")
echo "$CHECKOUT_RESPONSE" | jq .
```

**Verify:** Response includes:
- `checkoutId` — unique checkout session ID
- `payments` array with at least 1 entry containing `paymentId`, `hammerPrice`, `buyerPremium`, `vatAmount`, `vatRate`, `totalAmount`
- `totalAmount` — the total due including VAT and buyer premium
- `currency` — "EUR"

```bash
PAYMENT_ID=$(echo "$CHECKOUT_RESPONSE" | jq -r '.payments[0].paymentId')
echo "Payment ID: $PAYMENT_ID"
```

### 6.3 Buyer Submits Payment

The buyer submits payment via a payment method:

```bash
curl -sf -X POST "http://localhost:8080/api/v1/payments/checkout/$PAYMENT_ID/pay" \
  -H "Authorization: Bearer $WINNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"paymentMethod": "BANK_TRANSFER"}'
```

**Verify:** Response shows payment status updated (e.g., `"status": "COMPLETED"` or `"PROCESSING"`).

### 6.4 Admin Views Payments (Tab 3 — admin-dashboard)

1. Switch to Tab 3 (admin-dashboard)
2. Navigate to "Payments" in the sidebar
3. **Verify:** Payment management page loads
4. **Verify:** Payment summary cards visible (Total Pending, Total Paid, Total Overdue)
5. **Verify:** Payment list table shows the new payment entry

### 6.5 Admin Settles Payment

Settle the completed payment to trigger seller payout:

```bash
# Admin settles the payment
curl -sf -X PATCH "http://localhost:8080/api/v1/payments/$PAYMENT_ID/settle" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json"
```

**Verify:** Response includes:
- `settlementId` — unique settlement ID
- `sellerId` — the seller's UUID
- `netAmount` — hammer price minus 10% platform commission
- `commission` — 10% of hammer price
- `status` — "PAID" (simulated immediate settlement in dev)
- `bankReference` — SEPA reference string

### 6.6 Seller Views Settlement (Tab 2 — seller-portal)

1. Switch to Tab 2 (seller-portal)
2. Navigate to "Settlements" in the sidebar
3. **Verify:** Settlements page loads without errors
4. **Verify:** Settlement entry visible showing:
   - Net amount (hammer price minus commission)
   - Commission amount and rate (10%)
   - Status: PAID
   - Bank reference

---

## Step 7: Compliance Dashboard (Tab 3 — admin-dashboard)

### 7.1 GDPR Requests

1. Switch to Tab 3 (admin-dashboard)
2. Navigate to "GDPR" or "Data Requests" in sidebar
3. **Verify:** Page loads without errors
4. **Verify:** Table structure visible with columns: User, Type (export/erasure), Status, Date
5. **Verify:** Action buttons visible: Approve, Reject

### 7.2 Fraud Detection

1. Navigate to "Fraud Detection" in sidebar
2. **Verify:** Page loads without errors
3. **Verify:** Alerts table with columns: Type, Severity, Status, Description
4. **Verify:** Filter controls for severity, status, type
5. **Verify:** Action buttons: Investigate, Resolve, Dismiss

### 7.3 System Health

1. Navigate to "System Health" in sidebar
2. **Verify:** Page loads without errors
3. **Verify:** Service health cards visible for microservices
4. **Verify:** Status indicators (green/red/yellow) for each service

---

## Step 8: CO2 Impact Report (Tab 2 — seller-portal)

### 8.1 CO2 Report Page

1. Switch to Tab 2 (seller-portal)
2. Navigate to "CO2 Report" in sidebar
3. **Verify:** Page loads without errors (no 500)
4. **Verify:** Report header visible with title "CO2 Emissions Report" or similar
5. **Verify:** Summary cards visible:
   - Total CO2 Saved (kg)
   - Equivalent Trees Planted
   - Total Lots Contributed
   - Average CO2 per Lot
6. **Verify:** Export buttons visible (PDF, CSV)

---

## Cross-Step Verifications

These verify platform-wide behavior across the demo flow.

### Authentication Persistence

1. After completing all steps, verify all 3 tabs are still logged in
2. **Verify:** No unexpected logouts or token expiration errors during the flow
3. **Verify:** Switching between tabs does not trigger re-authentication

### Data Consistency

1. Lot created in Step 2 should be visible in:
   - Seller portal lot list (Tab 2) — status PENDING_REVIEW after submit
   - Admin pending queue (Tab 3) — if not yet approved
   - Search results (Tab 1) — after approval and ES indexing (may take ~30s)
2. Bid placed in Step 4 should update:
   - Current bid on lot detail (Tab 1)
   - Bid history on lot detail (Tab 1)
3. Payment and settlement from Step 6 should be visible in:
   - Admin payments page (Tab 3)
   - Seller settlements page (Tab 2)

### Error-Free Navigation

1. **Verify:** No JavaScript console errors on any of the 3 frontends
2. **Verify:** No 500 errors in any navigation flow
3. **Verify:** No blank pages or loading spinners that never resolve

---

## Known Limitations (Don't Demo These)

| Feature | Status | Workaround |
|---------|--------|------------|
| Broker portal | No frontend | Mention backend API works, don't navigate |
| WebSocket real-time bids | Works but fragile | Refresh page to see updated bids |
| Email notifications | Backend works, MailHog captures | Mention but don't show |
| Image uploads | MinIO configured | Use SPC product photos as placeholders |
| Adyen payments | Integration ready | Mention PSD2 readiness, don't process |
| User listing (admin) | No GET /users endpoint | Skip user management page |
| Award REST endpoint | May not exist yet | Use API call or add POST /auctions/{id}/award |
| Second buyer user | Needs Keycloak setup | Create buyer2@test.com or use API simulation |

---

## Seed Data Reference

The SPC seed script (`scripts/seed-demo-data.sh`) creates:

### Test Users (Keycloak)

| User | Email | Role | UUID |
|------|-------|------|------|
| Buyer 1 | buyer@test.com | buyer_active | 00000000-0000-0000-0000-000000000001 |
| Buyer 2 | buyer2@test.com | buyer_active | 00000000-0000-0000-0000-000000000005 |
| Seller | seller@test.com | seller_verified | 00000000-0000-0000-0000-000000000002 |
| Admin | admin@test.com | admin_ops | 00000000-0000-0000-0000-000000000004 |

> **Note:** buyer2@test.com must be created in Keycloak before running the competitive bidding demo. Add it to the realm import or create manually: Keycloak Admin > auction-platform realm > Users > Add user with buyer_active role.

### Categories (SPC product lines)
- Office & Residential Containers (Pisarniski in bivalni kontejnerji)
- Shipping Containers (Ladijski kontejnerji)
- Sanitary Containers (Sanitarni kontejnerji)
- Modular Structures (Modularne strukture)
- Climate Control (Klimatska oprema)
- Construction Equipment & Machinery (Gradbena oprema in stroji)
- Metal Fencing & Accessories (Kovinske ograje in dodatki)

### Lots

| Lot | Category | Location | Starting Bid | Auction |
|-----|----------|----------|-------------|---------|
| Pisarniski kontejner 6m | Office Containers | Ljubljana, SI | EUR 4,500 | Active, 24h, has bids |
| Ladijski zabojnik 20ft HC | Shipping Containers | Ljubljana, SI | EUR 2,800 | Active, 48h |
| Sanitarni kontejner z tusem | Sanitary Containers | Ljubljana, SI | EUR 7,200 | Active, ending soon (~3 min) |
| Master Climate DH 92 razvlazilec | Climate Control | Ljubljana, SI | EUR 850 | Active, 24h, has bids |
| Dieselski grelec 30kW | Climate Control | Ljubljana, SI | EUR 650 | No auction |
| Kovinska ograja ECONOMICO 30m | Construction Equipment | Ljubljana, SI | EUR 1,200 | No auction |
| Modularna pisarna 2x 20ft | Modular Structures | Ljubljana, SI | EUR 12,500 | No auction |
| Caterpillar mini bager 1.5t (2019) | Construction Machinery | Zagreb, HR | EUR 18,000 | No auction |
| Klimatska naprava 5kW | Climate Control | Ljubljana, SI | EUR 1,100 | Pending approval |
| Kontejnerska hisa 40ft | Modular Structures | Ljubljana, SI | EUR 22,000 | Pending approval |

### Pre-Placed Bids (from seed)

| Auction | Bids |
|---------|------|
| Pisarniski kontejner 6m | EUR 4,600 -> EUR 4,700 -> EUR 4,900 |
| Sanitarni kontejner z tusem | EUR 7,300 -> EUR 7,500 |
| Master Climate DH 92 | EUR 900 -> EUR 950 |

### Watchlist (buyer@test.com)

- Pisarniski kontejner 6m
- Sanitarni kontejner z tusem
- Modularna pisarna 2x 20ft

### Anti-Sniping Configuration

| Parameter | Value |
|-----------|-------|
| Anti-sniping window | Last 2 minutes before end time |
| Extension duration | 2 minutes from bid timestamp |
| Max extensions | 100 per auction |
| Auto-close scheduler | Runs every 1 second |

### Bid Increment Tiers

| Price Range (EUR) | Minimum Increment (EUR) |
|-------------------|-------------------------|
| 0 – 99 | 1 |
| 100 – 499 | 5 |
| 500 – 999 | 10 |
| 1,000 – 4,999 | 25 |
| 5,000 – 9,999 | 50 |
| 10,000 – 49,999 | 100 |
| 50,000 – 99,999 | 250 |
| 100,000+ | 500 |

### Payment & Settlement Flow

| Step | Endpoint | Role | Description |
|------|----------|------|-------------|
| Award | `POST /api/v1/auctions/{id}/award` | admin | Awards lot to winner after CLOSED |
| Checkout | `POST /api/v1/payments/checkout` | buyer | Creates payment with VAT calculation |
| Pay | `POST /api/v1/payments/checkout/{id}/pay` | buyer | Submits payment method |
| Settle | `PATCH /api/v1/payments/{id}/settle` | admin | Triggers seller payout (10% commission) |
| Invoices | `GET /api/v1/payments/invoices` | any | Lists generated invoices |
| Settlements | `GET /api/v1/payments/settlements` | seller | Lists seller payouts |
