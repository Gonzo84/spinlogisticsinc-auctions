# EU Auction Platform — Valuation Analysis

**Date:** 2026-03-13
**Purpose:** Comprehensive pricing analysis for potential sale of the platform

---

## 1. What You've Built

| Metric | Value |
|--------|-------|
| Total lines of code | ~110,000 |
| Kotlin backend | 41,000 lines across 272 files |
| Vue/TypeScript frontend | 54,000 lines across 370 files |
| SQL migrations | 84 files (1,583 lines) |
| Microservices | 13 (hexagonal architecture, event sourcing) |
| Frontend apps | 4 (Nuxt SSR + 3 Vite SPAs) |
| Shared libraries | 2 (kotlin-commons, nats-events with 30+ event types) |
| Infrastructure | Docker Compose, Helm charts, CI/CD, Prometheus/Grafana |
| Documentation | 7,700+ lines across 33 files |

---

## 2. Feature Maturity Assessment

| Feature Area | Rating | Business Value |
|---|---|---|
| Event Sourcing + CQRS | Production-Ready | Full audit trail, temporal queries — rare in market (~5-10% of platforms) |
| Anti-Sniping + Proxy Bidding | Production-Ready | Core competitive feature, verified end-to-end |
| WebSocket Real-Time Bidding | Production-Ready | Live auction experience with bid masking |
| Payment (Adyen + PSD2 SCA + EU VAT) | Near-Complete | All 27 EU VAT rates, 4 schemes, settlement flow |
| Compliance (GDPR/AML/DSA/Fraud) | Near-Complete | EU regulatory moat — increasingly required |
| Multi-Role RBAC (Keycloak + Casbin) | Production-Ready | 8 roles, per-service enforcement |
| Elasticsearch Search | Production-Ready | Full-text, faceting, suggestions, featured filtering |
| Media (S3 Presigned URLs) | Production-Ready | Direct browser-to-MinIO upload flow |
| Docker/Kubernetes/Helm | Production-Ready | Multi-stage builds, HPA, PDB, NetworkPolicy |
| CI/CD + Security Scanning | Production-Ready | 7-stage pipeline with OWASP + npm audit |
| i18n | MVP | 4 languages, infrastructure ready for 24 |
| CO2 Sustainability Tracking | MVP | Unique differentiator for ESG-conscious buyers |
| Broker Portal | MVP | Scaffolded, backend service present |

---

## 3. Market Context

### Competitor Landscape

| Company | Scale | Valuation/Revenue |
|---------|-------|-------------------|
| **RB Global (Ritchie Bros)** | $4.6B revenue, global | $19-20B market cap |
| **TBAuctions (Troostwijk + Surplex)** | EUR 1.5B hammer sales, 16 countries | PE-backed (Castik Capital, EUR 2B fund) |
| **Auction Technology Group** | $190M revenue (BidSpotter, Proxibid) | ~$500M market cap |
| **Catawiki** | EUR 105M revenue, 3.5M objects/yr | $607M valuation (2020 round) |
| **Surplex** | EUR 100-116M revenue, 12 countries | Acquired by TBAuctions (Aug 2024) |

### Market Size

- **Global online auction market:** $19.6B (2024) → $55.9B (2033), CAGR 8.4%
- **Hard asset equipment auctions:** $13.7B (2025) → $32.1B (2030), CAGR 18.5%
- **TBAuctions growth:** +50% hammer sales in 3 years, +65% cross-border in 3 years

### Revenue Model (Industry Standard)

| Stream | Typical Range |
|--------|---------------|
| Buyer premium | 10-25% of hammer (Troostwijk: 19%) |
| Seller commission | 2-20% of final sale |
| Listing/marketing fees | Flat fee per lot |
| SaaS/platform license | EUR 2,000-10,000/month per client |

---

## 4. Cost-to-Recreate Analysis

### Development Effort: ~10,000 person-hours

| Component | Estimate |
|-----------|----------|
| Architecture & design | 400-600 hrs |
| 13 microservices (hexagonal, event-sourced) | 3,500-5,000 hrs |
| 3 frontend apps (Nuxt SSR + 2 SPAs) | 2,000-3,000 hrs |
| Shared libs + NATS events | 400-600 hrs |
| Infrastructure (Docker, Helm, CI/CD, monitoring) | 600-1,000 hrs |
| Auth (Keycloak, Casbin, multi-role) | 400-600 hrs |
| Integration & testing | 600-1,000 hrs |

