# Happy Path Testing - EU B2B Online Auction Platform

Verified happy path flows for every platform role, tested end-to-end via browser.

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

| Role | Email | Password |
|------|-------|----------|
| Buyer | buyer@test.com | password123 |
| Seller | seller@test.com | password123 |
| Broker | broker@test.com | password123 |
| Admin | admin@test.com | password123 |
| Super Admin | superadmin@test.com | password123 |

---

## 1. Buyer Happy Path

**Application:** Buyer Web (http://localhost:3000)

### 1.1 Landing Page (Unauthenticated)

1. Navigate to http://localhost:3000
2. **Verify:** Hero section with "Find Industrial Equipment at Auction" heading
3. **Verify:** Search bar with category dropdown and search button
4. **Verify:** "Featured Lots" section with lot cards
5. **Verify:** Navigation bar with "Login" and "Register" buttons
6. **Verify:** Footer with platform info

### 1.2 Authentication

1. Click "Login" in the navbar
2. Redirected to Keycloak login page
3. Enter buyer@test.com / password123
4. Click "Sign In"
5. **Verify:** Redirected back to buyer-web
6. **Verify:** Navbar shows user avatar/menu instead of Login button
7. **Verify:** User dropdown has "My Bids", "Watchlist", "Profile", "Logout" options

### 1.3 Browse & Search Lots

1. Use the search bar on the homepage to search for equipment (e.g., "Caterpillar")
2. **Verify:** Search results page loads with matching lots
3. **Verify:** Each lot card shows: title, image placeholder, starting bid, location, status badge
4. **Verify:** Filter sidebar available (category, price range, location)

### 1.4 View Lot Detail

1. Click on a lot card from search results or featured lots
2. **Verify:** Lot detail page loads with:
   - Title and description
   - Image gallery (or placeholder)
   - Starting bid / current bid
   - Location (city, country)
   - Specifications table
   - Seller information
3. **Verify:** Bid panel visible on the right side with bid input and "Place Bid" button

### 1.5 Place a Bid

1. On a lot detail page for an ACTIVE lot (with an auction assigned), enter a bid amount in the bid panel
2. Click "Place Bid"
3. **Verify:** Bid confirmation shown
4. **Prerequisite:** An auction must have been created for the lot via Admin Dashboard (step 3.3b)

### 1.6 Navigation Pages

1. **My Bids:** Click user menu > "My Bids" — shows list of placed bids (empty for new user)
2. **Watchlist:** Click user menu > "Watchlist" — shows watched lots (empty for new user)
3. **Profile:** Click user menu > "Profile" — shows user profile form

### 1.7 Logout

1. Click user avatar in navbar
2. Click "Logout"
3. **Verify:** Redirected to homepage, Login button reappears

---

## 2. Seller Happy Path

**Application:** Seller Portal (http://localhost:5174)

### 2.1 Authentication

1. Navigate to http://localhost:5174
2. Automatically redirected to Keycloak login
3. Enter seller@test.com / password123
4. Click "Sign In"
5. **Verify:** Redirected to seller portal dashboard

### 2.2 Dashboard

1. **Verify:** Dashboard page shows:
   - KPI cards: Active Lots, Total Revenue, Sell-Through Rate, Avg. Hammer Price (showing zeros for new seller)
   - Recent Activity section
   - Quick Actions: "Create New Lot", "View Settlements"
   - Status Overview with lot status breakdown

### 2.3 Create a New Lot

1. Click "Create New Lot" from dashboard or navigate to My Lots > "Create Lot"
2. **Verify:** Lot creation form loads with fields:
   - Title (required)
   - Brand (required) — e.g., "Caterpillar"
   - Description (textarea)
   - Category dropdown (fetched from catalog-service)
   - Starting Bid (EUR)
   - Reserve Price (EUR)
   - Location: City + Country (ISO code dropdown, e.g., NL, DE, FR)
3. Fill in all required fields:
   - Title: "Caterpillar D6 Bulldozer 2019"
   - Brand: "Caterpillar"
   - Category: Select from dropdown (e.g., "Construction Machinery")
   - Starting Bid: 45000
   - Reserve Price: 55000
   - City: "Rotterdam"
   - Country: "NL"
4. Upload multiple images (at least 2–3):
   - Click the upload area or drag-and-drop image files (JPEG, PNG, or WebP, max 10MB each)
   - **Verify:** Each image shows upload progress bar, then completes with a thumbnail preview
   - **Verify:** First image gets "Primary" badge automatically
   - **Verify:** Image count updates (e.g., "3/10")
   - Optionally: hover over images to reorder or change primary image
5. Click "Create Lot"
6. **Verify:** Success notification shown
7. **Verify:** Redirected to lot detail page
8. **Verify:** All uploaded images visible in the image gallery on the lot detail page
9. **Verify:** New lot appears in My Lots list with status "DRAFT"

### 2.4 Submit Lot for Review

1. Click on the created lot in My Lots list
2. **Verify:** Lot detail page shows all entered information
3. Click "Submit for Review" button
4. **Verify:** Status changes to "PENDING_REVIEW"
5. **Verify:** Lot now visible to admin in approval queue

### 2.5 My Lots Page

1. Navigate to "My Lots" in sidebar
2. **Verify:** Paginated list of seller's lots
3. **Verify:** Each lot shows: title, status badge, starting bid, creation date
4. **Verify:** Pagination works (0-based backend, 1-based frontend)
5. Click on a lot to view detail

### 2.6 Edit a Lot

1. From lot detail page, click "Edit"
2. **Verify:** Edit form pre-populated with current lot data
3. Modify a field (e.g., change starting bid)
4. Click "Save Changes"
5. **Verify:** Success notification, changes reflected on detail page

### 2.7 Settlements Page

1. Navigate to "Settlements" in sidebar
2. **Verify:** Page loads without errors
3. **Verify:** Shows "No settlements found" empty state (no sold lots yet)
4. **Verify:** Settlement totals section visible (all zeros)

### 2.8 Analytics Page

1. Navigate to "Analytics" in sidebar
2. **Verify:** Page loads without errors
3. **Verify:** KPI cards showing zeros (Total Revenue, Lots Sold, Avg. Price, Sell-Through)
4. **Verify:** Chart sections present: Revenue Trend, Lot Performance, Category Breakdown

### 2.9 CO2 Report Page

1. Navigate to "CO2 Report" in sidebar
2. **Verify:** Page loads without errors
3. **Verify:** Header with "CO2 Emissions Report" title
4. **Verify:** Export buttons visible (PDF, CSV)

### 2.10 Profile Page

1. Navigate to "Profile" in sidebar
2. **Verify:** Company Details form with fields: Company Name, VAT Number, Address, etc.
3. **Verify:** Tabs for "Bank Settings" and "Notifications"
4. **Verify:** Form is editable and submittable

### 2.11 Logout

1. Click logout in sidebar/header
2. **Verify:** Redirected to Keycloak, session ended

---

## 3. Admin Happy Path

**Application:** Admin Dashboard (http://localhost:5175)

### 3.1 Authentication

1. Navigate to http://localhost:5175
2. Redirected to Keycloak login
3. Enter admin@test.com / password123
4. Click "Sign In"
5. **Verify:** Redirected to admin dashboard

### 3.2 Dashboard Overview

1. **Verify:** Admin dashboard home page loads with navigation sidebar
2. **Verify:** Sidebar links: Dashboard, Auctions, Lot Approval, Users, Payments, Fraud Detection, GDPR, Analytics, System Health

### 3.3 Lot Approval (Key Admin Flow)

1. Navigate to "Lot Approval" in sidebar
2. **Verify:** Page shows list of lots with status "PENDING_REVIEW"
3. **Verify:** Each lot shows: title, seller name, starting bid, submission date
4. Click "Approve" on a pending lot (e.g., "Caterpillar D6 Bulldozer 2019")
5. **Verify:** Success notification shown
6. **Verify:** Lot removed from pending queue
7. **Verify:** Lot status changes to "APPROVED" in catalog-service

### 3.3b Create Auction for Approved Lot

1. Navigate to "Auctions" in sidebar
2. Click "Create Auction" button
3. **Verify:** Lot selector dropdown shows approved lots
4. Select an approved lot from the dropdown
5. **Verify:** Lot details displayed (title, brand, starting bid, location)
6. Set start time to now (or near-future)
7. Set end time to 1 hour from now
8. Click "Create Auction"
9. **Verify:** Success — redirected to auctions list
10. **Verify:** Via API: `GET /api/v1/lots/{id}` shows `status: ACTIVE` and `auctionId` set

### 3.4 Auction Management

1. Navigate to "Auctions" in sidebar
2. **Verify:** Page loads with list of auctions (including the one just created)
3. **Verify:** Table columns: Title/ID, Status, Start Date, End Date, Lot Count, Total Bids
4. **Verify:** Filter controls for status, brand, date range
5. **Verify:** "Create Auction" button available
6. **Verify:** Auction data correctly normalized (auctionId → id, endTime → endDate)

### 3.5 User Management

1. Navigate to "Users" in sidebar
2. **Verify:** Page loads without errors
3. **Verify:** Shows empty user list (user-service has no list endpoint; individual user lookup works)
4. **Note:** Full user listing requires a backend `GET /users` endpoint to be implemented

### 3.6 Payment Management

1. Navigate to "Payments" in sidebar
2. **Verify:** Page loads without errors
3. **Verify:** Shows payment records table (empty if no transactions)
4. **Verify:** Filter controls for status, date range

### 3.7 Fraud Detection

1. Navigate to "Fraud Detection" in sidebar
2. **Verify:** Page loads without errors
3. **Verify:** Fraud alerts table with columns: Type, Severity, Status, Description
4. **Verify:** Filter controls for severity, status, type
5. **Verify:** Action buttons: Investigate, Resolve, Dismiss

### 3.8 GDPR Compliance

1. Navigate to "GDPR" in sidebar
2. **Verify:** Page loads without errors
3. **Verify:** GDPR requests table with columns: User, Type (export/erasure), Status, Requested At
4. **Verify:** Filter controls for type and status
5. **Verify:** Action buttons: Approve, Reject (with reason)

### 3.9 Analytics

1. Navigate to "Analytics" in sidebar
2. **Verify:** Page loads without errors
3. **Verify:** Platform overview KPIs: Total Revenue, Auctions, Lots Sold, Users, Bids
4. **Verify:** Charts: Monthly Revenue, Registration Trends, Category Popularity, Daily Bid Volume

### 3.10 System Health

1. Navigate to "System Health" in sidebar
2. **Verify:** Page loads without errors
3. **Verify:** Service health cards showing status for each microservice
4. **Verify:** NATS messaging status
5. **Verify:** Database pool metrics
6. **Verify:** System metrics (CPU, memory, disk)

---

## 4. Broker Happy Path

**Application:** No frontend exists (backend API only)

**Note:** The Keycloak client `broker-app` (port 3003) is configured, but no broker frontend application has been built. The backend APIs are fully functional and were verified via direct API calls.

### 4.1 Authentication

```bash
# Get broker access token
TOKEN=$(curl -s -X POST "http://localhost:8180/realms/auction-platform/protocol/openid-connect/token" \
  -d "client_id=broker-app" \
  -d "grant_type=password" \
  -d "username=broker@test.com" \
  -d "password=password123" | jq -r '.access_token')
```

### 4.2 Broker Dashboard

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/brokers/me/dashboard
```

**Verify:** Returns broker dashboard data:
- `activeLots`: number of active lots
- `totalLeads`: number of managed leads
- `conversionRate`: lead-to-lot conversion percentage
- `pendingActions`: items requiring attention

### 4.3 Lead Management

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/brokers/leads
```

**Verify:** Returns paginated list of leads with:
- Lead ID, status, contact information
- Associated lot references
- Commission terms

### 4.4 Lot Intake (on behalf of seller)

```bash
curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  http://localhost:8080/api/v1/brokers/lots/intake \
  -d '{"title":"...", "sellerId":"...", ...}'
```

**Verify:** Creates a lot on behalf of a seller through the broker's intake flow.

---

## Cross-Role Flow: Lot Lifecycle

This is the end-to-end lifecycle tested across roles:

```
Seller creates lot (DRAFT)
    ↓
Seller submits for review (PENDING_REVIEW)
    ↓
Admin approves lot (APPROVED)
    ↓
Admin creates auction + assigns lot (ACTIVE) — step 3.3b
    ↓
Buyer places bids — step 1.5
    ↓
Auction closes → highest bidder wins (SOLD)
    ↓
Payment processed → Seller receives settlement
```

**Tested and verified steps:** Seller create → Seller submit → Admin approve → Admin create auction → Buyer places bid

---

## Known Issues

| Issue | Impact | Status |
|-------|--------|--------|
| Casbin filter disabled | Authorization policies use incompatible patterns | Open |
| User-service has no list endpoint | Admin can't list all users | Open |
| Broker frontend missing | Broker must use API directly | Open |
| Seller-service NATS sync broken | Seller dashboard shows no lot data from catalog | Workaround applied |

---

## Bugs Fixed During Testing

27 bugs were found and fixed across 3 testing sessions. See `bugs-fixed.md` for the complete list including root causes and file-level fixes. Key categories:

- **Auth/OIDC (Bugs 1-5):** Role mismatches, issuer URLs, claim paths, UUID formats
- **Gateway (Bugs 6, 16):** Missing proxy implementation, missing route patterns
- **Backend (Bugs 7-9, 11-14):** Elasticsearch headers, missing DB tables, transaction conflicts, NATS config
- **Frontend DTOs (Bugs 15, 17-20, 22, 27):** Field name mismatches, pagination offset, nested vs flat objects
- **Error Handling (Bugs 21, 23, 25-26):** Missing graceful fallbacks for 404/500 responses
- **Auth Context (Bug 24):** Vue inject() called outside setup context
