# Happy Path Testing — Pitch Demo Flow

End-to-end test script that mirrors the live demo presentation flow.
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
| Buyer | buyer@test.com | password123 | buyer-web |
| Seller | seller@test.com | password123 | seller-portal |
| Admin | admin@test.com | password123 | admin-dashboard |

### Browser Tab Setup

Pre-login all three portals in separate browser tabs before testing.

| Tab | URL | Logged in as |
|-----|-----|-------------|
| 1 | http://localhost:3000 | buyer@test.com |
| 2 | http://localhost:5174 | seller@test.com |
| 3 | http://localhost:5175 | admin@test.com |

---

## Step 1: Buyer Homepage & i18n (Tab 1 — buyer-web)

### 1.1 Homepage Layout

1. Navigate to http://localhost:3000
2. **Verify:** Hero section loads with heading and search bar
3. **Verify:** Navigation bar with Login/Register or user menu (if pre-logged-in)
4. **Verify:** Country flag icons visible (NL, DE, BE, FR, PL, IT, RO)
5. Scroll down the page
6. **Verify:** Category grid section with category cards (Construction Machinery, Agriculture, Transport, etc.)
7. **Verify:** Featured auctions section with lot cards from seed data
8. **Verify:** Each lot card shows: title, image placeholder, starting bid, location

### 1.2 Language Switch

1. Find the language selector in the header/footer
2. Click to switch language to German (DE)
3. **Verify:** Page content updates to German — headings, navigation labels, button text
4. Switch language to Dutch (NL)
5. **Verify:** Page content updates to Dutch
6. Switch back to English (EN)
7. **Verify:** Page returns to English

### 1.3 CO2 Counter (if present on homepage)

1. Look for a CO2 savings counter or sustainability badge on the homepage
2. **Verify:** Displays a CO2 value (may be zero if no completed auctions yet) — page must not error

---

## Step 2: Seller Creates Lot (Tab 2 — seller-portal)

### 2.1 Seller Dashboard

