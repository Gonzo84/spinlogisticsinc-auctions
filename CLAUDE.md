# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
./gradlew detekt                             # Kotlin linting
./gradlew dependencyCheckAnalyze             # OWASP dependency security scan
```

### Frontend (Vue 3 / Nuxt 3 - npm)

Each frontend app is in `frontend/{buyer-web,seller-portal,admin-dashboard}`:

```bash
cd frontend/<app> && npm install && npm run dev    # Dev server with HMR
npm run build                                       # Production build
npm run lint                                        # ESLint
npm run typecheck                                   # TypeScript check (Nuxt only)
```

### Infrastructure (Docker Compose)

```bash
docker compose -f docker-compose.dev.yml up -d      # Start all infra services
docker compose -f docker-compose.dev.yml down -v     # Tear down with volumes
```

## Architecture Overview

**EU B2B online auction platform** built as 13 Kotlin/Quarkus microservices with 3 Vue frontends.

### Tech Stack

- **Backend:** Kotlin 2.3.0, Java 21, Quarkus 3.30.6
- **Frontend:** Vue 3 + Pinia + TailwindCSS. Buyer-web uses Nuxt 3 (SSR); seller-portal and admin-dashboard are Vite SPAs
- **Messaging:** NATS JetStream (event-driven communication between services)
- **Auth:** Keycloak (OIDC/JWT) + Casbin RBAC per service
- **Database:** PostgreSQL 16 (one database per service, Flyway migrations)
- **Cache:** Redis 7 (auction state, bid timers)
- **Search:** Elasticsearch 8
- **Object Storage:** MinIO (S3-compatible)
- **Observability:** OpenTelemetry + Prometheus + Grafana + JSON structured logging

### Microservices (ports in dev mode)

| Service | Port | Purpose |
|---------|------|---------|
| auction-engine | 8080 | Event-sourced bidding core (anti-sniping, proxy bidding, CQRS) |
| catalog-service | 8081 | Lot & category management |
| user-service | 8082 | User profiles, KYC |
| payment-service | 8083 | Adyen integration, PSD2 SCA, EU VAT |
| notification-service | 8084 | Email & push notifications |
| media-service | 8085 | Image uploads via MinIO |
| search-service | 8086 | Elasticsearch full-text search |
| seller-service | 8087 | Seller dashboard, settlements |
| broker-service | 8088 | Lead management, lot intake |
| compliance-service | 8089 | GDPR, AML, DSA, audit logs |
| co2-service | 8090 | Emission calculations |
| analytics-service | 8091 | Platform metrics, reporting |
| gateway-service | 8092 | API gateway, WebSocket hub, rate limiting |

### Service Code Organization (Hexagonal Architecture)

Each service follows the same package structure under `eu.auctionplatform.<service>`:

```
domain/          # Aggregates, events, value objects, domain exceptions
application/     # Use cases, command/query handlers, services
infrastructure/  # Persistence (JDBC, Flyway), messaging (NATS), config
api/             # JAX-RS REST resources, DTOs, request/response objects
```

### Shared Libraries (`shared/`)

- **kotlin-commons** — Base types (Money, AggregateRoot, DomainEvent, ValueObject), ID value objects, ApiResponse/PagedResponse wrappers, NATS utilities, error handling
- **nats-events** — Typed domain event classes used as message contracts between services

### Key Patterns

- **Event Sourcing + CQRS** in auction-engine: events stored in `auction_events` table, projected to read models. Optimistic concurrency via event stream versioning.
- **Outbox Pattern:** Domain events written to an outbox table transactionally, then published to NATS by a background poller. Guarantees at-least-once delivery.
- **API conventions:** All REST endpoints under `/api/v1/`, responses wrapped in `ApiResponse<T>`, errors follow RFC 7807 Problem Details. Pagination via `PagedResponse`.
- **Database per service:** Each microservice owns its PostgreSQL database and schema. Migrations in `src/main/resources/db/migration/`.
- **Casbin RBAC:** Each service has `casbin_model.conf` and `casbin_policy.csv` in resources. Role hierarchy: buyer, seller, broker, admin_ops, admin_super.

### Infrastructure Services (dev ports)

PostgreSQL: 5432 | NATS: 4222 | Keycloak: 8180 | Redis: 6379 | MinIO: 9000 | Elasticsearch: 9200 | Prometheus: 9090 | Grafana: 3333 | MailHog: 8025

### Keycloak Test Users

| Email | Password | Role |
|-------|----------|------|
| buyer@test.com | test | buyer_active |
| seller@test.com | test | seller_verified |
| broker@test.com | test | broker_active |
| admin@test.com | test | admin_ops |
| superadmin@test.com | test | admin_super |

### CI/CD

GitHub Actions pipeline (`.github/workflows/ci.yml`): lint → build → test → security scan → Docker image build (ghcr.io) → deploy staging/prod via Helm.

### Deployment

- Docker images use `eclipse-temurin:21-jre-alpine` with Quarkus fast-jar
- Helm chart in `helm/auction-platform/` with per-environment values files
- Kubernetes: HPA, PDB, NetworkPolicy, ServiceAccount templates included
