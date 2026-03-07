# Pitch Presentation Plan: SPC Aukcije — B2B Auction Platform for Containers & Industrial Equipment

## Context

We have a pitch/presentation for the SPC-branded EU B2B Online Auction Platform to **Matej Zupan**, director and 95.83% owner of **SPC, storitveno podjetje, d.o.o.** (Ljubljana, Slovenia). SPC is a growing container and industrial equipment company (EUR 4.19M revenue, 9 employees, A+ credit rating, 900+ customers across Central Europe & Balkans). The platform consists of 13 Kotlin/Quarkus microservices + 3 Vue frontends, all functional. Recent testing shows 29/32 steps PASS with 27 bugs fixed across 3 sessions.

**Goal:** Prove the platform is (1) a working product customized for SPC's business, (2) handles EU regulatory complexity competitors can't easily replicate, (3) turns SPC from an equipment dealer into a marketplace operator, and (4) is ready to deploy for SPC's market within 6 weeks.

**Audience profile:** Matej Zupan is a **hands-on business owner** (not a VC or corporate executive). He thinks in terms of: "Can I make money with this?", "Does it work?", "Can my team of 9 run this?", "What about my actual market (SI, HR, AT, DE)?", "How much will it cost?". Does NOT want to see code — wants to see the product in action with SPC's products.

**Deliverables:**
1. Bug fixes + SPC-tailored demo seed script + health check (code prep)
2. Markdown slide content (SPC growth story, opportunity, compliance, roadmap) for pasting into presentation tool
3. Live demo script using SPC's actual products, categories, and markets

---

## 1. MVP Scope Definition

### Include (Demo-Ready)

| Feature | Service(s) | SPC Business Relevance |
|---------|-----------|------------------------|
| Full auction lifecycle (create lot -> approve -> auction -> bid -> win) | catalog, auction-engine, gateway | Core value — SPC lists containers, buyers bid competitively |
| Event-sourced bidding (anti-sniping, proxy bidding, tiered increments) | auction-engine | Drives 15-30% higher prices vs. SPC's current fixed-price shop |
| Buyer web with search, lot detail, bid panel, auction timer | buyer-web + search-service | A buyer in Zagreb finds SPC's containers in Ljubljana |
| Seller portal with lot creation, status tracking, CO2 report | seller-portal + seller-service | SPC's team creates listings in 2 minutes |
| Admin dashboard with lot approval, auction creation, compliance UIs | admin-dashboard | Matej oversees everything from one screen |
| Keycloak SSO with role-based access (buyer/seller/admin) | All services | Enterprise-grade auth for B2B customers |
| EU compliance (GDPR, AML, DSA, VAT) | compliance-service, payment-service | Cross-border sales to HR, AT, DE, IT handled automatically |
| Elasticsearch search with autocomplete & faceting | search-service | Buyers search "kontejner" or "container" and find SPC's lots |
| CO2 avoidance tracking | co2-service | Every container resold avoids 8-12 tonnes CO2 — ESG reporting |
| i18n (7 languages: sl, hr, de, en, it, sr, hu) | buyer-web | SPC's actual markets: SI, HR, AT, DE, IT, BA, RS, HU |

### Defer (Mention but Don't Demo)