1. Switch to Tab 2 (seller-portal, http://localhost:5174)
2. **Verify:** Dashboard loads without errors (no 500)
3. **Verify:** KPI cards visible: Active Lots, Total Revenue, or similar metrics
4. **Verify:** Seeded lots appear in the lot list or recent activity

### 2.2 Create a New Lot

1. Click "Create Lot" or "New Lot" button
2. **Verify:** Lot creation form loads with all fields
3. Fill in the form:
   - **Title:** "ABB IRB 6700 Industrial Robot"
   - **Brand:** "troostwijk" (or select from dropdown)
   - **Category:** Select from dropdown (e.g., "Welding Equipment" under Metalworking)
   - **Description:** "ABB IRB 6700-150 industrial robot arm with IRC5 controller."
   - **Starting Bid:** 25000
   - **Reserve Price:** 65000
   - **City:** "Eindhoven"
   - **Country:** "NL"
4. Click "Create Lot" / Submit
5. **Verify:** Success notification shown
6. **Verify:** Redirected to lot list or lot detail page
7. **Verify:** New lot appears with status "DRAFT"

### 2.3 Submit Lot for Review

1. Click on the newly created lot to open its detail page
2. **Verify:** Lot detail page loads with all entered information
3. **Verify:** Category displays as a human-readable name (NOT a UUID)
4. **Verify:** Location shows "Eindhoven, NL" (not raw ISO code or blank)
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
4. **Verify:** Pending lots visible — at least 2 from seed data + the one just created
5. **Verify:** Each pending lot shows: title, seller info, starting bid

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
3. Type "Caterpillar" or "excavator"
4. **Verify:** Search results page loads with matching lots
5. **Verify:** At least one result from seed data (Caterpillar 320F L Hydraulic Excavator)
6. **Verify:** Each result card shows: title, starting bid, location, status

### 4.2 View Lot Detail

1. Click on the Caterpillar excavator lot card
2. **Verify:** Lot detail page loads with:
   - Title: "Caterpillar 320F L Hydraulic Excavator"
   - Image gallery area (placeholder if no images)
   - Specifications table (Manufacturer, Model, Year, Operating Hours, etc.)
   - Location with country flag
   - CO2 badge (if available)
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
3. Navigate to user menu → "Watchlist" (or /my/watchlist)
4. **Verify:** Watchlist page loads
5. **Verify:** Watched lots appear with red heart icons
6. **Verify:** Each watchlist item shows: title, image, starting bid

---

## Step 5: Anti-Sniping & Proxy Bidding (Tab 1 — buyer-web, verbal)

> This step is primarily verbal during the presentation. For testing, verify the UI elements exist.

### 5.1 Auction Timer

1. On any active lot detail page, look at the bid panel
2. **Verify:** Countdown timer is visible and counting down
3. **Verify:** Timer shows hours:minutes:seconds or appropriate format

### 5.2 Bid Increment Tiers

1. On a lot detail page, check the minimum bid increment displayed
2. **Verify:** Increment matches the tiered system:
   - Current bid < EUR 100 → EUR 1 increment
   - EUR 100-500 → EUR 5
   - EUR 500-1,000 → EUR 10
   - EUR 1,000-5,000 → EUR 25
   - EUR 5,000-10,000 → EUR 50
   - EUR 10,000+ → EUR 100

---

## Step 6: Compliance Dashboard (Tab 3 — admin-dashboard)

### 6.1 GDPR Requests

1. Switch to Tab 3 (admin-dashboard)
2. Navigate to "GDPR" or "Data Requests" in sidebar
3. **Verify:** Page loads without errors
4. **Verify:** Table structure visible with columns: User, Type (export/erasure), Status, Date
5. **Verify:** Action buttons visible: Approve, Reject

### 6.2 Fraud Detection

1. Navigate to "Fraud Detection" in sidebar
2. **Verify:** Page loads without errors
3. **Verify:** Alerts table with columns: Type, Severity, Status, Description
4. **Verify:** Filter controls for severity, status, type
5. **Verify:** Action buttons: Investigate, Resolve, Dismiss

### 6.3 System Health

1. Navigate to "System Health" in sidebar
2. **Verify:** Page loads without errors
3. **Verify:** Service health cards visible for microservices
4. **Verify:** Status indicators (green/red/yellow) for each service

---

## Step 7: CO2 Impact Report (Tab 2 — seller-portal)

### 7.1 CO2 Report Page

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
| Image uploads | MinIO configured | Use placeholder images |
| Adyen payments | Integration ready | Mention PSD2 readiness, don't process |
| User listing (admin) | No GET /users endpoint | Skip user management page |

---

## Seed Data Reference

The seed script (`scripts/seed-demo-data.sh`) creates:

| Lot | Category | Location | Starting Bid | Auction |
|-----|----------|----------|-------------|---------|
| Caterpillar 320F L Hydraulic Excavator | Excavators | Rotterdam, NL | EUR 15,000 | Active, 24h, has bids |
| Siemens SINUMERIK 840D CNC Milling Center | CNC Machines | Stuttgart, DE | EUR 25,000 | Active, 48h |
| ABB IRB 6700 Industrial Robot | Welding Equipment | Eindhoven, NL | EUR 20,000 | Active, ending soon (~3 min) |
| Liebherr LTM 1100-5.2 Mobile Crane | Cranes | Antwerp, BE | EUR 50,000 | Active, 24h, has bids |
| Trumpf TruLaser 3030 Fiber Laser | CNC Machines | Milan, IT | EUR 30,000 | Active, 48h |
| Toyota 8FBE18T Electric Forklift | Forklifts | Warsaw, PL | EUR 3,000 | No auction |
| Volvo FH 500 Tractor Unit | Trucks | Gothenburg, SE | EUR 18,000 | No auction |
| John Deere 6250R Premium Tractor | Tractors | Rennes, FR | EUR 35,000 | No auction |
| Krones Modulfill PET Bottling Line | Food Processing | Munich, DE | EUR 40,000 | Pending approval |
| SMA Sunny Central 2500-EV Solar Inverter | Energy & Utilities | Valencia, ES | EUR 15,000 | Pending approval |

### Pre-Placed Bids (from seed)

| Auction | Bids |
|---------|------|
| Caterpillar Excavator | EUR 16,000 → EUR 18,000 → EUR 20,000 |
| ABB Robot | EUR 21,000 → EUR 23,000 |
| Liebherr Crane | EUR 55,000 → EUR 60,000 |

### Watchlist (buyer@test.com)

- Caterpillar 320F L Hydraulic Excavator
- ABB IRB 6700 Industrial Robot
- Trumpf TruLaser 3030 Fiber Laser
