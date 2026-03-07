# Pitch Presentation Plan: Tradex EU B2B Auction Platform

## Context

We have a pitch/presentation in 1 week for the Tradex EU B2B Online Auction Platform to a **potential buyer (acquirer)** who is **business-focused** (not deeply technical) and already recognizes the business potential of selling industrial goods via auctions. The platform consists of 13 Kotlin/Quarkus microservices + 3 Vue frontends, all functional. Recent testing shows 29/32 steps PASS with 27 bugs fixed across 3 sessions.

**Goal:** Prove the platform is (1) a working product, not a prototype, (2) handles EU regulatory complexity competitors can't easily replicate, (3) is a multi-brand platform ready to onboard auction houses, and (4) is worth acquiring.

**Audience profile:** Business decision-maker. Understands the industrial auction market. Cares about: Does it work? Can it scale? What's the competitive moat? How fast can we go to market? Does NOT want to see code — wants to see the product in action and understand the business advantages.

**Deliverables:**
1. Bug fixes + demo seed script + health check (code prep)
2. Markdown slide content (problem, solution, compliance, roadmap) for pasting into presentation tool
3. Live demo script optimized for business-focused audience

---

## 1. MVP Scope Definition

### Include (Demo-Ready)

| Feature | Service(s) | Why It Matters |
|---------|-----------|----------------|
| Full auction lifecycle (create lot -> approve -> auction -> bid -> win) | catalog, auction-engine, gateway | Core value proposition |
| Event-sourced bidding (anti-sniping, proxy bidding, tiered increments) | auction-engine | Technical crown jewel — drives higher final prices for sellers |
| Buyer web with search, lot detail, bid panel, auction timer | buyer-web + search-service | Primary user experience |
| Seller portal with lot creation, status tracking, CO2 report | seller-portal + seller-service | Seller value prop |
| Admin dashboard with lot approval, auction creation, compliance UIs | admin-dashboard | Operational story |
| Keycloak SSO with role-based access (buyer/seller/admin) | All services | Enterprise-grade auth |
| EU compliance (GDPR, AML, DSA, VAT) | compliance-service, payment-service | Regulatory differentiator — 12-18 month head start |
| Elasticsearch search with autocomplete & faceting | search-service | Discovery UX |
| Multi-brand/white-label (troostwijk, surplex) | All services (brand field) | Platform vs product story — "Shopify for auctions" |
| CO2 avoidance tracking | co2-service | ESG differentiator |
| i18n (7 EU languages) | buyer-web | Cross-border readiness |

### Defer (Mention but Don't Demo)

