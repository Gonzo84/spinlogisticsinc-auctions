# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **Code Conventions:** See [CONVENTIONS.md](./CONVENTIONS.md) (~1400 lines) for mandatory code style, patterns, and constraints. All agents MUST read and follow that file when generating or modifying code. Covers Kotlin style, Vue SFC ordering, composable enforcement, API layer patterns, CSS/Tailwind, testing, Docker, and security.

## Build & Development Commands

### Backend (Kotlin/Quarkus - Gradle)

```bash
./gradlew build                              # Build all projects
./gradlew build -x test                      # Build without tests
./gradlew :services:<name>:build             # Build single service (e.g. :services:auction-engine:build)
./gradlew :services:<name>:quarkusDev        # Dev mode with hot reload for a service
./gradlew test                               # Run all unit tests
./gradlew :services:<name>:test              # Run tests for a single service
./gradlew integrationTest                    # Integration tests (requires Docker services)
./gradlew dependencyCheckAnalyze             # OWASP dependency security scan
./scripts/security-audit.sh                  # Run OWASP + npm audit for all frontends
./scripts/pre-commit.sh                      # Run detekt + ESLint on staged files
```

> **Note:** Detekt is currently disabled in `build.gradle.kts` because detekt 1.23.x is incompatible with Kotlin 2.3.0. Config is ready at `config/detekt/detekt.yml` — re-enable when `dev.detekt` 2.0.0 reaches stable.

### Frontend (Vue 3 / Nuxt 3 - npm)

Each frontend app is in `frontend/{buyer-web,seller-portal,admin-dashboard,broker-portal}`:

```bash
cd frontend/<app> && npm install && npm run dev    # Dev server with HMR
npm run build                                       # Production build
npm run lint                                        # ESLint
npm run typecheck                                   # TypeScript check (Nuxt only)
```

### Infrastructure (Docker Compose)

```bash
docker compose -f docker/compose/docker-compose-infrastructure.yaml --env-file docker/compose/.env up -d   # Start all infra services
docker compose -f docker/compose/docker-compose-infrastructure.yaml --env-file docker/compose/.env down -v  # Tear down with volumes
```

## Architecture Overview

**EU B2B online auction platform** built as 13 Kotlin/Quarkus microservices with 3 Vue frontends.

### Tech Stack

- **Backend:** Kotlin 2.3.0, Java 21, Quarkus 3.30.6
- **Frontend:** Vue 3 + Pinia + TailwindCSS + PrimeVue 4 (Aura theme). Buyer-web uses Nuxt 3 (SSR); seller-portal and admin-dashboard are Vite SPAs. Shared design tokens in `frontend/shared/design-tokens/` (preset.ts, pt.ts, status-severity.ts). PrimeVue auto-imported via `unplugin-vue-components` (Vite SPAs) and `@primevue/nuxt-module` (buyer-web).
- **Messaging:** NATS JetStream (event-driven communication between services)
- **Auth:** Keycloak (OIDC/JWT) + Casbin RBAC per service
- **Database:** PostgreSQL 16 (one database per service, Flyway migrations)
- **Cache:** Redis 7 (auction state, bid timers)
- **Search:** Elasticsearch 8
- **Object Storage:** MinIO (S3-compatible)
- **Logging:** JBoss Logger (all services), structured format with traceId/spanId: `%d{HH:mm:ss} %-5p traceId=%X{traceId} spanId=%X{spanId} [%c{2.}] %s%e%n`
- **Observability:** OpenTelemetry + Prometheus + Grafana + JSON structured logging

### Microservices (ports in Docker Compose)

| Service | Port | Purpose |
|---------|------|---------|
| gateway-service | 8080 | API gateway, WebSocket hub (`/ws/auctions/{auctionId}` + `/ws/user`), rate limiting |
| auction-engine | 8081 | Event-sourced bidding core (anti-sniping, proxy bidding, CQRS) |
| catalog-service | 8082 | Lot & category management |
| user-service | 8083 | User profiles, KYC |
| payment-service | 8084 | Adyen integration, PSD2 SCA, EU VAT |
| notification-service | 8085 | Email & push notifications |
| media-service | 8086 | Image uploads via MinIO |
| search-service | 8087 | Elasticsearch full-text search |
| seller-service | 8088 | Seller dashboard, settlements |
| broker-service | 8089 | Lead management, lot intake |
| analytics-service | 8090 | Platform metrics, reporting |
| compliance-service | 8091 | GDPR, AML, DSA, audit logs |
| co2-service | 8092 | Emission calculations |

### Service Code Organization (Hexagonal Architecture)

Each service follows the same package structure under `eu.auctionplatform.<service>`:

```
domain/          # Aggregates, events, value objects, domain exceptions
application/     # Use cases, command/query handlers, services
infrastructure/  # Persistence (JDBC, Flyway), messaging (NATS), config
api/             # JAX-RS REST resources, DTOs, request/response objects
```

