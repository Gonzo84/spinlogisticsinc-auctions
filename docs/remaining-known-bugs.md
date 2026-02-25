# Remaining Known Bugs

**Date:** 2026-02-25
**Status:** Identified but not yet fixed

---

## Buyer Web (localhost:3000)

### BUG-01: Search for "Caterpillar" returns no results
- **File:** `frontend/buyer-web/pages/search.vue`
- **Description:** Lots use "CAT" not "Caterpillar". Elasticsearch full-text search doesn't match brand abbreviations/synonyms.
- **Severity:** Medium

### BUG-02: Lot detail shows category UUID instead of name
- **File:** `frontend/buyer-web/pages/lots/[id].vue:158`
- **Description:** Displays `10000000-0000-0000-0000-000000000001` instead of "Construction Machinery". No category name resolution from catalog-service.
- **Severity:** Medium

### BUG-03: Bid panel hidden on lot detail
- **File:** `frontend/buyer-web/components/auction/BidPanel.vue:76`
- **Description:** Lots are in `"approved"` status, not `"active"`. No auction has been created for any lot, so the bid input/Place Bid button never renders.
- **Severity:** High

### BUG-04: Lot timers show --:--:--
- **File:** `frontend/buyer-web/components/auction/Timer.vue`
- **Description:** `endTime` is empty string `""` because no auctions exist for the lots.
- **Severity:** Medium

### BUG-05: WebSocket connection fails silently
- **File:** `frontend/buyer-web/composables/useWebSocket.ts`
- **Description:** Tries to connect to `ws://localhost:8080/ws` but gateway-service WebSocket hub may not be fully wired. No real-time bid updates.
- **Severity:** Medium

### BUG-06: Search page category counts are hardcoded
- **File:** `frontend/buyer-web/pages/search.vue`
- **Description:** Filter sidebar shows fake counts like "Transport 1240", "Construction 1560" that don't reflect actual lot counts.
- **Severity:** Low

---

## Seller Portal (localhost:5174)

### BUG-07: ProfileView save buttons don't work via native click
- **File:** `frontend/seller-portal/src/views/ProfileView.vue`
- **Description:** Form `@submit.prevent` doesn't trigger from MCP/programmatic clicks. Fixed with `nextTick()` but underlying Vue reactivity batching means rapid double-clicks could skip the success banner.
- **Severity:** Low

### BUG-08: Lot detail shows category UUID
- **File:** `frontend/seller-portal/src/views/LotDetailView.vue`
- **Description:** Same as BUG-02. Category shows UUID on lot cards and detail pages instead of human-readable name.
- **Severity:** Medium

### BUG-09: Dashboard Recent Activity is hardcoded
- **File:** `frontend/seller-portal/src/views/DashboardView.vue`
- **Description:** The activity feed (bids, approvals, sales) shows fake static data, not real events.
- **Severity:** Low

### BUG-10: Dashboard KPI trend percentages are hardcoded
- **File:** `frontend/seller-portal/src/views/DashboardView.vue`
- **Description:** "+3 this week", "+12%", "+5 this month" are static strings, not computed from real data.
- **Severity:** Low

### BUG-11: Monthly Revenue chart is empty
- **File:** `frontend/seller-portal/src/views/AnalyticsView.vue`
- **Description:** Seller-service `/sellers/me/analytics` returns empty `monthlyRevenue[]` for test seller. Chart renders but shows nothing.
- **Severity:** Low

---

## Admin Dashboard (localhost:5175)

### BUG-12: No GET /users list endpoint
- **File:** `frontend/admin-dashboard/src/composables/useApi.ts`
- **Description:** Admin dashboard tries to list users but user-service only has `GET /users/me` and `GET /users/{id}`. User management page would fail.
- **Severity:** High

### BUG-13: /admin/lots/pending doesn't exist
- **File:** `frontend/admin-dashboard/src/composables/useApi.ts`
- **Description:** Must use `GET /lots?status=PENDING_REVIEW` from catalog-service instead.
- **Severity:** High

---

## Backend / Infrastructure

### BUG-14: Casbin filter disabled
- **File:** All services `src/main/resources/casbin_policy.csv`
- **Description:** Policies use `keyMatch2` with incompatible `**` glob patterns. RBAC enforcement is effectively bypassed on all services.
- **Severity:** Critical

### BUG-15: NATS event consumers may miss events
- **File:** `services/seller-service/src/main/kotlin/.../infrastructure/`
- **Description:** Seller-service doesn't sync lot data from catalog-service via NATS events. Lot changes in catalog-service aren't reflected in seller-service dashboard counts.
- **Severity:** Medium

### BUG-16: Lot creation requires brand field
- **File:** `services/catalog-service/src/main/kotlin/.../api/dto/LotDtos.kt`
- **Description:** Missing `brand` field causes 400 error. Frontend sends it but it's not documented and easy to forget in API calls.
- **Severity:** Low

### BUG-17: Services don't start via java -jar
- **File:** All services `src/main/resources/application.yml`
- **Description:** Missing config values (NATS, Keycloak URLs). Must use `gradlew quarkusDev` with application.yml defaults.
- **Severity:** Medium

### BUG-18: Auction-engine has no active auctions
- **File:** `services/auction-engine/`
- **Description:** No auction has been created/started for any lot. The entire bidding flow (place bid, auto-bid, anti-sniping, proxy bidding) is untestable without manually creating auctions via API.
- **Severity:** High

---

## Cosmetic / Minor

### BUG-19: Seller name shows "auctions" on buyer lot detail
- **File:** `frontend/buyer-web/pages/lots/[id].vue`
- **Description:** Seller info displays truncated brand name instead of company name.
- **Severity:** Low

### BUG-20: buyer-web post.logout.redirect.uris may need updating
- **File:** Keycloak `buyer-web` client config
- **Description:** May need updating like seller-portal did. Currently works because Nuxt handles logout differently.
- **Severity:** Low

---

## Summary by Severity

| Severity | Count |
|----------|-------|
| Critical | 1 |
| High | 4 |
| Medium | 6 |
| Low | 9 |
| **Total** | **20** |
