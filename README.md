# Tradex - EU B2B Online Auction Platform

A full-stack B2B auction platform for the European market, built with Kotlin/Quarkus microservices and Vue/Nuxt frontends. Designed for industrial equipment, machinery, and commercial asset auctions with full EU regulatory compliance (GDPR, PSD2, DSA, VAT).

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend Apps                            │
│  ┌─────────────┐  ┌────────────────┐  ┌───────────────────┐    │
│  │  Buyer Web   │  │ Seller Portal  │  │ Admin Dashboard   │    │
│  │  (Nuxt 3)    │  │  (Vue 3 SPA)   │  │   (Vue 3 SPA)     │    │
│  └──────┬───────┘  └───────┬────────┘  └────────┬──────────┘    │
└─────────┼──────────────────┼───────────────────┼────────────────┘
          │                  │                   │
┌─────────▼──────────────────▼───────────────────▼────────────────┐
│                     Gateway Service                              │
│         (Rate Limiting, CORS, WebSocket Hub, Webhooks)           │
└─────────┬───────────────────────────────────────────────────────┘
          │
┌─────────▼───────────────────────────────────────────────────────┐
│                     Backend Microservices                         │
│                                                                  │
│  ┌────────────────┐  ┌──────────────┐  ┌───────────────────┐    │
│  │ Auction Engine  │  │   Catalog    │  │   User Service    │    │
│  │ (Event Sourced) │  │   Service    │  │  (Keycloak OIDC)  │    │
│  └────────────────┘  └──────────────┘  └───────────────────┘    │
│  ┌────────────────┐  ┌──────────────┐  ┌───────────────────┐    │
│  │    Payment     │  │ Notification │  │   Media Service   │    │
│  │    Service     │  │   Service    │  │   (MinIO S3)      │    │
│  └────────────────┘  └──────────────┘  └───────────────────┘    │
│  ┌────────────────┐  ┌──────────────┐  ┌───────────────────┐    │
│  │    Seller      │  │    Broker    │  │   Compliance      │    │
│  │    Service     │  │   Service    │  │   Service         │    │
│  └────────────────┘  └──────────────┘  └───────────────────┘    │
│  ┌────────────────┐  ┌──────────────┐  ┌───────────────────┐    │
│  │  CO2 Service   │  │  Analytics   │  │  Search Service   │    │
│  │ (Sustainability)│  │   Service    │  │ (Elasticsearch)   │    │
│  └────────────────┘  └──────────────┘  └───────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
          │
┌─────────▼───────────────────────────────────────────────────────┐
│                       Infrastructure                             │
│  PostgreSQL │ NATS JetStream │ Keycloak │ Redis │ MinIO │ ES    │
└─────────────────────────────────────────────────────────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Kotlin 2.3.0, Quarkus 3.30.6, Java 21 |
| Frontend (Buyer) | Nuxt 3 (SSR), Pinia, TailwindCSS |
| Frontend (Seller/Admin) | Vue 3, Vite, Pinia, TailwindCSS |
| Database | PostgreSQL 16, Flyway migrations |
| Messaging | NATS JetStream (event streaming) |
| Auth | Keycloak 24 (OIDC), Casbin (RBAC) |
| Search | Elasticsearch 8 (multi-language) |
| Storage | MinIO (S3-compatible) |
| Cache | Redis 7 |
| Observability | Prometheus, Grafana, OpenTelemetry |
| CI/CD | GitHub Actions (7-stage pipeline) |
| Deployment | Helm charts, Kubernetes |
| Container | Docker, multi-stage builds |

## Project Structure