### Shared Libraries (`shared/`)

- **kotlin-commons** — Base types (Money, AggregateRoot, DomainEvent, ValueObject), ID value objects, ApiResponse/PagedResponse wrappers, NATS utilities (NatsPublisher with traceId headers, NatsConsumer with exponential backoff), OutboxPoller abstract class, error handling
- **nats-events** — Typed domain event classes used as message contracts between services

### Key Patterns

- **Event Sourcing + CQRS** in auction-engine: events stored in `auction_events` table, projected to read models. Optimistic concurrency via event stream versioning.
- **Outbox Pattern:** Domain events written to an outbox table transactionally, then published to NATS by a background poller (`OutboxPoller` abstract class in kotlin-commons with `SELECT...FOR UPDATE SKIP LOCKED`). Guarantees at-least-once delivery.
- **TraceId Propagation:** `NatsPublisher` injects `trace-id` and `user-id` headers from `DomainEvent.metadata`. `NatsConsumer.extractTraceContext()` extracts them on the receiving side.
- **API conventions:** All REST endpoints under `/api/v1/`, responses wrapped in `ApiResponse<T>`, errors follow RFC 7807 Problem Details. Pagination via `PagedResponse`.
- **Database per service:** Each microservice owns its PostgreSQL database and schema. Migrations in `src/main/resources/db/migration/`.
- **Casbin RBAC:** Each service has `casbin_model.conf` and `casbin_policy.csv` in resources. Policies use keyMatch2 with `:param` patterns (not glob `*`/`**`). Role hierarchy: buyer, seller, broker, admin_ops, admin_super.
- **Frontend Type Safety:** All 3 frontends enforce `no-explicit-any` via ESLint. Types are centralized in `src/types/` with barrel exports. All composable/store returns use `readonly()`.
- **Pinia Setup Stores:** buyer-web uses Composition API (Setup) stores with `ref()`, `computed()`, and plain functions — not Options API.
- **Post-auction flow endpoints:** `POST /auctions/{id}/award` (admin, awards closed auction), `GET /auctions/by-lot/{lotId}` (public, lookup by lot), `GET /payments/{id}` (buyer/seller/admin, single payment detail), `PATCH /payments/{id}/settle` (admin, settles completed/processing payment), `GET /payments/summary` (admin, KPI aggregation).
- **Bean Validation error responses:** `ConstraintViolationExceptionMapper` in `shared/kotlin-commons` returns 400 with field-level error details (field, message, rejectedValue) instead of empty body.
- **Inter-service HTTP lookups:** `AuctionLotLookupService` in payment-service calls auction-engine and catalog-service REST APIs to resolve hammer price, seller ID, and lot title for checkout. Config: `service.auction-engine.url`, `service.catalog-service.url`.

### Infrastructure Services (dev ports)

PostgreSQL: 5432 | NATS: 4222 | Keycloak: 8180 | Redis: 6379 | MinIO: 9000 | Elasticsearch: 9200 | Prometheus: 9090 | Grafana: 3333 | MailHog: 8025

### Keycloak Test Users

| Email | Password | Role |
|-------|----------|------|
| buyer@test.com | test | buyer_active |
| buyer2@test.com | test | buyer_active |
| seller@test.com | test | seller_verified |
| broker@test.com | test | broker_active |
| admin@test.com | test | admin_ops |
| superadmin@test.com | test | admin_super |

### CI/CD

GitHub Actions pipeline (`.github/workflows/ci.yml`): lint → build → test → security scan → Docker image build (ghcr.io) → deploy staging/prod via Helm.

### Testing

Test infrastructure is set up for key services:

```bash
# Backend (Quarkus JUnit 5 + Testcontainers)
./gradlew :services:auction-engine:test      # Domain tests (AuctionDomainTest)
./gradlew :services:payment-service:test     # VAT calculation tests
# TestFixtures.kt available in auction-engine, catalog, payment, user services

# Frontend (Vitest)
cd frontend/buyer-web && npx vitest          # vitest.config.ts configured
cd frontend/seller-portal && npx vitest
cd frontend/admin-dashboard && npx vitest
```

### Critical Gotchas

1. **Auction-engine has separate list vs detail DTOs.** `AuctionSummaryResponse` (used by list endpoint) has fewer fields than `AuctionDetailResponse`. When adding a field to the detail DTO, always check if the summary DTO also needs it. The `toSummaryResponse()` mapping in `AuctionResource.kt` must also be updated.

2. **CSS layer order for PrimeVue + Tailwind is non-negotiable.** Must be `tailwind-base, primevue, tailwind-utilities` in both `preset.ts` and each frontend's CSS entry point. Wrong order causes PrimeVue styles to be overridden by Tailwind reset or vice versa.