- Broker portal (no frontend exists, backend APIs work)
- Real Adyen payment processing (integration ready, not live)
- WebSocket real-time bid updates (works but fragile for live demo)
- Email notifications (MailHog captures, don't demo)
- Image uploads via MinIO (use placeholder images)
- CI/CD pipeline execution (mention it exists, don't run it)

---

## 2. Pre-Presentation Work (Prioritized)

### P0: Must Do (Days 1-3) — Demo fails without these

| # | Task | Effort | Details |
|---|------|--------|---------|
| 1 | **Fix seller-service /me/* 500 errors** | 1h | `SellerProfileRepository.kt` — missing `period` column in UPSERT_METRICS SQL. All seller dashboard endpoints broken without this. |
| 2 | **Fix category UUID display** | 30m | `frontend/seller-portal/src/views/LotDetailView.vue` — shows UUID instead of category name. Makes seller portal look unfinished. |
| 3 | **Fix watchlist heart icon color** | 30m | buyer-web watchlist component — should be red when favorited, currently gray. |
| 4 | **Create demo data seed script** | 4h | New: `scripts/seed-demo-data.sh` — populates via API calls: 8-10 realistic industrial lots (Caterpillar excavator EUR 45k, Siemens CNC EUR 12.5k, ABB robot EUR 28k, Liebherr crane EUR 180k, Trumpf laser EUR 95k...), 2 active auctions with bid history, 1 auction ending soon (anti-sniping demo), 2 pending lots for admin approval. Must be idempotent. |
| 5 | **Create pre-demo health check** | 1h | New: `scripts/check-health.sh` — validates all 13 services are up, Keycloak responds, seed data exists. |
| 6 | **Full end-to-end demo rehearsal** | 4h | Run through entire demo flow (ACT 2, steps 1-7), fix any blockers found. |

**P0 total: ~11 hours**

### P1: Should Do (Days 3-5) — Significantly improves impression

| # | Task | Effort | Details |
|---|------|--------|---------|
| 7 | **Record backup demo video** | 2h | Screen recording of full demo flow as fallback if live demo fails. |
| 8 | **Screenshot deck** | 1h | Every key screen captured for fallback slides. |
| 9 | **Grafana "Platform Overview" dashboard** | 2h | Active auctions, bids/min, service health, revenue chart — powerful scalability visual. |
| 10 | **Slide deck content** | 4h | Create `docs/presentation/slides.md` with all 10 slides + speaker notes. |
| 11 | **Verify CO2 numbers display** | 1h | Ensure CO2 counter (buyer homepage) and CO2 report (seller portal) show realistic seeded data. |
| 12 | **Test i18n switching** | 30m | EN -> DE -> NL during buyer-web for "EU-ready" moment. |

**P1 total: ~10.5 hours**

### P2: Nice to Have (Days 5-7) — If time permits

| # | Task | Effort | Details |
|---|------|--------|---------|
| 13 | Add stock photos for demo lots | 2h | Via media-service or hardcoded image URLs |
| 14 | Second buyer user for competitive bidding demo | 1h | Keycloak user + seed script bids |
| 15 | 1-page handout (architecture + key stats) | 2h | Leave-behind for audience |
| 16 | Full dress rehearsal with timing | 2h | Practice the 35-45 min presentation end-to-end |

**P2 total: ~7 hours**

---

## 3. Presentation Flow (35-45 minutes)

### ACT 1: The Problem (5 min)

**Slide 1 — Market:**
> "EUR 150 billion in used industrial equipment changes hands annually in Europe. 75% of B2B industrial auctions still rely on fragmented, single-country platforms."

**Slide 2 — Pain Points:**
- Sellers can't reach buyers across borders efficiently
- 27 different VAT rules, GDPR, AML, PSD2, DSA requirements
- No single platform handles all EU compliance in one solution
- Zero sustainability tracking (CO2 avoidance from reuse vs. new manufacturing)
- Legacy auction platforms can't scale

**Slide 3 — Opportunity:**
> "Tradex is the first EU-native B2B auction platform built from day one for cross-border compliance and multi-brand white-labeling."

---

### ACT 2: Live Demo (20 min) — The Heart of the Pitch

**Step 1: Buyer Homepage (2 min)**
- Open `localhost:3000` — show hero section, search bar, country flags (NL/DE/BE/FR/PL/IT/RO)
- Scroll down: category grid (Construction, Electronics, Metal Processing...), featured auctions carousel, CO2 counter
- Switch language to German, then back to English
- Say: "7 European languages supported from day one"

**Step 2: Seller Creates Lot (3 min)**
- Open seller portal (tab 2) — already logged in as seller@test.com
- Click "Create Lot"
- Fill in: "ABB IRB 6700 Industrial Robot", Eindhoven NL, EUR 25,000, reserve EUR 30,000
- Click "Create" -> success notification
- Submit for review -> status changes to "PENDING_REVIEW"
- Say: "Every lot goes through a compliance review before going live. This is a regulatory requirement in the EU."

**Step 3: Admin Approves & Creates Auction (3 min)**
- Open admin dashboard (tab 3) — logged in as admin@test.com
- Navigate to "Lot Approval" -> show the pending lot with seller info
- Click "Approve"
- Navigate to "Auctions" -> "Create Auction"
- Select the approved lot, set start time (now), end time (10 min from now)
- Click "Create Auction"
- Say: "The auction is now live across the platform. Let's go bid."

**Step 4: Buyer Places Bid (4 min)**
- Switch back to buyer-web tab
- Login as buyer@test.com
- Search for "ABB Robot" using the search bar
- Open lot detail page
- Point out: specifications, location, CO2 badge ("8,500 kg CO2 avoided"), bid panel with auction timer
- Place bid: EUR 26,000
- Say: "Bid confirmed. Notice the minimum increment is EUR 100 — we use tiered increments based on price range, just like major auction houses."

**Step 5: Anti-Sniping & Proxy Bidding (3 min)**
- Say: "Two features that drive higher final prices for sellers..."
- **Anti-sniping:** "If someone bids in the last 2 minutes, the auction automatically extends by 2 minutes. This prevents last-second sniping that costs sellers money. Every major auction house wants this feature."
- **Proxy bidding:** "Buyers can set their maximum price — the system bids on their behalf using the minimum increment, just like a human agent at a physical auction. This increases engagement and drives higher final prices."
- Keep this conversational — NO code. Use the UI to illustrate.

**Step 6: Compliance Dashboard (3 min)**
- Switch to admin dashboard
- Navigate to "GDPR Requests" -> show the request management table (export/erasure requests)
- Navigate to "Fraud Detection" -> show alert types (shill bidding, bid manipulation, account takeover)
- Navigate to "System Health" -> show all 13 services with status indicators
- Say: "GDPR data erasure, fraud detection, anti-money laundering — all built in, not bolted on afterwards."

**Step 7: CO2 Impact (2 min)**
- Switch to seller portal
- Navigate to "CO2 Report"
- Show: total CO2 avoided, equivalent trees planted, per-lot breakdown, monthly trend
- Point out PDF/CSV export buttons
- Say: "Every item sold on the platform avoids CO2 compared to manufacturing new. Sellers get ESG reports. Buyers get sustainability credits. This is increasingly required for corporate procurement."

---

### ACT 3: Why This Is Hard to Replicate (5 min)

**Slide — "Built for Europe, Not Bolted On":**
- Simple visual: map of EU with 27 country flags
- "Each country has different VAT rates, compliance rules, and languages"
- "We handle all 27 from day one — GDPR, AML, PSD2, DSA, VAT"
- "This is a 12-18 month head start over anyone starting from scratch"

**Slide — EU Compliance Matrix:**

| Regulation | What It Means | Status |
|-----------|---------------|--------|
| GDPR | Users can export/delete their data on demand | Built-in |
| PSD2 SCA | Strong authentication for payments (EU law) | Adyen-ready |
| DSA | Fraud detection & content moderation required | Built-in |
| AML | Anti-money laundering screening for high-value lots | Built-in |
| VAT | 27 countries, reverse charge, OSS, margin scheme | Automated |

**Slide — Platform, Not Product:**
- "Any auction house can run their brand on Tradex"
- Multi-brand concept: Troostwijk, Surplex, Industrial Auctions — each gets their own branded experience, shared infrastructure
- "This is how Shopify works for e-commerce. We're building that for industrial auctions."
- Mention: shared technology investment, lower cost per auction house, network effects as more sellers/buyers join

---

### ACT 4: Scale & Operations (3 min)

**Slide — "Ready for Production":**
- 13 independent services — each can scale separately based on demand
- Auto-scaling: during peak auction closing times, bidding capacity increases automatically
- Every bid is permanently recorded — perfect for regulatory audits and dispute resolution
- Real-time monitoring dashboard (show Grafana screenshot or live)
- All infrastructure runs on industry-standard cloud services (Kubernetes)
- Focus on BUSINESS meaning, not technical jargon

---

### ACT 5: Roadmap & Ask (5 min)

**Slide — Roadmap:**
- **Q2 2026:** Broker portal UI, live payment processing (Adyen), image upload polish
- **Q3 2026:** Mobile-responsive buyer experience, real-time bid updates, email notification templates
- **Q4 2026:** Bidder behavior analytics, price prediction, full white-label customization, first partner pilots (Troostwijk, Surplex)

**Slide — Closing:**
- Tailor to acquisition conversation:
  - What the platform is worth (technology + compliance + head start)
  - Time to market advantage (vs building from scratch)
  - Revenue model (transaction fees, SaaS licensing to auction houses, premium features)
  - Integration path for the buyer's existing business

---

### Three Narrative Themes (repeat throughout)

1. **"Built for Europe, not bolted on"** — 27-country VAT, GDPR, AML from day one. This is a 12-18 month head start over any competitor.
2. **"Every bid is permanently recorded"** — perfect for regulatory audits, dispute resolution. Immutable event trail that no competitor can retrofit.
3. **"Platform, not product"** — multi-brand white-label. Any auction house can onboard with their own branding. Think "Shopify for industrial auctions."

---

## 4. Demo Infrastructure Setup

### Pre-Demo Checklist (30 min before)

```bash
# 1. Start infrastructure
docker compose -f docker/compose/docker-compose-infrastructure.yaml --env-file docker/compose/.env up -d
# Wait 60s for PostgreSQL, Keycloak, NATS, Redis, Elasticsearch

# 2. Start backend services (13 terminals or background)
./gradlew :services:gateway-service:quarkusDev &
./gradlew :services:auction-engine:quarkusDev &
./gradlew :services:catalog-service:quarkusDev &
./gradlew :services:user-service:quarkusDev &
./gradlew :services:search-service:quarkusDev &
./gradlew :services:seller-service:quarkusDev &
./gradlew :services:compliance-service:quarkusDev &
./gradlew :services:analytics-service:quarkusDev &
./gradlew :services:co2-service:quarkusDev &
./gradlew :services:payment-service:quarkusDev &
./gradlew :services:notification-service:quarkusDev &
./gradlew :services:media-service:quarkusDev &
./gradlew :services:broker-service:quarkusDev &

# 3. Start frontends
cd frontend/buyer-web && npm run dev &       # port 3000
cd frontend/seller-portal && npm run dev &   # port 5174
cd frontend/admin-dashboard && npm run dev & # port 5175

# 4. Seed demo data
bash scripts/seed-demo-data.sh

# 5. Health check
bash scripts/check-health.sh

# 6. Pre-login all 3 portals in separate browser tabs
```

### Minimum Services for Demo
- **Must run:** gateway, auction-engine, catalog, user, search (core buyer/bid flows)
- **Should run:** seller-service, co2-service, analytics-service, compliance-service (seller portal + admin flows)
- **Optional:** payment, notification, media, broker (mention but don't need running)

---

## 5. Risk Mitigation

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Service fails to start | Medium | Start 30 min early. Pre-test every flow. Keep service logs visible in terminal. |
| Keycloak login fails | Low | Pre-login all 3 portals before demo. Have tokens cached in browser. |
| No seed data (first run) | High | Idempotent seed script. Verify with curl that lots exist before demo starts. |
| Bid placement fails | Low | Pre-test the exact bid flow. Have a backup auction already with existing bids. |
| Search returns empty results | Medium | ES indexing is async. Seed data 5+ min before demo. Fallback: browse by category. |
| Seller-service 500 errors | High if unfixed | **P0 bug fix #1.** This is the absolute top priority. |
| Laptop/network issues | Low | Everything runs locally. Zero external dependencies needed. |

### Backup Plans (in order of preference)
1. **Pre-recorded video** — 5-min screencast of full demo flow, recorded the evening before
2. **Screenshot slides** — every key screen embedded in slide deck as fallback
3. **Narrated walkthrough** — use screenshots + talk through the flow conversationally ("here the seller would create a lot, here the admin approves it..."). Business audiences accept this gracefully.
4. **Partial demo** — frontends render with hardcoded/seed data even if some backend services are down

---

## 6. Critical Files

### Bug Fixes (P0)
- `services/seller-service/src/main/kotlin/eu/auctionplatform/seller/infrastructure/persistence/repository/SellerProfileRepository.kt` — UPSERT_METRICS missing period column
- `frontend/seller-portal/src/views/LotDetailView.vue` — category UUID vs name display
- `frontend/buyer-web/` — watchlist heart icon color (identify exact component)

### New Files to Create
- `scripts/seed-demo-data.sh` — demo data population script (lots, auctions, bids)
- `scripts/check-health.sh` — pre-demo service health validator
- `docs/presentation/slides.md` — slide content with speaker notes

---

## 7. Slide Content Deliverable

Create `docs/presentation/slides.md` — markdown content for each slide, ready to paste into Google Slides / PowerPoint / Keynote:

1. **Title slide** — Tradex: The EU B2B Auction Platform
2. **Market slide** — EUR 150B market, fragmentation problem, opportunity
3. **Pain points slide** — cross-border complexity, compliance burden, no sustainability tracking
4. **Solution overview** — what Tradex does (1 sentence per capability)
5. **Demo transition** — "Let me show you how it works" (cue for live demo)
6. **Compliance matrix** — simple table of EU regulations handled
7. **Platform vs Product** — multi-brand white-label concept (Shopify analogy)
8. **Scale readiness** — key operational stats (13 services, auto-scaling, audit trail)
9. **Roadmap** — Q2/Q3/Q4 milestones with clear deliverables
10. **Closing / Ask** — tailored to acquisition conversation

Each slide includes: headline, 3-5 bullet points, and speaker notes with talking points.

---

## 8. Verification

After all P0 tasks are complete:
1. Run `scripts/check-health.sh` — all 13 services show green
2. Run `scripts/seed-demo-data.sh` — lots, auctions, bids populated
3. Walk through full demo script (ACT 2, steps 1-7) end-to-end in browser
4. Verify: seller dashboard loads without 500, categories show names not UUIDs, watchlist hearts are red
5. Time the full presentation — target 35-45 minutes
6. Record backup video of one successful demo run