- Broker portal (no frontend exists, backend APIs work)
- Real Adyen payment processing (integration ready, not live)
- WebSocket real-time bid updates (works but fragile for live demo)
- Email notifications (MailHog captures, don't demo)
- Image uploads via MinIO (use SPC product photos as placeholders)
- CI/CD pipeline execution (mention it exists, don't run it)

---

## 2. Pre-Presentation Work (Prioritized)

### P0: Must Do (Days 1-3) — Demo fails without these

| # | Task | Effort | Details |
|---|------|--------|---------|
| 1 | **Fix seller-service /me/* 500 errors** | 1h | `SellerProfileRepository.kt` — missing `period` column in UPSERT_METRICS SQL. All seller dashboard endpoints broken without this. |
| 2 | **Fix category UUID display** | 30m | `frontend/seller-portal/src/views/LotDetailView.vue` — shows UUID instead of category name. Makes seller portal look unfinished. |
| 3 | **Fix watchlist heart icon color** | 30m | buyer-web watchlist component — should be red when favorited, currently gray. |
| 4 | **Create SPC demo data seed script** | 4h | New: `scripts/seed-demo-data-spc.sh` — populates via API calls: 8-10 SPC products (office container EUR 4,500, shipping container EUR 2,800, sanitary container EUR 7,200, dehumidifier EUR 850, fencing EUR 1,200, modular office EUR 12,500, mini excavator EUR 18,000...), 2 active auctions with bid history, 1 auction ending soon (anti-sniping demo), 2 pending lots for admin approval. Must be idempotent. |
| 5 | **Create pre-demo health check** | 1h | New: `scripts/check-health.sh` — validates all 13 services are up, Keycloak responds, seed data exists. |
| 6 | **Full end-to-end demo rehearsal with SPC data** | 4h | Run through entire demo flow (ACT 2, steps 1-7) using SPC products, fix any blockers found. |

**P0 total: ~11 hours**

### P1: Should Do (Days 3-5) — Significantly improves impression

| # | Task | Effort | Details |
|---|------|--------|---------|
| 7 | **Record backup demo video with SPC data** | 2h | Screen recording of full demo flow with SPC products as fallback if live demo fails. |
| 8 | **Screenshot deck** | 1h | Every key screen captured with SPC products for fallback slides. |
| 9 | **Grafana "Platform Overview" dashboard** | 2h | Active auctions, bids/min, service health, revenue chart — powerful scalability visual. |
| 10 | **Slide deck content (SPC-customized)** | 4h | SPC-specific slides: growth story, market opportunity, competitor gap. See ACT 1-5 below. |
| 11 | **Verify CO2 numbers for container reuse** | 1h | Ensure CO2 counter shows realistic data — 8-12 tonnes per container resold. |
| 12 | **Test i18n switching (sl/hr/de/en)** | 30m | Slovenian -> Croatian -> German -> English during buyer-web for "EU-ready" moment. |

**P1 total: ~10.5 hours**

### P2: Nice to Have (Days 5-7) — If time permits

| # | Task | Effort | Details |
|---|------|--------|---------|
| 13 | **Swap buyer-web color scheme to SPC #004d71** | 2h | CSS variable override in `preset.ts` and `tailwind.preset.ts` — see `docs/SPC_REBRANDING_PLAN.md` Phase 1 |
| 14 | **Add SPC logo to buyer-web navbar** | 1h | Replace bolt icon with SPC logo in `Navbar.vue` and `Footer.vue` |
| 15 | **Add SPC product photos to demo lots** | 2h | Via media-service or hardcoded image URLs from `docs/presentation/assets/` |
| 16 | **Add Slovenian (sl) and Croatian (hr) translations** | 2h | Minimum viable i18n for SPC's primary markets |
| 17 | **Full dress rehearsal with timing** | 2h | Practice the 35-45 min presentation end-to-end with SPC framing |

**P2 total: ~9 hours**

---

## 3. Presentation Flow (35-45 minutes)

### ACT 1: The Opportunity for SPC (5 min)

**Slide 1 — SPC's Growth Story:**
- SPC grew from EUR 3.28M (2022) to EUR 4.19M (2024) — 28% growth in 2 years
- 900+ customers across Central Europe & Balkans
- A+ credit rating, own production facilities in Ljubljana
- Vision: expand to full European market with technology
- *"What if your next growth leap isn't more trucks and warehouses — it's a platform?"*

**Slide 2 — The Used Equipment Problem:**
- SPC already sells used containers and equipment (fixed price on shop.spc.si or quote-based)
- Fixed prices leave money on the table — competitive bidding drives 15-30% higher prices
- Cross-border sales are manual (VAT calculations, compliance, language barriers)
- No visibility into demand from AT, DE, IT, HR markets

**Slide 3 — The Auction Platform Opportunity:**
- EUR 150B in used industrial equipment traded annually in Europe
- Online auction market growing at 28.9% CAGR (2025-2029)
- Competitors (Troostwijk, Euro Auctions, IronPlanet) are large but NOT focused on containers & modular structures
- *"SPC can own the container & construction equipment auction niche for Central Europe & Balkans"*
- Show competitor logos: Troostwijk, Euro Auctions, IronPlanet, Ritchie Bros — then show the gap: none are Slovenian, none focus on containers, none serve the Balkans well

---

### ACT 2: Live Demo (20 min) — "Let Me Show You Your Platform"

> **Critical framing:** Throughout the demo, use SPC's name, products, and colors. This isn't a generic platform — this is *SPC's auction platform*.

**Step 1: Buyer Homepage (3 min)**
- Open `localhost:3000` — show hero section, search bar
- Point out country selector with SI/HR/AT/DE/IT flags — "Your actual markets"
- Scroll down: category grid — "See: Containers, Climate Control, Construction Equipment — your product lines"
- Show CO2 counter: "Every container you resell avoids manufacturing a new one. This matters for EU corporate buyers."
- Switch language to German: "A buyer in Vienna sees this in German automatically"
- Switch to Croatian: "A buyer in Zagreb sees it in Croatian"
- Say: *"7 languages supported, covering your entire market from Slovenia to Serbia."*

**Step 2: SPC Team Lists Equipment (3 min)**
- Open seller portal (tab 2) — already logged in as seller@test.com
- Click "Create Lot"
- Fill in: "Pisarniski kontejner 6m — Custom Office Container", Category: Office Containers, Location: Ljubljana SI, Starting bid: EUR 4,500, Reserve: EUR 5,200
- Click "Create" -> success notification -> Submit for review
- Say: *"Your team creates the listing in 2 minutes. Photos, specs, starting price. Then it goes to review."*

**Step 3: Matej Approves (3 min)**
- Open admin dashboard (tab 3) — logged in as admin@test.com
- Say: *"This is your control center, Matej. Everything on one screen."*
- Show dashboard KPIs: active auctions, revenue, lots pending
- Navigate to "Lot Approval" -> show pending container listing -> click "Approve"
- Navigate to "Auctions" -> "Create Auction" -> set times -> create
- Say: *"You approve, you set the auction schedule, it goes live across all markets."*

**Step 4: A Buyer from Zagreb Bids (4 min)**
- Switch back to buyer-web tab -> login as buyer@test.com
- Search "kontejner" or "container"
- Open a lot detail page — show specs, location (Ljubljana), CO2 badge
- Place bid: EUR 4,600
- Say: *"A construction company in Zagreb found your container. They bid EUR 4,600. The minimum increment keeps it fair."*
- Say: *"If your starting price was EUR 4,500 fixed on shop.spc.si, you'd get EUR 4,500. With an auction, competitive demand can push this to EUR 5,500 or more."*

**Step 5: Smart Auction Features (3 min)**
- **Anti-sniping:** *"If someone bids in the last 2 minutes, the clock extends. No more losing money to last-second snipes. Every serious auction house uses this — now SPC has it too."*
- **Proxy bidding:** *"The buyer sets their max — say EUR 6,000 — and the system bids the minimum increment automatically. Like having an agent at a physical auction. This drives final prices up."*
- **Tiered increments:** *"EUR 1 increment for small items, EUR 100 for expensive machinery. Professional-grade."*
- Keep this conversational — NO code. Use the UI to illustrate.

**Step 6: Compliance Dashboard (3 min)**
- Switch to admin dashboard
- Navigate to "GDPR Requests" -> show the request management table
- Say: *"When a customer in Germany asks to delete their data — which is their legal right under GDPR — you handle it with one click. Try doing that manually."*
- Navigate to "Fraud Detection" -> show shill bidding detection
- Navigate to "System Health" -> show all 13 services with status
- Say: *"VAT is automatic. Selling a container from Ljubljana to a company in Zagreb? Reverse charge — 0% VAT, correctly calculated. To a German company? Same. 27 countries, all handled."*

**Step 7: CO2 Impact (2 min)**
- Switch to seller portal -> Navigate to "CO2 Report"
- Say: *"Every container resold instead of scrapped avoids 8-12 tonnes of CO2. Your buyers can report this for their ESG compliance. And you, as a platform operator, can market this — 'Buy green, buy used, buy on SPC.'"*
- Show PDF/CSV export buttons: *"Your sustainability report, downloadable."*

---

### ACT 3: Why SPC, Why Now (5 min)

**Slide — "Built for Europe, Not Bolted On":**
- Show a simplified EU map highlighting SI, HR, AT, DE, IT, BA, RS, HU
- "Each of these countries has different VAT rates, different GDPR enforcement, different languages"
- "This platform handles all of them from day one"
- "Building this from scratch would take 18-24 months and EUR 500K+ in development"

**Slide — SPC's Competitive Advantage:**

| SPC Today | SPC with Auction Platform |
|-----------|--------------------------|
| Fixed-price sales (shop.spc.si) | Auction + fixed price (higher revenue per item) |
| Manual quotes per customer | Automated bidding with instant settlement |
| Slovenia + some Balkans | 27 EU countries, 7 languages |
| VAT calculated manually | Automated cross-border VAT |
| No sustainability reporting | CO2 avoidance per sale, ESG reports |
| 900 customers | Open marketplace attracting new buyers across EU |
| Own products only | Platform for OTHER sellers too (commission revenue) |

**Slide — Platform Revenue Model:**
- **Transaction fee:** 3-8% of hammer price on every sale
- **Listing fees:** for premium placement (featured lots)
- **SaaS licensing:** other construction/container companies pay monthly to sell on SPC's platform
- **White-label:** SPC runs the platform, but other brands can have their own storefront
- *"This turns SPC from an equipment dealer into a marketplace operator. Think about what that means for your valuation."*

---

### ACT 4: Scale & Operations (3 min)

**Slide — "Your Team of 9 Can Run This":**
- Admin dashboard manages everything — no developers needed for daily operations
- Lot approval, auction scheduling, fraud monitoring — all point-and-click
- 13 independent services — if one has an issue, everything else keeps running
- Auto-scaling during peak auction closing times
- Every bid permanently recorded — perfect for any dispute or tax audit

**Slide — Infrastructure:**
- Runs on standard cloud services (AWS, Azure, or any Kubernetes provider)
- Full monitoring dashboard (Grafana) — real-time visibility
- Database per service — no single point of failure
- *"This isn't a WordPress plugin. This is production-grade infrastructure."*

---

### ACT 5: Next Steps & Ask (5 min)

**Slide — Customization for SPC (4-6 weeks):**
- Rebrand with SPC logo, colors (#004d71), fonts (Open Sans/Raleway) — see `docs/SPC_REBRANDING_PLAN.md`
- Configure SPC's product categories (Containers, Climate Control, Construction Equipment, Modular Structures)
- Set up SI/HR/AT/DE/IT as primary markets
- Add Slovenian (sl) and Croatian (hr) translations
- Connect SPC's existing customer database (900+ contacts)
- Configure payment processing (Adyen or local provider)
- Deploy to production (cloud hosting)

**Slide — Roadmap After Launch:**
- **Month 1-2:** SPC's own equipment on the platform (existing inventory)
- **Month 3-4:** Invite 5-10 partner sellers (construction companies in SI/HR)
- **Month 5-6:** Open marketplace to broader EU sellers
- **Month 7-12:** Mobile app, advanced analytics, broker features for intermediaries

**Slide — The Ask:**
- Options to present:
  - **Acquisition:** SPC acquires the platform outright — full ownership, full control
  - **License + customize:** SPC licenses the platform, we customize and maintain
  - **Partnership:** SPC operates the platform, revenue share on transactions
- Key number: *"Building this from scratch would cost EUR 500K-800K and 18-24 months. You can be live in 6 weeks."*

---

### Three Narrative Themes (repeat throughout)

1. **"From dealer to marketplace"** — SPC today sells its own equipment. With an auction platform, SPC becomes the platform where EVERYONE sells equipment. That's the Shopify story, applied to industrial auctions.

2. **"Your buyers are already in Europe — they just can't find you yet"** — 900 customers is great. But there are construction companies in Munich, contractors in Milan, logistics firms in Zagreb who need containers right now and don't know SPC exists. An auction platform with search and multi-language puts SPC in front of them.

3. **"Every container resold is a container not manufactured"** — The sustainability angle isn't just nice marketing. EU corporate procurement increasingly REQUIRES ESG reporting. A platform that tracks CO2 avoidance per sale gives SPC's buyers something their competitors can't offer.

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

# 4. Seed SPC-tailored demo data
bash scripts/seed-demo-data-spc.sh

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
| Matej asks about mobile | Low | *"Mobile-responsive is on the 3-month roadmap. The current web app works on tablets."* |
| Matej asks about price | High | Have 2-3 pricing options ready (see ACT 5). Don't lead with price — lead with value. |
| Matej asks about hosting costs | Medium | *"Cloud hosting runs EUR 300-800/month depending on traffic. Scales automatically."* |
| Matej asks "can I see containers with photos?" | Medium | *"Image upload via MinIO is built in. For this demo we're using placeholder images, but in production your team uploads photos directly."* |

### Backup Plans (in order of preference)
1. **Pre-recorded video** — 5-min screencast of full demo flow with SPC-specific data, recorded the evening before
2. **Screenshot slides** — every key screen captured with SPC products as fallback
3. **Narrated walkthrough** — use screenshots + talk through the flow conversationally. Business audiences accept this gracefully.
4. **Partial demo** — frontends render with hardcoded/seed data even if some backend services are down

---

## 6. Critical Files

### Bug Fixes (P0)
- `services/seller-service/src/main/kotlin/eu/auctionplatform/seller/infrastructure/persistence/repository/SellerProfileRepository.kt` — UPSERT_METRICS missing period column
- `frontend/seller-portal/src/views/LotDetailView.vue` — category UUID vs name display
- `frontend/buyer-web/` — watchlist heart icon color (identify exact component)

### New Files to Create
- `scripts/seed-demo-data-spc.sh` — SPC-tailored demo data (containers, equipment, auctions)
- `scripts/check-health.sh` — pre-demo service health validator
- `docs/presentation/slides.md` — slide content with speaker notes (SPC-customized)

### Rebranding Reference
- `docs/SPC_REBRANDING_PLAN.md` — comprehensive guide for full platform rebranding to SPC

---

## 7. Slide Content Deliverable

Create `docs/presentation/slides.md` — markdown content for each slide, ready to paste into Google Slides / PowerPoint / Keynote:

1. **Title slide** — "SPC Aukcije: Your Auction Platform for Containers & Industrial Equipment"
2. **SPC Growth slide** — EUR 3.28M -> EUR 4.19M (+28%), 900+ customers, A+ rating
3. **The Problem slide** — fixed prices leave money on the table, manual cross-border sales, no demand visibility
4. **The Opportunity slide** — EUR 150B market, 28.9% CAGR, competitor gap in containers & Balkans
5. **Demo transition** — *"Let me show you how this works — with your products, your categories, your market."*
6. **Compliance matrix** — GDPR, PSD2, DSA, AML, VAT — all built-in (12-18 month head start)
7. **From Dealer to Marketplace** — SPC Today vs SPC with Platform comparison table
8. **Revenue Model** — transaction fees, listing fees, SaaS licensing, white-label
9. **Roadmap** — 6-week deployment, Month 1-2 own inventory, Month 3-6 partner sellers, Month 7-12 open marketplace
10. **The Ask** — acquisition / license / partnership options, key number: EUR 500K-800K to build from scratch vs. 6 weeks to go live

Each slide includes: headline, 3-5 bullet points, and speaker notes with SPC-specific talking points.

---

## 8. SPC Demo Data (Seed Script)

The seed script (`scripts/seed-demo-data-spc.sh`) creates:

### Categories (matching SPC product lines)
- Office & Residential Containers (Pisarniski in bivalni kontejnerji)
- Shipping Containers (Ladijski kontejnerji)
- Sanitary Containers (Sanitarni kontejnerji)
- Modular Structures (Modularne strukture)
- Climate Control (Klimatska oprema)
- Construction Equipment & Machinery (Gradbena oprema in stroji)
- Metal Fencing & Accessories (Kovinske ograje in dodatki)

### Lots (10, SPC's actual products)

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

---

## 9. Verification

After all P0 tasks are complete:
1. Run `scripts/check-health.sh` — all 13 services show green
2. Run `scripts/seed-demo-data-spc.sh` — SPC lots, auctions, bids populated
3. Walk through full demo script (ACT 2, steps 1-7) end-to-end using SPC products
4. Verify: seller dashboard loads without 500, categories show names not UUIDs, watchlist hearts are red
5. Verify: SPC product names appear correctly in search, lot detail, bid panel
6. Test language switch: at minimum Slovenian -> German -> English
7. Time the full presentation — target 35-45 minutes
8. Record backup video of one successful demo run with SPC data
