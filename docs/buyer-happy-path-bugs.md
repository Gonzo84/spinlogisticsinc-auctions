# Buyer Happy Path - Bug Report

**Date:** 2026-02-25
**Tester:** Claude Code (automated via Chrome DevTools MCP)
**Application:** Buyer Web (http://localhost:3000)
**Environment:** Fresh Docker Compose stack (all volumes recreated), all backend services running on ports 8080-8092, Keycloak on 8180

---

## Summary

| Severity | Count |
|----------|-------|
| Critical (blocks core flow) | 4 |
| Major (feature broken) | 7 |
| Minor (cosmetic/UX) | 4 |
| **Total** | **15** |

| Happy Path Step | Status |
|-----------------|--------|
| 1.1 Landing Page | PARTIAL - page loads but content sections empty |
| 1.2 Authentication | PASS (with warnings) |
| 1.3 Browse & Search | FAIL - no results displayed |
| 1.4 View Lot Detail | FAIL - wrong API endpoint called |
| 1.5 Place a Bid | BLOCKED - lot detail doesn't load |
| 1.6 Navigation Pages | PARTIAL - missing pages, profile save broken |
| 1.7 Logout | PASS |

---

## Critical Bugs

### BUG-01: Lot detail page calls wrong API endpoint
- **Step:** 1.4 View Lot Detail
- **Severity:** Critical
- **URL:** `http://localhost:3000/lots/{lotId}`
- **Expected:** Frontend calls `GET /api/v1/lots/{lotId}` (catalog-service)
- **Actual:** Frontend calls `GET /api/v1/auctions/{lotId}` (auction-engine), which returns 500
- **Result:** Page shows "Error: This lot could not be found" for all lots
- **Impact:** Entire lot detail view is broken; bid placement is also blocked since the bid panel is on this page
- **Console Error:** `API Server Error: 500`
- **Network:** `GET http://localhost:8080/api/v1/auctions/019c93aa-3764-73dc-919b-bf4ce3fd34a4 → 500`

### BUG-02: Search index (Elasticsearch) not populated - NATS sync broken
- **Step:** 1.3 Browse & Search
- **Severity:** Critical
- **Expected:** Lots approved in catalog-service should appear in search results
- **Actual:** `GET /api/v1/search/lots` always returns `{"items":[],"totalCount":0}` even with 3 approved lots in catalog
- **Root Cause:** NATS JetStream streams not auto-created; catalog-service lot events never reach search-service
- **Impact:** Search returns zero results; homepage "Featured Auctions" and "Newly Listed" sections are empty
- **Network:** `GET http://localhost:8080/api/v1/search/lots?q=Caterpillar&sort=closing_soonest&page=1 → 200 (empty)`

### BUG-03: Notification service returns 500 on every page
- **Step:** All authenticated pages
- **Severity:** Critical
- **Expected:** `GET /api/v1/notifications?page=1&limit=20` returns notifications or empty list
- **Actual:** Returns 500 Internal Server Error on every page load (2x per page)
- **Impact:** Console flooded with errors; notification bell non-functional
- **Network:** `GET http://localhost:8080/api/v1/notifications?page=1&limit=20 → 500`
- **Response:** `{"status":500,"title":"Internal Server Error","instance":"/api/v1/notifications"}`

### BUG-04: WebSocket endpoint returns 404
- **Step:** 1.4 View Lot Detail (and likely other authenticated pages)
- **Severity:** Critical
- **Expected:** WebSocket connection to `ws://localhost:8080/ws` succeeds for real-time bid updates
- **Actual:** WebSocket handshake fails with 404; repeated reconnect attempts flood console
- **Console Error:** `WebSocket connection to 'ws://localhost:8080/ws?token=...' failed: Error during WebSocket handshake: Unexpected response code: 404`
- **Impact:** No real-time updates for bids, auction status, or notifications; generates 4+ error messages per page view

---

## Major Bugs

### BUG-05: Vue render error on search results page
- **Step:** 1.3 Browse & Search
- **Severity:** Major
- **Expected:** When search returns empty results, show "No results found" empty state
- **Actual:** Vue throws `Unhandled error during execution of render function` at `search.vue:113`; skeleton loaders remain stuck permanently
- **Console Error:** `Uncaught (in promise)` at `search.vue:113:27` in `_sfc_render`, originating from `fetchResults` at `search.vue:253:18`
- **Impact:** User sees frozen skeleton loader cards instead of a useful empty state message

### BUG-06: "My Bids" page missing (spec requirement)
- **Step:** 1.6 Navigation Pages
- **Severity:** Major
- **Expected:** User dropdown contains "My Bids" link; `/my/bids` page exists showing placed bids
- **Actual:** Dropdown shows "My Purchases" instead; `/my/bids` returns Nuxt 404 page
- **Impact:** Users cannot view their bid history

### BUG-07: "Watchlist" page missing (spec requirement)
- **Step:** 1.6 Navigation Pages
- **Severity:** Major
- **Expected:** User dropdown contains "Watchlist" link; `/watchlist` page exists showing watched lots
- **Actual:** "Watchlist" not in dropdown at all; `/watchlist` returns Nuxt 404 page
- **Impact:** Users cannot save/track lots they're interested in

### BUG-08: Profile save fails - 405 Method Not Allowed
- **Step:** 1.6 Navigation Pages (Profile)
- **Severity:** Major
- **Expected:** Clicking "Save Changes" on profile form persists user data
- **Actual:** Error shown: `[PATCH] "http://localhost:8080/api/v1/users/me": 405 Method Not Allowed`
- **Root Cause:** Gateway or user-service does not accept PATCH requests on `/api/v1/users/me`; may need PUT instead, or the gateway route is missing PATCH support
- **Impact:** Users cannot update their profile information

### BUG-09: "My Purchases" API endpoint missing
- **Step:** 1.6 Navigation Pages
- **Severity:** Major
- **Expected:** `GET /api/v1/users/me/purchases` returns user's won lots
- **Actual:** Returns 404: `Unable to find matching target resource method`
- **Impact:** Page handles gracefully (shows empty state) but data can never load even if purchases exist

### BUG-10: Lot creation ignores startingBidAmount and reservePrice fields
- **Step:** Verified via API during test data setup
- **Severity:** Major
- **Expected:** Creating a lot with `startingBidAmount: 45000` sets `startingBid` to 45000
- **Actual:** `startingBid` is always `1.00` regardless of input; `reservePrice` is always `null`
- **API Request:** `POST /api/v1/lots` with `"startingBidAmount": 45000, "reservePriceAmount": 55000`
- **API Response:** `"startingBid": 1.00, "reservePrice": null`
- **Impact:** All lots appear with incorrect pricing

### BUG-11: Lot submit/approve uses wrong HTTP method in HAPPY_PATHS.md (PUT vs POST)
- **Step:** Verified via API during test data setup
- **Severity:** Major
- **Expected:** `PUT /api/v1/lots/{id}/submit` works (as used by seller-portal frontend)
- **Actual:** PUT returns 405; only POST works: `POST /api/v1/lots/{id}/submit`
- **Impact:** If seller-portal uses PUT (as previously documented), lot submission flow breaks

---

## Minor Bugs

### BUG-12: SSR hydration mismatch in Navbar component
- **Step:** Every page load after authentication
- **Severity:** Minor
- **Console Warning:** `[Vue warn]: Hydration node mismatch: rendered on server: [node Comment] - expected on client: div at <Navbar>`
- **Console Error:** `Hydration completed but contains mismatches.`
- **Root Cause:** Server renders unauthenticated navbar (comment node), client hydrates with authenticated navbar (div) — timing mismatch between SSR and OIDC token availability
- **Impact:** Brief flash of content; no functional impact but degrades perceived quality

### BUG-13: OIDC hash fragment not cleaned from URL after login
- **Step:** 1.2 Authentication
- **Severity:** Minor
- **Expected:** After successful OIDC redirect, URL should be cleaned to `http://localhost:3000/`
- **Actual:** URL retains long hash fragment: `http://localhost:3000/#state=...&session_state=...&iss=...&code=...`
- **Console Warning:** `[Vue Router warn]: The selector "#state=..." is invalid`
- **Impact:** Ugly URL; Vue Router warning in console; could cause issues if URL is shared

### BUG-14: Homepage "Featured Auctions" and "Newly Listed" sections permanently empty
- **Step:** 1.1 Landing Page
- **Severity:** Minor (consequence of BUG-02)
- **Expected:** Sections show lot cards fetched from API
- **Actual:** "Featured Auctions" shows disabled carousel arrows, no cards; "Newly Listed" shows nothing
- **Root Cause:** Depends on auction-engine and search-service data which are empty
- **Impact:** Homepage appears barren/incomplete to new visitors

### BUG-15: Category counts on homepage are hardcoded
- **Step:** 1.1 Landing Page
- **Severity:** Minor
- **Expected:** Category counts reflect actual lot counts (or are omitted)
- **Actual:** Shows hardcoded counts like "Transport (1240)", "Construction (1560)" when actual database has 0 lots in each
- **Impact:** Misleading to users; suggests data exists when it doesn't

---

## Console Error Summary

### Errors present on EVERY authenticated page:
| Error | Source | Count per page |
|-------|--------|----------------|
| `Failed to load resource: 500` | `/api/v1/notifications` | 2 |
| `API Server Error: 500 [object Object]` | notifications fetch | 2 |
| `Hydration node mismatch` | Navbar SSR vs client | 2 |
| `Hydration completed but contains mismatches` | Vue hydration | 1 |

### Additional errors on Lot Detail page:
| Error | Source | Count |
|-------|--------|-------|
| `WebSocket connection failed: 404` | `ws://localhost:8080/ws` | 4+ (retries) |
| `WebSocket error` | WebSocket reconnect | 4+ |
| `Failed to load resource: 500` | `/api/v1/auctions/{id}` | 2 |

### Additional errors on Search page:
| Error | Source | Count |
|-------|--------|-------|
| `Unhandled error during execution of render function` | `search.vue:113` | 1 |
| `Unhandled error during execution of component update` | Search component | 1 |
| `Uncaught (in promise)` | `search.vue:113` | 1 |

---

## Network Request Failures

| Endpoint | HTTP Status | Page | Root Cause |
|----------|-------------|------|------------|
| `GET /api/v1/notifications?page=1&limit=20` | 500 | All authenticated | notification-service internal error |
| `GET /api/v1/auctions/{lotId}` | 500 | Lot detail | Wrong endpoint (should be `/lots/`) |
| `GET /api/v1/users/me/purchases` | 404 | My Purchases | Endpoint not implemented |
| `PATCH /api/v1/users/me` | 405 | Profile | Method not allowed |
| `ws://localhost:8080/ws` | 404 | Lot detail | WebSocket endpoint not configured |

---

## Test Data Notes

During testing, 3 lots were created via API to validate search and lot detail:

1. **Caterpillar D6 Bulldozer 2019** (ID: `019c93aa-3764-73dc-919b-bf4ce3fd34a4`) - Approved, Rotterdam NL
2. **Komatsu PC200 Excavator 2020** (ID: `019c93ab-3eba-7011-bbe5-793bdf4d6732`) - Approved, Berlin DE
3. **Volvo FH16 Truck 2021** (ID: `019c93ab-3f21-71c5-a2b8-f7edfc07bae6`) - Approved, Amsterdam NL

All lots have `startingBid: 1.00` (BUG-10) and are in catalog but not in search index (BUG-02).

---

## Recommendations (Priority Order)

1. **Fix lot detail API endpoint** (BUG-01) — Change frontend from `/auctions/{id}` to `/lots/{id}`
2. **Fix notification-service 500** (BUG-03) — Debug and fix the internal error
3. **Fix search-service sync** (BUG-02) — Auto-create NATS JetStream streams or add manual reindex
4. **Fix lot startingBid/reservePrice** (BUG-10) — Ensure create lot DTO maps amount fields
5. **Add WebSocket endpoint to gateway** (BUG-04) — Implement or configure ws:// proxy
6. **Fix profile save** (BUG-08) — Support PATCH on `/users/me` or change frontend to PUT
7. **Implement missing pages** (BUG-06, BUG-07) — My Bids, Watchlist
8. **Fix search empty state** (BUG-05) — Handle empty results gracefully in search.vue
9. **Clean OIDC hash** (BUG-13) — Strip hash fragment after token extraction
10. **Fix SSR hydration** (BUG-12) — Defer auth-dependent rendering or use client-only wrapper