```
eu-auction-platform/
├── services/                    # Backend microservices
│   ├── auction-engine/          # Event-sourced bidding engine
│   ├── catalog-service/         # Lot & category management
│   ├── user-service/            # User profiles & KYC
│   ├── payment-service/         # Adyen, PSD2 SCA, EU VAT
│   ├── notification-service/    # Email & push via NATS
│   ├── media-service/           # Image uploads via MinIO
│   ├── search-service/          # Elasticsearch full-text
│   ├── seller-service/          # Seller dashboard & settlements
│   ├── broker-service/          # Lead mgmt & lot intake
│   ├── compliance-service/      # GDPR, AML, DSA, audit
│   ├── co2-service/             # Emission calculations
│   ├── analytics-service/       # Platform metrics & reporting
│   └── gateway-service/         # API gateway & WebSocket hub
├── shared/
│   ├── kotlin-commons/          # Shared types, Money, events, security
│   └── nats-events/             # Typed event classes
├── frontend/
│   ├── buyer-web/               # Nuxt 3 SSR buyer application
│   ├── seller-portal/           # Vue 3 SPA seller dashboard
│   └── admin-dashboard/         # Vue 3 SPA admin panel
├── infrastructure/
│   ├── docker/                  # Dockerfiles & nginx config
│   └── config/                  # Keycloak, Grafana, Prometheus, NATS, OTel
├── docker/
│   └── compose/                 # Modular Docker Compose files (one per service)
├── helm/
│   └── auction-platform/        # Helm chart with multi-env values
├── .github/
│   └── workflows/ci.yml         # 7-stage CI/CD pipeline
├── docker-compose.dev.yml       # Infrastructure-only (dev)
├── docker-compose-full.yaml     # Full stack: infra + all 13 backend services
├── start-platform.sh            # One-command: build + start everything
├── build.gradle.kts             # Root Gradle build
├── settings.gradle.kts          # Multi-project includes
└── gradle.properties            # Quarkus/Kotlin versions
```

## Prerequisites

- **Java 21** (Eclipse Temurin recommended)
- **Node.js 22** (for frontends)
- **Docker & Docker Compose** (for infrastructure services)
- **Gradle** (wrapper included, no global install needed)

## Getting Started

### Quick Start — Full Stack (One Command)

Build and start **all infrastructure + all 13 backend services** in Docker:

```bash
./start-platform.sh
```

This builds all JARs with Gradle, then starts 24 Docker containers (11 infrastructure + 13 backend services). Once running, all backends are accessible on their respective ports.

### Alternative: Step-by-Step Setup

#### 1. Start Infrastructure Only

```bash
docker compose -f docker-compose.dev.yml up -d
```

This starts:

| Service | URL / Port |
|---------|-----------|
| PostgreSQL | `localhost:5432` (user: `dev`, pass: `dev`) |
| NATS | `localhost:4222` (monitoring: `localhost:8222`) |
| Keycloak | `http://localhost:8180` (admin/admin) |
| Redis | `localhost:6379` |
| MinIO | `http://localhost:9001` (minioadmin/minioadmin) |
| Elasticsearch | `http://localhost:9200` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3333` (admin/admin) |
| MailHog | `http://localhost:8025` (SMTP: `1025`) |
| OTel Collector | `localhost:4317` (gRPC), `localhost:4318` (HTTP) |

#### 2. Start Full Stack (Infra + All Backends)

```bash
# Build all services first
./gradlew quarkusBuild -x test --parallel

# Start everything in Docker
docker compose -f docker-compose-full.yaml up -d --build
```

#### 3. Run a Single Backend Service (Dev Mode)

```bash
# Run with Quarkus dev mode (hot reload)
./gradlew :services:auction-engine:quarkusDev
```

### Backend Service Ports

| Service | Port | Purpose |
|---------|------|---------|
| gateway-service | 8080 | API gateway, WebSocket hub, rate limiting |
| auction-engine | 8081 | Event-sourced bidding core |
| catalog-service | 8082 | Lot & category management |
| user-service | 8083 | User profiles, KYC |
| payment-service | 8084 | Adyen integration, PSD2 SCA |
| notification-service | 8085 | Email & push notifications |
| media-service | 8086 | Image uploads via MinIO |
| search-service | 8087 | Elasticsearch full-text search |
| seller-service | 8088 | Seller dashboard, settlements |
| broker-service | 8089 | Lead management, lot intake |
| analytics-service | 8090 | Platform metrics, reporting |
| compliance-service | 8091 | GDPR, AML, DSA, audit logs |
| co2-service | 8092 | Emission calculations |