### Cost by Team Location

| Scenario | Blended Rate | Total Cost |
|----------|-------------|------------|
| Western EU agency | EUR 125/hr | **EUR 1,250,000-1,625,000** |
| Mixed team (Western architect + CEE devs) | EUR 70/hr | **EUR 700,000** |
| CEE-only team | EUR 50/hr | **EUR 500,000** |

### Timeline to Recreate: 9-18 months (team of 4-6 experienced devs)

---

## 5. IP Value Multipliers

| Factor | Impact | Applies? |
|--------|--------|----------|
| Event sourcing + CQRS (rare, premium) | +0.3-0.5x | Yes |
| EU compliance stack (GDPR, PSD2, DSA, AML) | +0.3-0.5x | Yes |
| Microservices architecture | +0.2-0.4x | Yes |
| Production infra (K8s-ready, observability) | +0.1-0.2x | Yes |
| Documentation quality | +0.1-0.2x | Yes |
| Pre-revenue (no customers) | -0.3-0.5x | Yes |
| Limited test coverage | -0.1-0.2x | Yes |
| Niche vertical | -0.1-0.2x | Partially |

**Composite multiplier: 1.5x - 2.5x** on cost-to-recreate

---

## 6. Valuation Summary

### Outright Sale

| Approach | Low | Mid | High |
|----------|-----|-----|------|
| Raw cost-to-recreate | EUR 500,000 | EUR 700,000 | EUR 1,500,000 |
| With IP multiplier (1.5-2.5x) | EUR 750,000 | EUR 1,400,000 | EUR 3,125,000 |
| **Realistic asking price (pre-revenue)** | **EUR 500,000** | **EUR 1,000,000** | **EUR 1,500,000** |
| If 1 anchor customer signed | EUR 1,000,000 | EUR 1,750,000 | EUR 2,500,000 |

### White-Label Licensing Alternative

| Model | Revenue |
|-------|---------|
| One-time license per deployment | EUR 100,000-300,000 |
| Monthly SaaS per client | EUR 2,000-10,000/month |
| Setup/onboarding fee | EUR 5,000-25,000 |
| Annual maintenance (15-25% of license) | EUR 15,000-75,000/year |

---

## 7. Recommendations

### Recommended asking price: EUR 750,000 - 1,200,000

This is anchored on:
- **EUR 700K+ cost-to-recreate** (mixed EU team, conservative estimate)
- **1.5-2x IP multiplier** for event sourcing, EU compliance, production infra
- **Discount** for pre-revenue status and limited test coverage

### To maximize sale price:

1. **Sign 1 paying customer** — even a small pilot at EUR 2-5K/month transforms the valuation from "code asset" to "operating business" (could add EUR 250-500K to price)
2. **Add test coverage** — even 30-40% backend coverage removes a major buyer objection
3. **Fix the 4 architectural gaps** — lot lifecycle, seller settlements, service-to-service auth (2-3 weeks work)
4. **Prepare a live demo environment** — hosted instance buyers can click through
5. **Target acquirers strategically:**
   - Existing auction houses wanting to go online (Troostwijk-like companies)
   - Industrial equipment dealers wanting a marketplace
   - PE-backed roll-up platforms (Castik Capital model)
   - EU fintech companies needing auction infrastructure

### Licensing may be more lucrative long-term:
- 5 clients at EUR 5,000/month = EUR 300,000/year recurring
- At 5x ARR SaaS multiple = EUR 1,500,000 valuation after year 1
- Lower risk for buyers (try before committing), higher total return for you

---

## 8. Detailed Market Research

### Recent Acquisitions/Deals (2021-2025)

| Deal | Year | Value | Notes |
|------|------|-------|-------|
| ATG acquires LiveAuctioneers | 2021 | $525M EV | ~1,600 auctioneers, 27K auctions/yr |
| Castik Capital acquires TBAuctions | 2021 | Undisclosed | From EPIC III fund (EUR 2B final close) |
| TBAuctions acquires Klaravik (Sweden) | 2022 | Undisclosed | Nordic expansion |
| TBAuctions acquires PS Auction + Auksjonen | 2022-23 | Undisclosed | Nordic consolidation |
| TBAuctions acquires Vavato (Belgium) | 2023 | Undisclosed | Belgian market entry |
| TBAuctions acquires Surplex (Germany) | Aug 2024 | Undisclosed | EUR 100M+ revenue, 220 employees |

