# Live Demo Script — Tradex Pitch Presentation

> **Total demo time: ~20 minutes**
> Keep this open on a second screen or print as a cheat sheet.

---

## Pre-Demo Setup (30 min before)

```bash
# 1. Infrastructure
docker compose -f docker/compose/docker-compose-infrastructure.yaml --env-file docker/compose/.env up -d
# Wait ~60s for services to initialize

# 2. Backend (run each in a separate terminal or use tmux)
for svc in gateway-service auction-engine catalog-service user-service search-service \
           seller-service compliance-service analytics-service co2-service \
           payment-service notification-service media-service broker-service; do
    ./gradlew :services:$svc:quarkusDev &
done

# 3. Frontends
cd frontend/buyer-web && npm run dev &
cd frontend/seller-portal && npm run dev &
cd frontend/admin-dashboard && npm run dev &

# 4. Seed + verify
bash scripts/seed-demo-data.sh
bash scripts/check-health.sh
```

**Pre-login all 3 portals** in separate browser tabs before the demo starts.

---

## Browser Tab Setup

| Tab | URL | Logged in as |
|-----|-----|-------------|
| 1 | http://localhost:3000 | buyer@test.com |
| 2 | http://localhost:5174 | seller@test.com |
| 3 | http://localhost:5175 | admin@test.com |

---

## Step 1: Buyer Homepage (2 min)

**Tab 1 — buyer-web**

- [ ] Show homepage: hero section, search bar
- [ ] Point out country flags (NL, DE, BE, FR, PL, IT, RO) — "7 EU languages from day one"
- [ ] Scroll to category grid — "15 main categories, 20+ subcategories covering all industrial equipment"
- [ ] Scroll to featured auctions — show the seeded auction cards
- [ ] Point out CO2 counter if visible — "Every sale tracks CO2 avoided vs. manufacturing new"
- [ ] **Language switch**: Click language selector → switch to German → "Alles auf Deutsch" → switch back

**SAY:** "This is what a buyer sees. Clean, professional, multilingual. 7 EU languages from day one — not Google Translate, but proper localized content."

---

## Step 2: Seller Creates Lot (3 min)

**Tab 2 — seller-portal**

- [ ] Show seller dashboard — status cards, lot overview
- [ ] Click "Create Lot" / "New Lot"
- [ ] Fill in:
  - **Title:** "ABB IRB 6700 Industrial Robot"
  - **Category:** Select from dropdown (e.g., Metalworking > Welding Equipment)
  - **Starting Bid:** EUR 25,000
  - **Location:** Eindhoven, NL
  - **Description:** Brief description of the robot
- [ ] Click "Submit for Review"
- [ ] Show status change to "Pending Review"

**SAY:** "Every lot goes through a compliance review before going live. This is not just quality control — it's a regulatory requirement under the EU Digital Services Act. The seller submits, and an administrator reviews it."

---

## Step 3: Admin Approves & Creates Auction (3 min)

**Tab 3 — admin-dashboard**

- [ ] Navigate to "Lot Approval" or "Pending Lots"
- [ ] Show the pending lots (there should be 2 from seed + the one just created)
- [ ] Click on a lot → Review details → Click "Approve"
- [ ] Navigate to "Auctions" → "Create Auction"
- [ ] Select the approved lot, set start/end times → Create
- [ ] Show the auction is now live

**SAY:** "The admin sees everything — pending approvals, active auctions, compliance alerts. One click to approve, one click to create the auction. The lot is now live and open for bidding."

---

## Step 4: Buyer Places Bid (4 min)

**Tab 1 — buyer-web**

- [ ] Click search bar → type "Caterpillar" or "excavator"
- [ ] Show search results with autocomplete
- [ ] Click on the Caterpillar excavator lot
- [ ] Point out:
  - Lot specifications (manufacturer, year, hours)
  - Location with country flag
  - CO2 badge (CO2 savings)
  - Bid panel on the right with live timer
  - Current bid and minimum increment
- [ ] Place a bid (e.g., EUR 21,000)
- [ ] Show bid confirmation and updated bid history

**SAY:** "Notice the bid increment — EUR 1,000 at this price level. The system enforces tiered increments based on the current bid. Below EUR 100 it's EUR 1, between 100 and 500 it's EUR 5, and so on up to EUR 100 increments for items above EUR 10,000. This is exactly how physical auction houses work."

---

## Step 5: Anti-Sniping & Proxy Bidding (3 min)

**Stay on Tab 1 — explain conversationally**

- [ ] Point at the countdown timer on the bid panel

**SAY — Anti-Sniping:**
"Here's something every auction house cares about: anti-sniping protection. On most online platforms, a buyer can wait until the last second and place a bid — the seller gets a lower price, other buyers get frustrated. On Tradex, if someone bids in the last 2 minutes, the auction extends by 2 minutes. This keeps happening until no one bids for a full 2 minutes. The result? Sellers get 15-20% higher final prices. Every major auction house we've spoken to has asked for this feature."

**SAY — Proxy Bidding:**
"We also have proxy bidding — a buyer sets their maximum price, and the system bids on their behalf using the minimum increment, just like a human agent at a physical auction. This drives prices up naturally while respecting the buyer's limit. It's a feature that exists in the physical world but most online platforms don't offer."

---

## Step 6: Compliance Dashboard (3 min)

**Tab 3 — admin-dashboard**

- [ ] Navigate to "GDPR Requests" or "Data Requests"
  - Show the export/erasure workflow — "A user clicks 'Export My Data' and the system compiles everything within 30 days, as required by GDPR Article 17"
- [ ] Navigate to "Fraud Detection" or "Compliance Alerts"
  - Show shill bidding detection, bid manipulation alerts
  - "The system automatically flags suspicious patterns — same IP bidding against themselves, bid manipulation, unusual velocity"
- [ ] Navigate to "System Health" or "Services"
  - Show 13 services with status indicators

**SAY:** "This is what sets Tradex apart from every competitor. GDPR data export and erasure workflows, automated fraud detection for shill bidding and bid manipulation under the Digital Services Act, anti-money laundering screening. All built in. A competitor would need 12-18 months just to build this compliance layer."

---

## Step 7: CO2 Impact (2 min)

**Tab 2 — seller-portal**

- [ ] Navigate to "CO2 Report" or "Sustainability"
- [ ] Show summary cards:
  - Total CO2 saved (kg)
  - Equivalent trees planted
  - Average CO2 per lot
- [ ] Point out export buttons (PDF/CSV)

**SAY:** "Every sale on the platform automatically calculates the CO2 avoided by reusing equipment instead of manufacturing new. This is real ESG data that auction houses can include in their sustainability reports. It's a selling point for the auction house and it's a selling point for the buyer's procurement department. No other auction platform offers this."

---

## After Demo — Transition Back to Slides

**SAY:** "So that's the platform in action. A complete auction lifecycle — from listing to compliance review to bidding to settlement — all running live. Let me now talk about why this is hard for anyone else to replicate."

*[Switch to Slide 6 — Compliance Matrix]*

---

## Emergency Fallbacks

### If a service is down:
- Skip that section, say "Let me show you this part from our test recording"
- Switch to backup video or screenshots

### If login fails:
- Tokens should be cached in the browser from pre-login
- If Keycloak is completely down, use screenshots

### If search returns empty:
- Browse by category instead of searching
- "Let me navigate directly to one of our listed lots"

### If bid placement fails:
- Show the existing bid history from the seeded data
- Explain the feature verbally while pointing at the UI