### Frontend Apps

**Buyer Web (Nuxt 3 SSR):**
```bash
cd frontend/buyer-web
npm install
npm run dev        # http://localhost:3000
```

**Seller Portal (Vue 3 SPA):**
```bash
cd frontend/seller-portal
npm install
npm run dev        # http://localhost:5174
```

**Admin Dashboard (Vue 3 SPA):**
```bash
cd frontend/admin-dashboard
npm install
npm run dev        # http://localhost:5175
```

## Available Commands

### Gradle (Backend)

```bash
# Build
./gradlew build                          # Build all projects
./gradlew build -x test                  # Build without tests
./gradlew :services:<name>:build         # Build a specific service

# Dev Mode
./gradlew :services:<name>:quarkusDev    # Run service with hot reload

# Tests
./gradlew test                           # Run unit tests
./gradlew integrationTest                # Run integration tests
./gradlew :services:<name>:test          # Test a specific service

# Code Quality
./gradlew detekt                         # Run Kotlin linter

# Security
./gradlew dependencyCheckAnalyze         # OWASP dependency check

# Clean
./gradlew clean                          # Clean build outputs
```

### npm (Frontends)

All three frontends support:

```bash
npm install          # Install dependencies
npm run dev          # Start dev server with hot reload
npm run build        # Production build
npm run preview      # Preview production build
npm run lint         # Run ESLint
```

Buyer web additionally supports:
```bash
npm run generate     # Static site generation
npm run typecheck    # TypeScript type checking
```

### Docker

```bash
# --- Full Stack (infrastructure + all 13 backends) ---
./start-platform.sh                                         # One-command build + start
docker compose -f docker-compose-full.yaml up -d --build    # Start (builds images)
docker compose -f docker-compose-full.yaml down             # Stop all
docker compose -f docker-compose-full.yaml down -v          # Stop + wipe data
docker compose -f docker-compose-full.yaml logs -f          # Follow all logs

# --- Infrastructure Only ---
docker compose -f docker-compose.dev.yml up -d              # Start infra
docker compose -f docker-compose.dev.yml down               # Stop infra

# --- Individual Services (modular compose files) ---
docker compose -f docker/compose/infrastructure.yml up -d   # Start infra only
docker compose -f docker/compose/auction-engine.yml up -d   # Start single service

# --- Logs ---
docker compose -f docker-compose-full.yaml logs -f auction-engine
docker compose -f docker-compose-full.yaml logs -f gateway-service
```

### Modular Docker Compose Structure

Each service has its own compose file in `docker/compose/`, combined via `docker-compose-full.yaml`:

```
docker/compose/
├── infrastructure.yml          # PostgreSQL, NATS, Keycloak, Redis, MinIO, ES, etc.
├── auction-engine.yml
├── catalog-service.yml
├── user-service.yml
├── payment-service.yml
├── notification-service.yml
├── media-service.yml
├── search-service.yml
├── seller-service.yml
├── broker-service.yml
├── compliance-service.yml
├── co2-service.yml
├── analytics-service.yml
└── gateway-service.yml
```

### Helm (Kubernetes Deployment)

```bash
# Install/upgrade to staging
helm upgrade --install auction-platform ./helm/auction-platform \
  --namespace auction-staging \
  --values ./helm/auction-platform/values-staging.yaml

# Install/upgrade to production
helm upgrade --install auction-platform ./helm/auction-platform \
  --namespace auction-prod \
  --values ./helm/auction-platform/values-prod.yaml

# Dry run
helm install auction-platform ./helm/auction-platform --dry-run --debug
```

## Key Features

### Auction Engine
- **Event Sourcing** with PostgreSQL event store
- **Anti-Sniping** - 2-minute auto-extension on bids within 2 minutes of closing
- **Auto-Bid (Proxy Bidding)** - eBay-style automatic incremental bidding with second-price logic
- **CQRS** - Separate command/query paths with read model projections

