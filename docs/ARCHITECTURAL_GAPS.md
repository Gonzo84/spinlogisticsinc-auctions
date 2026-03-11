# Architectural Gaps Analysis

**Date:** 2026-03-11
**Branch:** mvp
**Analyzed by:** Claude Code (4 parallel deep-analysis agents)

---

## Executive Summary

The platform's **happy path demo flow works end-to-end** (lot creation through settlement), but has significant gaps in event-driven integrations, security, and cross-service data synchronization. The most critical gaps are:

1. **Seller-service NATS settlement sync** — sellers can't see settlements
2. **Lot status lifecycle incomplete** — lots never transition to ACTIVE/SOLD
3. **No service-to-service authentication** — inter-service HTTP calls are unauthenticated
4. **Missing broker portal** — backend APIs exist but no frontend
5. **Several frontend endpoints call non-existent backend APIs**

---

## Table of Contents

1. [Happy Path Demo Gaps](#1-happy-path-demo-gaps)
2. [Backend Service Gaps](#2-backend-service-gaps)
3. [Frontend Gaps](#3-frontend-gaps)
4. [Infrastructure & Gateway Gaps](#4-infrastructure--gateway-gaps)
5. [Security Gaps](#5-security-gaps)
6. [Observability & CI/CD Gaps](#6-observability--cicd-gaps)
7. [Priority Matrix](#7-priority-matrix)

---

## 1. Happy Path Demo Gaps

### Demo Flow Status

| Step | Description | Status | Gap |
|------|------------|--------|-----|
| 1-2 | Seller creates lot, admin approves | PASS | None |
| 3 | Admin creates auction | PARTIAL | Lot status not updated to ACTIVE |
| 4 | Buyer searches and finds lot | PASS | None |
| 5-6 | Competitive bidding + anti-sniping + auto-close | PASS | None |
| 7 | Admin awards winner | PASS | None |
| 8-9 | Checkout + payment | PASS | None |
| 10 | Admin settles payment | PASS | None |
| 11 | Seller views settlement | FAIL | Seller-service has no NATS consumer |

### GAP-DEMO-01: Lot Status Never Transitions to ACTIVE (MAJOR)

- **Services:** catalog-service, auction-engine
- **Issue:** When an auction is created for an approved lot, catalog-service does NOT update the lot status to ACTIVE. The lot remains APPROVED throughout the auction lifecycle.
- **Root cause:** Catalog-service has no NATS consumer for auction events. `LotService.assignToAuction()` exists but is never invoked.
- **Impact:** Seller dashboard shows wrong lot status; lot filtering by status is broken.
- **Fix:** Create `AuctionEventCatalogConsumer.kt` in catalog-service to listen for auction creation events and call `LotService.assignToAuction()`.

### GAP-DEMO-02: Lot Status Never Transitions to SOLD (MAJOR)

- **Services:** catalog-service, auction-engine, payment-service
- **Issue:** After award and checkout, the lot status remains ACTIVE/APPROVED instead of SOLD.
- **Root cause:** No service publishes a "lot sold" event that catalog-service consumes. The `LotAwardedEvent` is consumed by payment-service but not by catalog-service.
- **Impact:** Sold lots appear as still available; inventory management broken.
- **Fix:** Add catalog-service consumer for `LotAwardedEvent` to transition lot to SOLD.

### GAP-DEMO-03: Seller Cannot See Settlements (CRITICAL)

- **Services:** seller-service, payment-service
- **Issue:** After admin settles a payment, seller-portal shows "No settlements found." The settlement exists in payment-service DB but seller-service never receives it.
- **Root cause:** Payment-service settlement flow doesn't publish `PaymentSettledEvent` to NATS. The `PaymentEventSellerConsumer` in seller-service exists but has no events to consume.
- **Impact:** Sellers have no visibility into their earnings. Core business flow broken.
- **Fix:** Add `PaymentSettledEvent` to nats-events, publish it from payment-service settlement flow, ensure seller-service consumer processes it.

### GAP-DEMO-04: buyer2@test.com Credentials Require Keycloak Restart (MINOR)

- **Issue:** buyer2 user exists in realm JSON with correct password, but Keycloak requires container restart with `KC_SPI_IMPORT_REALM_FILE_STRATEGY: OVERWRITE` to reimport.
- **Impact:** Multi-buyer competitive bidding demo requires infrastructure restart.
- **Workaround:** Restart Keycloak container before demo.

### GAP-DEMO-05: Keycloak SSO Session Sharing (INFO)

- **Issue:** Switching between portals (buyer-web, seller-portal, admin-dashboard) shares the SSO session. Logging into admin-dashboard as admin@test.com means seller-portal also shows admin@test.com.
- **Impact:** Demo requires explicit logout/re-login between portals.
- **Workaround:** Use different browsers or incognito windows per portal.

---

## 2. Backend Service Gaps

### 2.1 Missing NATS Consumers

| Gap ID | Publisher | Event | Missing Consumer | Severity |
|--------|-----------|-------|-----------------|----------|
| GAP-BE-01 | payment-service | `PaymentSettledEvent` | seller-service | CRITICAL |
| GAP-BE-02 | auction-engine | Auction created event | catalog-service (lot→ACTIVE) | MAJOR |
| GAP-BE-03 | auction-engine | `LotAwardedEvent` | catalog-service (lot→SOLD) | MAJOR |
| GAP-BE-04 | payment-service | `NonPaymentPenaltyEvent` | user-service (block buyer) | MAJOR |
| GAP-BE-05 | payment-service | `LotRelistRequestedEvent` | catalog-service (relist lot) | MAJOR |
| GAP-BE-06 | compliance-service | `GdprErasureEvent` | user/payment/auction services | MAJOR |
| GAP-BE-07 | auction-engine | `ReserveMetEvent` | notification-service | **FIXED** (2026-03-11) |
| GAP-BE-08 | auction-engine | `ProxyBidTriggeredEvent` | notification-service (auto-bid notifications) | **FIXED** (2026-03-11) |
| GAP-BE-09 | payment-service | Settlement events | analytics-service | MAJOR |

### 2.2 Missing or Incomplete REST Endpoints

| Gap ID | Service | Endpoint | Issue | Severity |
|--------|---------|----------|-------|----------|
| GAP-BE-10 | seller-service | `GET /sellers/me/settlements` | No endpoint to retrieve settlement history | MAJOR |
| GAP-BE-11 | seller-service | `GET /sellers/me/settlements/monthly` | Frontend calls it but doesn't exist | MEDIUM |
| GAP-BE-12 | user-service | `GET /users` (list) | **FIXED** — list endpoint exists with search, status filter, pagination | **FIXED** |
| GAP-BE-13 | compliance-service | `/compliance/fraud/alerts/**` | **FIXED** (2026-03-11) — real domain model, DB persistence, full CRUD | **FIXED** |
| GAP-BE-14 | payment-service | `POST /payments/{id}/refund` | Frontend calls it but doesn't exist | MEDIUM |
| GAP-BE-15 | payment-service | `POST /payments/{id}/reminder` | Frontend calls it but doesn't exist | MEDIUM |
| GAP-BE-16 | gateway-service | Health service restart | **FIXED** — backend exists; frontend stub replaced with real API call (2026-03-11) | **FIXED** |

### 2.3 Missing Inter-Service Integrations

| Gap ID | From → To | Issue | Severity |
|--------|-----------|-------|----------|
| GAP-BE-17 | seller-service → catalog-service | Seller lots not synced; CatalogEventSellerConsumer never initialized | MAJOR |
| GAP-BE-18 | payment-service → auction-engine | AuctionLotLookupService has no auth tokens | CRITICAL |
| GAP-BE-19 | notification-service | Settlement notification handler reads wrong field names (settlementAmount vs netAmount) | MAJOR |

### 2.4 Database & Schema Gaps

| Gap ID | Service | Issue | Severity |
|--------|---------|-------|----------|
| GAP-BE-20 | Multiple | Missing indexes on read model tables (user_bids, seller_lots, auction_read_model) | MINOR |
| GAP-BE-21 | payment-service | Settlement outbox publishing incomplete | MAJOR |

### 2.5 Shared Library Gaps

| Gap ID | Library | Issue | Severity |
|--------|---------|-------|----------|
| GAP-BE-22 | nats-events | Missing `PaymentSettledEvent` class | CRITICAL |
| GAP-BE-23 | nats-events | Missing `AuctionCancelledEvent`, `DepositRefundedEvent`, `BuyerBlockedEvent` | MEDIUM |
| GAP-BE-24 | kotlin-commons | Missing ID value objects: PaymentId, SettlementId, BrokerId | LOW |

---

## 3. Frontend Gaps

### 3.1 Broker Portal (95% COMPLETE — FIXED 2026-03-11)

- **Status:** **FIXED** — Broker portal exists with full views, composables, types. BulkIntakeView and ProfileView stubs replaced with real implementations. Keycloak redirect URI fixed.
- **Backend APIs ready:** `GET /brokers/me/dashboard`, `GET /brokers/leads`, `POST /brokers/leads/{id}/assign-lot`
- **Keycloak:** `broker-app` client configured, redirect URIs updated.

### 3.2 Frontend Calling Non-Existent Backend Endpoints

| Frontend | Composable | Endpoint Called | Status |
|----------|-----------|----------------|--------|
| seller-portal | `useSettlements.ts` | `/sellers/me/settlements/monthly` | Does not exist |
| admin-dashboard | `useSystemHealth.ts` | `/health/services/{name}/restart` | **FIXED** (2026-03-11) |
| admin-dashboard | `usePayments.ts` | `POST /payments/{id}/refund` | Does not exist |
| admin-dashboard | `usePayments.ts` | `POST /payments/{id}/reminder` | Does not exist |
| admin-dashboard | `useCompliance.ts` | `/compliance/fraud/alerts` (full CRUD) | **FIXED** (2026-03-11) |

### 3.3 Real-Time Updates Missing

| Frontend | WebSocket | Issue | Severity |
|----------|-----------|-------|----------|
| buyer-web | Implemented | Subscribes to bid, extension, close events | PASS |
| seller-portal | **FIXED** (2026-03-11) | WebSocket composable + settlement/lot status notifications | **FIXED** |
| admin-dashboard | **FIXED** (2026-03-11) | WebSocket composable + fraud alerts, payment updates, live bid chart | **FIXED** |

### 3.4 i18n Coverage

| Frontend | i18n | Languages | Issue | Severity |
|----------|------|-----------|-------|----------|
| buyer-web | vue-i18n | 7 (en, sl, hr, de, it, sr, hu) | Some non-English keys are English placeholders (`biddingRestricted`, `buyerAccountRequired`) | LOW |
| seller-portal | None | English only | No i18n system | LOW |
| admin-dashboard | None | English only | No i18n system | LOW |

### 3.5 Error Handling Inconsistencies

- **seller-portal:** Most errors silently set `error.value = null` on catch, hiding failures from users.
- **admin-dashboard:** Better — uses error messages in catch blocks.
- **buyer-web:** Uses `useToast()` for error notifications — best practice.
- **Severity:** LOW-MEDIUM

---

## 4. Infrastructure & Gateway Gaps

### 4.1 Gateway Proxy

| Gap ID | Issue | Severity |
|--------|-------|----------|
| GAP-INFRA-01 | Missing HEAD method handler in ApiProxyResource | HIGH |
| GAP-INFRA-02 | No per-endpoint timeout configuration (hardcoded 30s) | LOW |
| GAP-INFRA-03 | Query parameter special character encoding not validated | MEDIUM |

### 4.2 Docker Compose

| Gap ID | Issue | Severity |
|--------|-------|----------|
| GAP-INFRA-04 | otel-collector not in service `depends_on` — services start before collector | MEDIUM |
| GAP-INFRA-05 | Inter-service URLs only in payment-service.yml, not centralized in .env | LOW |
| GAP-INFRA-06 | No volume mounts for runtime Casbin config overrides | LOW |

### 4.3 Keycloak

| Gap ID | Issue | Severity |
|--------|-------|----------|
| GAP-INFRA-07 | **FIXED** (2026-03-11) — 10 bearer-only OIDC clients added to realm JSON + creation script | **FIXED** |
| GAP-INFRA-08 | **FIXED** (2026-03-11) — mobile-app client removed from realm JSON | **FIXED** |
| GAP-INFRA-09 | broker-app client configured but no broker frontend | MEDIUM |

### 4.4 NATS Configuration

| Gap ID | Issue | Severity |
|--------|-------|----------|
| GAP-INFRA-10 | No dead-letter queue naming convention or monitoring | MEDIUM |
| GAP-INFRA-11 | No exponential backoff between consumer redelivery attempts | MEDIUM |
| GAP-INFRA-12 | Missing `PaymentSettledEvent` subject in NatsSubjects | CRITICAL |

---

## 5. Security Gaps

| Gap ID | Issue | Severity | Notes |
|--------|-------|----------|-------|
| GAP-SEC-01 | No service-to-service authentication (AuctionLotLookupService) | CRITICAL | Any network service can spoof internal calls |
| GAP-SEC-02 | CORS origins hardcoded to localhost in all services | MEDIUM | Requires rebuild for production |
| GAP-SEC-03 | No rate limiting on inter-service HTTP calls | MEDIUM | DDoS between services possible |
| GAP-SEC-04 | No CSRF protection (mitigated by JWT auth, not cookie-based) | LOW | Lower risk with bearer tokens |
| GAP-SEC-05 | Casbin filter disabled — keyMatch2 incompatible patterns | CRITICAL | Authorization enforcement weakened |

---

## 6. Observability & CI/CD Gaps

### 6.1 Observability

| Gap ID | Issue | Severity |
|--------|-------|----------|
| GAP-OBS-01 | AuctionLotLookupService HTTP calls don't propagate trace headers | HIGH |
| GAP-OBS-02 | No custom metrics for outbox publish rate, consumer lag, redelivery count | MEDIUM |
| GAP-OBS-03 | NATS consumer errors logged generically — no structured failure tracking | LOW |

### 6.2 CI/CD

| Gap ID | Issue | Severity |
|--------|-------|----------|
| GAP-CI-01 | **FIXED** (2026-03-11) — NATS health check uses proper /healthz endpoint | **FIXED** |
| GAP-CI-02 | **FIXED** (2026-03-11) — Frontend test job added for all 3 apps | **FIXED** |
| GAP-CI-03 | **FIXED** (2026-03-11) — Keycloak service container added to CI | **FIXED** |
| GAP-CI-04 | **FIXED** (2026-03-11) — Trivy scans expanded to all 13 services via matrix | **FIXED** |
| GAP-CI-05 | **FIXED** (2026-03-11) — E2E tests use Playwright route mocking for API fixtures | **FIXED** |

---

## 7. Priority Matrix

### P0 — Blocks Demo / Critical Business Flow

| ID | Gap | Service(s) | Effort |
|----|-----|-----------|--------|
| GAP-DEMO-03 / GAP-BE-01 / GAP-BE-22 | Seller can't see settlements (missing NATS event + consumer) | payment-service, seller-service, nats-events | Medium |
| GAP-SEC-01 / GAP-BE-18 | No service-to-service authentication | payment-service, all services | Large |
| GAP-SEC-05 | **FIXED** — all 442 policy lines use compatible keyMatch2 `:param` patterns | **FIXED** |
| GAP-INFRA-12 | Missing PaymentSettledEvent NATS subject | shared/nats-events | Small |

### P1 — Breaks Key Functionality

| ID | Gap | Service(s) | Effort |
|----|-----|-----------|--------|
| GAP-DEMO-01 / GAP-BE-02 | Lot never becomes ACTIVE | catalog-service | Medium |
| GAP-DEMO-02 / GAP-BE-03 | Lot never becomes SOLD | catalog-service | Small |
| GAP-BE-04 | Non-payment penalty not enforced | user-service, payment-service | Medium |
| GAP-BE-06 | GDPR erasure not propagated | Multiple services | Large |
| GAP-BE-08 | **FIXED** (2026-03-11) — ProxyBidTriggered notification consumer added | **FIXED** |
| GAP-BE-09 | Settlement analytics missing | analytics-service | Small |
| GAP-BE-10 | Seller settlement REST endpoints missing | seller-service | Small |
| GAP-BE-17 | Seller lots not synced from catalog | seller-service | Medium |
| GAP-BE-19 | Notification settlement field mismatch | notification-service | Small |
| GAP-FE-01 (3.1) | **FIXED** (2026-03-11) — Broker portal complete with BulkIntake + Profile views | **FIXED** |

### P2 — Important but Non-Blocking

| ID | Gap | Effort |
|----|-----|--------|
| GAP-BE-11-16 | Missing backend endpoints called by frontend | Medium |
| GAP-INFRA-01 | Gateway missing HEAD method | Small |
| GAP-INFRA-07 | **FIXED** (2026-03-11) — 10 OIDC clients added | **FIXED** |
| GAP-OBS-01 | Missing trace header propagation in inter-service calls | Small |
| GAP-CI-05 | No frontend E2E tests in CI | Large |
| Real-time updates | **FIXED** (2026-03-11) — WebSocket composables + NATS forwarders for both portals | **FIXED** |

### P3 — Nice to Have

| ID | Gap | Effort |
|----|-----|--------|
| GAP-BE-20 | Missing DB indexes on read models | Small |
| GAP-BE-24 | Missing ID value objects | Small |
| GAP-INFRA-04-06 | Docker Compose cleanup | Small |
| GAP-OBS-02-03 | Custom metrics and structured logging | Medium |
| GAP-CI-01-05 | **FIXED** (2026-03-11) — NATS health, Trivy matrix, Keycloak CI, E2E mocks, frontend tests | **FIXED** |
| i18n | Seller/admin portals English-only | Large |

---

## Appendix: Files Referenced

### Backend Services
- `services/catalog-service/src/main/kotlin/.../application/service/LotService.kt`
- `services/auction-engine/src/main/kotlin/.../api/v1/resource/AuctionResource.kt`
- `services/payment-service/src/main/kotlin/.../api/v1/resource/PaymentResource.kt`
- `services/payment-service/src/main/kotlin/.../application/service/AuctionLotLookupService.kt`
- `services/seller-service/src/main/kotlin/.../infrastructure/nats/PaymentEventSellerConsumer.kt`
- `services/notification-service/src/main/kotlin/.../infrastructure/nats/PaymentEventNotificationConsumer.kt`
- `services/gateway-service/src/main/kotlin/.../infrastructure/proxy/ApiProxyResource.kt`

### Frontend
- `frontend/seller-portal/src/composables/useSettlements.ts`
- `frontend/admin-dashboard/src/composables/usePayments.ts`
- `frontend/admin-dashboard/src/composables/useSystemHealth.ts`
- `frontend/admin-dashboard/src/composables/useCompliance.ts`

### Infrastructure
- `docker/compose/infrastructure.yml`
- `docker/compose/payment-service.yml`
- `infrastructure/config/keycloak/auction-platform-realm.json`
- `shared/kotlin-commons/src/main/kotlin/.../messaging/NatsSubjects.kt`
- `.github/workflows/ci.yml`