3. **`preset.ts` component overrides need a type assertion.** PrimeVue's Aura preset types don't include all component keys (e.g., `datepicker`, `accordion`). Cast the components object with `as Record<string, Record<string, Record<string, string>>>` in `definePreset()`.

4. **Gateway proxy must support every HTTP method used by services.** `ApiProxyResource.kt` has separate handler methods per HTTP verb. When adding a new endpoint that uses an uncommon method (e.g., PATCH), you must add both a `@PATCH` handler method AND a case in the internal `proxy()` switch. Missing this causes 405 Method Not Allowed.

5. **Outbox poller `markPublished()` requires `setLong()` for bigint ID columns.** The outbox tables use `bigint` primary keys. Using `setString()` causes `ERROR: operator does not exist: bigint = character varying`, making outbox entries re-publish every poll cycle. Always use `setLong(idx, id.toLong())`.

6. **Auction read model must store winnerId/winningBid on close.** The `AuctionReadModelRepository` projection for `AuctionClosedEvent` must write `current_high_bid` and `current_high_bidder_id` columns. Without this, downstream services (payment-service) see null winner after auction closes.

7. **Payment-service inter-service lookups need explicit URL config.** `AuctionLotLookupService` requires `service.auction-engine.url` and `service.catalog-service.url` in `application.yml`, plus `AUCTION_ENGINE_URL` and `CATALOG_SERVICE_URL` env vars in Docker Compose.

8. **Checkout has two modes: explicit items vs lotIds lookup.** `CheckoutRequest` accepts either `items` (with pre-resolved auctionId/hammerPrice/sellerId from the award response) or `lotIds` (triggering HTTP lookups to auction-engine and catalog-service). Always prefer `items` mode — the `lotIds` lookup is fragile without service-to-service auth tokens.

9. **Keycloak `KC_SPI_IMPORT_REALM_FILE_STRATEGY: OVERWRITE` does NOT actually reimport.** Despite the env var being set, Keycloak logs `Strategy: IGNORE_EXISTING` and skips reimport. New users or clients added to `auction-platform-realm.json` must be created via the Keycloak admin API (`POST /admin/realms/{realm}/users`, `POST /admin/realms/{realm}/clients`). Role assignments also need a separate API call (`POST /users/{id}/role-mappings/realm`).

10. **New REST endpoints need Casbin policy for every accessing role.** Adding a new endpoint (e.g., `GET /payments/{id}`) requires policy entries in `casbin_policy.csv` for each role that should access it (`buyer_active`, `seller_verified`, `admin_ops`). The `admin_super` wildcard covers admins, but other roles need explicit entries or the endpoint returns 403. Easy to miss when the endpoint works in unit tests (no Casbin) but fails through the gateway.

11. **NATS durable consumers with changed config require deletion before restart.** If `NatsConsumer` backoff/batch/config changes, the existing durable consumer in NATS JetStream rejects recreation with `[SUB-90016] Existing consumer cannot be modified`. Fix: `docker run --rm --network auction-platform-network natsio/nats-box:0.14.5 nats consumer rm <STREAM> <durable-name> -s nats://nats:4222 --force`, then restart the service.

12. **Payment-service OIDC client needs separate `OIDC_AUTH_URL` env var in Docker Compose.** The standard `QUARKUS_OIDC_AUTH_SERVER_URL` only overrides `quarkus.oidc.auth-server-url`, NOT `quarkus.oidc-client.internal.auth-server-url`. The `oidc-client` section uses `${OIDC_AUTH_URL}` — must be set explicitly or the service hangs on startup trying to reach `localhost:8180`. The Keycloak client `payment-service-internal` must also exist in the realm.

13. **Seller-service settlement fields differ from frontend expectations.** Backend returns `commission` (not `commissionAmount`) and no `commissionRate`. Frontend `useSettlements.ts` must normalize: map `commission` → `commissionAmount` and compute `commissionRate` from `commission / hammerPrice`. Without this, the settlements page shows "NaN %".

14. **PrimeIcons font decode warnings in Vite dev server.** PrimeIcons fonts in `node_modules` produce "invalid sfntVersion" warnings when Vite pre-bundles them. Fix: add `optimizeDeps: { exclude: ['primeicons'] }` to each frontend's `vite.config.ts`.

15. **Seed data creates duplicates on repeated runs.** Running the seed script multiple times creates duplicate lots. Any frontend lot list (homepage featured, search results) should deduplicate by title or ID. See `buyer-web/pages/index.vue` `fetchLotsFromCatalog()` for the pattern.

### Deployment

- Docker images use `eclipse-temurin:21-jre-alpine` with Quarkus fast-jar (multi-stage build: `docker/Dockerfile.service-full`)
- Production nginx config with strict CSP: `infrastructure/config/nginx/nginx-spa-prod.conf`
- Helm chart in `helm/auction-platform/` with per-environment values files
- Kubernetes: HPA, PDB, NetworkPolicy, ServiceAccount templates included
