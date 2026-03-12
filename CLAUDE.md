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
- **Media-service presigned upload flow:** `POST /media/upload/presigned` (lotId optional) returns `{imageId, uploadUrl, publicUrl, expiresIn}`. Client PUTs file directly to MinIO via presigned URL. `POST /media/images/associate` moves temp images to lot-specific path. MinIO buckets `auction-media` and `auction-thumbnails` are auto-created with anonymous download policy in `infrastructure.yml`.

### Infrastructure Services (dev ports)

PostgreSQL: 5432 | NATS: 4222 | Keycloak: 8180 | Redis: 6379 | MinIO: 9000 | Elasticsearch: 9200 | Prometheus: 9090 | Grafana: 3333 | MailHog: 8025

### Keycloak Test Users

| Email | Password | Role | UUID |
|-------|----------|------|------|
| buyer@test.com | password123 | buyer_active | ...0001 |
| buyer2@test.com | password123 | buyer_active | ...0007 |
| seller@test.com | password123 | seller_verified | ...0002 |
| broker@test.com | password123 | broker / broker_active | ...0003 |
| admin@test.com | password123 | admin_super | ...0004 |
| superadmin@test.com | password123 | admin_super | ...0005 |
| blocked@test.com | password123 | buyer_blocked | ...0006 |

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

9. **Keycloak realm import uses explicit `kc.sh import --override true` before `start-dev`.** The `--import-realm` startup flag always skips existing realms (by design in Keycloak 24). The old `KC_SPI_IMPORT_REALM_FILE_STRATEGY: OVERWRITE` env var had no effect on it. The fix is a two-phase command in Docker Compose: `kc.sh import --override true` runs first to overwrite the realm, then `kc.sh start-dev --import-realm` handles first-boot (when no DB schema exists yet). The realm JSON at `infrastructure/config/keycloak/auction-platform-realm.json` is the single source of truth for all users, clients, and roles. **Note:** `--override true` replaces the entire realm on every restart, so any users created via the Keycloak admin UI during dev will be lost on container restart.

10. **New REST endpoints need Casbin policy for every accessing role.** Adding a new endpoint (e.g., `GET /payments/{id}`) requires policy entries in `casbin_policy.csv` for each role that should access it (`buyer_active`, `seller_verified`, `admin_ops`). The `admin_super` wildcard covers admins, but other roles need explicit entries or the endpoint returns 403. Easy to miss when the endpoint works in unit tests (no Casbin) but fails through the gateway.

11. **NATS durable consumers with changed config require deletion before restart.** If `NatsConsumer` backoff/batch/config changes, the existing durable consumer in NATS JetStream rejects recreation with `[SUB-90016] Existing consumer cannot be modified`. Fix: `docker run --rm --network auction-platform-network natsio/nats-box:0.14.5 nats consumer rm <STREAM> <durable-name> -s nats://nats:4222 --force`, then restart the service.

12. **Payment-service OIDC client needs separate `OIDC_AUTH_URL` env var in Docker Compose.** The standard `QUARKUS_OIDC_AUTH_SERVER_URL` only overrides `quarkus.oidc.auth-server-url`, NOT `quarkus.oidc-client.internal.auth-server-url`. The `oidc-client` section uses `${OIDC_AUTH_URL}` — must be set explicitly or the service hangs on startup trying to reach `localhost:8180`. The Keycloak client `payment-service-internal` must also exist in the realm.

13. **Seller-service settlement fields differ from frontend expectations.** Backend returns `commission` (not `commissionAmount`) and no `commissionRate`. Frontend `useSettlements.ts` must normalize: map `commission` → `commissionAmount` and compute `commissionRate` from `commission / hammerPrice`. Without this, the settlements page shows "NaN %".

14. **PrimeIcons font decode warnings in Vite dev server.** PrimeIcons fonts in `node_modules` produce "invalid sfntVersion" warnings when Vite pre-bundles them. Fix: add `optimizeDeps: { exclude: ['primeicons'] }` to each frontend's `vite.config.ts`.

15. **Seed data creates duplicates on repeated runs.** Running the seed script multiple times creates duplicate lots. Any frontend lot list (homepage featured, search results) should deduplicate by title or ID. See `buyer-web/pages/index.vue` `fetchLotsFromCatalog()` for the pattern.

16. **Image upload requires temp path + public URLs for presigned flow.** Media-service `PresignedUrlService` supports `lotId=null` for temp uploads before lot creation. Objects stored at `uploads/temp/{imageId}.{ext}`, moved to `uploads/{lotId}/{imageId}.{ext}` on association. The S3 presigner must use `publicEndpoint` (not internal Docker endpoint) with `pathStyleAccessEnabled(true)` — otherwise presigned URLs use virtual-hosted style (`bucket.host:port`) which doesn't resolve from the browser. Config: `MINIO_PUBLIC_ENDPOINT` env var in `media-service.yml`.

17. **Catalog-service `lot_images.imageUrl` must be populated at creation time.** When creating lots with images, the frontend must send `images: [{id, url}]` (not just `imageIds: [id]`) in `CreateLotRequest`. The catalog-service stores the URL directly — there's no async event that fills it later. Without the URL, `imageUrl` is stored as empty string and images appear broken on the lot detail page.

18. **Backend image field `imageUrl` must be mapped to frontend `url` in normalizeLot().** The catalog API returns `imageUrl` for image objects but the `LotImage` TypeScript type and Vue templates use `url`. The seller-portal `normalizeLot()` in `useLots.ts` must map `imageUrl → url` and `displayOrder → sortOrder`. Buyer-web handles this in `auction-mapper.ts`.