### SaaS M&A Multiples (2024-2025)

- Median EV/Revenue: 4.1x (Q4 2024), up to 6.0x average
- Companies with >120% net revenue retention: 11.7x revenue
- Companies with >80% gross margin: 7.6x revenue
- ATG: ~2.6x revenue (currently depressed)
- RB Global: ~4.1x revenue

### Developer Rates (Europe, 2025-2026)

| Role | Western EU | Eastern EU (CEE) |
|------|-----------|------------------|
| Senior Kotlin/JVM backend | EUR 80-120/hr | EUR 45-70/hr |
| Senior Vue.js/Nuxt frontend | EUR 70-110/hr | EUR 40-65/hr |
| DevOps/Infrastructure | EUR 80-120/hr | EUR 45-70/hr |
| Solution architect | EUR 100-150/hr | EUR 50-80/hr |
| Mid-sized EU agency (blended) | EUR 125-200/hr | EUR 75-125/hr |

### White-Label Auction Platform Pricing (Competitors)

| Platform | Model | Price Range |
|----------|-------|-------------|
| AuctionMethod | Flat monthly, no commissions | From ~$100/month |
| iRostrum | Tiered by monthly lot volume | Pay-as-you-go or fixed |
| BidHarvest | Retainer + per-auction + % of sales | Custom |
| Webtron | Custom pricing | Contact for quote |
| General market range | Monthly SaaS | $22 to $999+/month |

---

## 9. Sources

- [TBAuctions Official](https://tbauctions.com/)
- [Castik Capital - TBAuctions Portfolio](https://www.castik.com/portfolio/tbauctions)
- [TBAuctions - Market Shifts Fuel Growth](https://www.prnewswire.com/news-releases/market-shifts-fuel-growth-of-global-online-equipment-auctions-302455509.html)
- [TBAuctions + Surplex Joint Forces](https://www.prnewswire.com/news-releases/tbauctions-and-surplex-join-forces-to-expand-across-europe-302216808.html)
- [RB Global Revenue](https://companiesmarketcap.com/ritchie-bros-auctioneers/revenue/)
- [ATG Revenue](https://stockanalysis.com/quote/lon/ATG/revenue/)
- [ATG LiveAuctioneers Acquisition ($525M)](https://www.traverssmith.com/knowledge/knowledge-container/travers-smith-advises-auction-technology-group-plc-on-the-proposed-525-million-acquisition-of-liveauctioneers-funded-by-a-244-million-cash-box-placing-and-new-debt-financing/)
- [Catawiki Crosses EUR 100M Revenue](https://www.catawiki.com/en/press/1860-catawiki-crosses-100m-revenue-mark)
- [Technavio - Hard Asset Equipment Online Auction Market](https://www.technavio.com/report/hard-asset-equipment-online-auction-market-size-industry-analysis)
- [European Developer Hourly Rates 2026](https://www.index.dev/blog/european-developer-hourly-rates)
- [SaaS Valuations 2025 - FE International](https://www.feinternational.com/blog/saas-metrics-value-saas-business)
- [Private SaaS Company Valuations - SaaS Capital](https://www.saas-capital.com/blog-posts/private-saas-company-valuations-multiples/)
- [B2B SaaS 2026 Valuation Multiples - Finerva](https://finerva.com/report/b2b-saas-2026-valuation-multiples/)
- [SaaS M&A Landscape 2024 - Aventis Advisors](https://aventis-advisors.com/saas-ma-landscape/)
- [IP Valuation: The Cost Method - WIPO](https://www.wipo.int/web-publications/intellectual-property-valuation-basics-for-technology-transfer-professionals/en/4-the-cost-method.html)
- [Software Valuation Using Cost of Replication - Quandary Peak](https://quandarypeak.com/2023/11/software-valuation-using-cost-of-replication-method/)
- [White-Label Pricing Models - Monetizely](https://www.getmonetizely.com/articles/white-label-pricing-models-maximizing-value-when-licensing-your-technology)