### EU Compliance
- **GDPR** - Data export and erasure request workflows
- **PSD2 SCA** - Strong Customer Authentication for payments
- **DSA** - Digital Services Act content reporting
- **AML** - Anti-Money Laundering screening
- **EU VAT** - Reverse charge, margin scheme, OSS support

### Sustainability
- **CO2 Tracking** - Emission avoidance calculations per lot
- **20 Equipment Categories** - Pre-seeded emission factors
- **Seller Reports** - CO2 savings, equivalent trees planted, car-km avoided

### Security
- **Keycloak OIDC** with PKCE for all frontends
- **Casbin RBAC** with role hierarchy (buyer, seller, broker, admin_ops, admin_super)
- **Webhook HMAC validation** for Adyen and Onfido
- **Rate limiting** at the gateway layer

## Test Users (Keycloak)

The Keycloak realm is pre-configured with test users:

| User | Password | Role |
|------|----------|------|
| buyer@test.com | test | buyer_active |
| seller@test.com | test | seller_verified |
| broker@test.com | test | broker_active |
| admin@test.com | test | admin_ops |
| superadmin@test.com | test | admin_super |

## Observability

- **Grafana Dashboards** - Platform overview, auction engine, payment pipeline (`http://localhost:3333`)
- **Prometheus Metrics** - All services expose Micrometer metrics (`http://localhost:9090`)
- **OpenTelemetry** - Distributed tracing across services
- **JSON Logging** - Structured logs for all services
- **Health Checks** - Liveness and readiness probes on every service (`/q/health`)

## CI/CD Pipeline

The GitHub Actions pipeline runs in 7 stages:

1. **Lint** - Detekt (Kotlin) + ESLint (Frontend)
2. **Build** - Gradle build + npm build
3. **Test** - Unit, integration, and frontend tests
4. **Security** - OWASP dependency check
5. **Docker** - Build and push container images to GHCR
6. **Deploy Staging** - Helm deploy on `develop` branch
7. **Deploy Production** - Helm deploy on `main` / `release/*` branches

## Changelog

### 2026-02-24 — Bug Audit & Fixes (20 bugs fixed)

**Critical fixes:**
- Fixed NATS event type mismatches that silently broke inter-service messaging for auction close, extension, and proxy bid events
- Fixed settlement service using `auctionId` as `sellerId` — added `sellerId` to Payment model with DB migration
- Fixed notification service never marking failed notifications as FAILED (stuck PENDING forever)
- Fixed GDPR erasure requests marked COMPLETED before downstream services process the deletion
- Fixed CO2 emission factor update overwriting `categoryId` with random UUID
- Fixed payment completion lacking error isolation — invoice and settlement failures are now independent

**High-priority fixes:**
- Fixed `co2_lot_seller_mapping` table missing `lot_id` column (new migration)
- Fixed `Deposit.isActive()` returning true during pending refund
- Fixed catalog lot pagination returning page size as total count for filtered queries
- Fixed missing `await` on async auto-bid cancellation in buyer frontend
- Fixed duplicate Adyen webhook processing (added payment status guard)

**Medium-priority fixes:**
- Added NOT NULL constraint on outbox `created_at` (new migration)
- Added NOT NULL constraint on media `original_url` (new migration)
- Fixed AML screening sort order (`NULLS LAST` instead of `NULLS FIRST`)
- Fixed content report sort inconsistency (both queries now use `DESC`)
- Fixed seller analytics using `Instant.now()` fallback for missing timestamps
- Fixed broker lot intake silently accepting invalid lead IDs
- Fixed buyer profile form not populating when user data loads asynchronously
- Fixed search pagination ellipsis placement logic
- Added 4 missing NATS subject constants

See [BUGS.md](../docs/BUGS.md) for the full audit report with file paths and detailed descriptions.

## License

Proprietary - All rights reserved.