19. **Keycloak user IDs must be valid UUIDs (max 36 chars).** The `USER_ENTITY.ID` column is `varchar(36)`. Using descriptive string IDs like `service-account-payment-service-internal` (42 chars) causes `kc.sh import --override true` to fail with `ERROR: value too long for type character varying(36)`. Always use proper UUID format for all users including service accounts.

20. **Keycloak Docker image entrypoint is `kc.sh` — cannot use `bash -c` in `command`.** The `quay.io/keycloak/keycloak` image sets `ENTRYPOINT` to `/opt/keycloak/bin/kc.sh`. Passing `command: bash -c '...'` sends `bash` as an argument to `kc.sh`, causing `Unknown option: 'bash'`. To run shell commands before `start-dev`, override `entrypoint: /bin/bash` and use `command: ["-c", "script"]`.

21. **`CREATE INDEX CONCURRENTLY` deadlocks inside Flyway migrations.** Flyway wraps each migration in a transaction. `CONCURRENTLY` cannot run inside a transaction — it silently blocks forever waiting for an exclusive lock, causing the service to hang after Flyway output with no error message. The `-- flyway:postgresql:executeInTransaction=false` comment syntax does NOT work. Fix: use `CREATE INDEX IF NOT EXISTS` (without `CONCURRENTLY`) in Flyway migrations. For production, run `CONCURRENTLY` indexes manually outside Flyway.

22. **Quarkus 3.x OTel env var is `QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT`, not the old 2.x name.** The legacy `QUARKUS_OPENTELEMETRY_TRACER_EXPORTER_OTLP_ENDPOINT` is silently ignored in Quarkus 3.x, falling back to `localhost:4317`. All service compose files must use the new name. Using the wrong name may cause startup hangs or telemetry loss when the OTel collector is unreachable.

23. **Auction-engine requires `QUARKUS_REDIS_HOSTS` env var in Docker Compose.** The `quarkus-redis-client` dependency on the classpath causes Quarkus to eagerly connect to Redis at startup. Without `QUARKUS_REDIS_HOSTS=redis://redis:6379`, it defaults to `localhost:6379` which is unreachable inside the container, blocking startup indefinitely with no error log.

24. **Bean Validation requires nullable fields for proper 400 error bodies.** When Jackson can't deserialize a required non-null Kotlin field (missing from JSON), it throws a deserialization exception *before* Bean Validation runs. The `ConstraintViolationExceptionMapper` never fires, resulting in an empty 400 body with no field-level details. Fix: make required DTO fields nullable with `= null` defaults, add `@NotNull`/`@NotBlank` annotations, and use `!!` in the resource method after `@Valid` passes. See `CreateAuctionRequest` for the pattern.

25. **PrimeVue DataTable Column slots must not use nested `<template>` tags.** Using `<template v-if>` / `<template v-else>` inside a `<template #body>` slot causes Vue runtime `TypeError: Cannot read properties of null`. Use `<div>`/`<span>` with `v-if`/`v-else` instead.

26. **Keycloak `post.logout.redirect.uris` separator is `##`, not space.** Multiple redirect URIs in Keycloak client config must be delimited with `##`. Spaces between URIs cause the second URI to be silently ignored, breaking post-logout redirects.

27. **`NatsConsumer.maxDeliver` must exceed `backoff` array length.** NATS JetStream rejects consumer creation if `maxDeliver <= backoff.length` with error `[SUB-90016]` or `[10116]`. The consumer thread crashes silently on startup — no events are ever forwarded. Fix: `effectiveMaxDeliver = max(maxRedeliveries, backoff.length) + 1`. This was the root cause of WebSocket bidding never working (`BidEventForwarder` crashed on startup).

28. **Gateway `BidEventForwarder` must read `bidAmount`/`bidCurrency` from domain events, not `amount`/`currency`.** The `BidPlacedEvent` domain event uses `bidAmount: BigDecimal` and `bidCurrency: String`. The gateway's JSON extraction must use `node.path("bidAmount")` with fallback to `node.path("amount")`. Without this, WebSocket `bid_placed` messages have `amount: null`.

29. **Frontend `useWebSocket.ts` must read `message.type` (not `message.event`) and normalize gateway event names.** The gateway sends `{type: "bid_placed"}` but the original frontend code read `message.event`. Gateway also sends `lot_extended`/`lot_closed` but the frontend expects `auction_extended`/`auction_closed`. The handler data must be `message.data ?? message`, and `auctionId` must be injected from the top-level message into the data object.

30. **`subscribeToAuction()` must pass `auctionId` to `ws.connect()`, and the lot detail page must subscribe with the auction ID (not the lot ID).** The WebSocket URL is `/ws/auctions/{auctionId}` — using the catalog lot UUID instead of the auction-engine auction UUID results in a connected but useless WebSocket (wrong room). The `[id].vue` page must use `lot.value.id` (which is the mapped `auctionId` from `mapAuctionResponse`) via a watcher, not `route.params.id`.

### Deployment

- Docker images use `eclipse-temurin:21-jre-alpine` with Quarkus fast-jar (multi-stage build: `docker/Dockerfile.service-full`)
- Production nginx config with strict CSP: `infrastructure/config/nginx/nginx-spa-prod.conf`
- Helm chart in `helm/auction-platform/` with per-environment values files
- Kubernetes: HPA, PDB, NetworkPolicy, ServiceAccount templates included
