# Code Conventions & Architectural Standards

**Stack**: Vue 3 + TypeScript | Kotlin 2.3 + Quarkus 3 | REST (JAX-RS) | PostgreSQL 16 | Keycloak | NATS JetStream | MinIO | Redis | Elasticsearch

> This document is the single source of truth for coding style, patterns, and architectural decisions across all services in this platform. AI agents MUST follow these conventions when generating or modifying code. Maintained by the project owner -- updates here override any conflicting defaults.

---

## Table of Contents

1. [Universal Principles](#1-universal-principles)
2. [Naming Conventions (Cross-Service)](#2-naming-conventions-cross-service)
3. [Vue 3 + TypeScript Frontend](#3-vue-3--typescript-frontend)
4. [Kotlin + Quarkus 3 Backend](#4-kotlin--quarkus-3-backend)
5. [REST API Design](#5-rest-api-design)
6. [Error Contract](#6-error-contract)
7. [Database Conventions](#7-database-conventions)
8. [Authentication & Authorization](#8-authentication--authorization)
9. [Observability](#9-observability)
10. [Event-Driven Patterns (NATS JetStream)](#10-event-driven-patterns-nats-jetstream)
11. [Testing Strategy](#11-testing-strategy)
12. [Docker & Infrastructure](#12-docker--infrastructure)
13. [Git & CI/CD](#13-git--cicd)
14. [Security](#14-security)
15. [Documentation Standards](#15-documentation-standards)
16. [Critical Gotchas](#16-critical-gotchas)
17. [What NOT To Do](#17-what-not-to-do)

---

## 1. Universal Principles

These apply to every line of code in every service.

### 1.1 Simplicity Over Cleverness

Write the simplest code that solves the problem. Three similar lines are better than a premature abstraction. Do not design for hypothetical future requirements.

### 1.2 Explicit Over Implicit

Prefer explicit types, explicit error handling, and explicit dependencies. Avoid magic (reflection, global state, auto-wiring) unless the framework requires it.

### 1.3 Fail Fast, Fail Loud

Validate inputs at system boundaries. Reject invalid state early with clear error messages. Never swallow errors silently.

### 1.4 Dependencies Flow Inward

```
API Layer (JAX-RS Resources) -> Application Services -> Domain Layer
                                                            ^
                                                            |
                                                   (no framework imports)
```

Domain code has zero framework dependencies. Infrastructure adapters implement domain interfaces. API resources are thin delegation layers.

### 1.5 One Responsibility Per Unit

Each file, class, function, or module does one thing well. If a function needs a comment explaining what it does, consider splitting it.

### 1.6 12-Factor Compliance

| Factor | Rule |
|--------|------|
| Config | Environment variables via `application.yml` substitution, never hardcoded |
| Dependencies | Explicitly declared (`package.json`, `build.gradle.kts`) |
| Backing services | Treated as attached resources (PostgreSQL, MinIO, NATS, Keycloak, Redis, Elasticsearch) |
| Processes | Stateless; all state in PostgreSQL/Redis/MinIO/NATS |
| Logs | JSON to stdout, collected externally |
| Port binding | Each service binds its own port |

### 1.7 General Formatting

- **Indent:** 2 spaces for Kotlin, TypeScript, Vue, JSON, YAML, CSS, HTML. 4 spaces for everything else.
- **Line endings:** LF everywhere.
- **Max line length:** 120 characters (Kotlin), no hard limit for frontend but keep readable.
- **Final newline:** Always.
- **No trailing whitespace** (except Markdown).

---

## 2. Naming Conventions (Cross-Service)

Consistent naming across the entire stack reduces cognitive load when context-switching between services.

### 2.1 Language-Specific Identifier Style

| Context | Vue/TypeScript | Kotlin | SQL | JSON |
|---------|---------------|--------|-----|------|
| Variables/fields | `camelCase` | `camelCase` | `snake_case` | `camelCase` |
| Functions/methods | `camelCase` | `camelCase` | n/a | n/a |
| Types/classes | `PascalCase` | `PascalCase` | n/a | n/a |
| Constants | `UPPER_SNAKE` (env-derived) / `camelCase` | `UPPER_SNAKE` (`const val`) / `camelCase` (`val`) | n/a | n/a |
| Files | `PascalCase.vue` / `camelCase.ts` | `PascalCase.kt` | `snake_case.sql` | `camelCase.json` |
| Packages/dirs | `kebab-case` | `lowercase` | n/a | n/a |
| CSS classes | Tailwind utility-first | n/a | n/a | n/a |
| Env variables | `VITE_UPPER_SNAKE` | `UPPER_SNAKE` | n/a | n/a |

### 2.2 Acronyms

- **TypeScript/Kotlin**: First letter uppercase only for 3+ letter acronyms: `HttpClient`, `XmlParser`, `ApiError`. Two-letter acronyms are all-caps: `ID`, `IO`.

### 2.3 Domain Term Glossary

Use these terms consistently across all services:

| Concept | Canonical Term | NOT |
|---------|---------------|-----|
| Unique identifier | `id` or `{entity}Id` | `identifier`, `key`, `pk` |
| Creation timestamp | `createdAt` | `created`, `createDate`, `timestamp` |
| Update timestamp | `updatedAt` | `modified`, `lastModified` |
| Authenticated user | `userId` / `subjectId` | `user`, `principal`, `sub` |
| Auction lot | `lot` | `item`, `product`, `listing` |
| Bid amount | `amount` | `price`, `value`, `bidPrice` |
| Currency | `currency` (ISO 4217) | `curr`, `currencyCode` |
| File content type | `mimeType` or `contentType` | `type`, `fileType` |
| File size | `sizeBytes` | `size`, `length`, `fileSize` |
| Location city | `locationCity` (backend flat) / `location.city` (frontend nested) | `city` alone |

---

## 3. Vue 3 + TypeScript Frontend

### 3.1 Architecture Layer Enforcement

```
services/  (useApi.ts, keycloak setup)     -- Low-level, no Vue reactivity
    |
stores/    (Pinia: auth.ts, ui.ts)         -- Global state, devtools-visible
    |
composables/ (useAuth, useLots, useBids)   -- Reactive proxy layer
    |
views/layouts/ (*.vue)                     -- UI components
```

**Rule**: Views and layouts SHOULD access backend data through composables. Composables wrap API calls and provide reactive state (loading, error, data). Direct store access from views is acceptable for simple state reads, but all API interaction goes through composables.

### 3.2 Project Layout

This platform has three frontend applications with different frameworks:

| App | Framework | Component Order | Auth Method |
|-----|-----------|----------------|-------------|
| `buyer-web` | Nuxt 3 (SSR) | `<template>` first, then `<script setup>` | `useNuxtApp().$keycloak` plugin |
| `seller-portal` | Vite SPA | `<script setup>` first, then `<template>` | `inject('keycloak')` in `setup()` |
| `admin-dashboard` | Vite SPA | `<script setup>` first, then `<template>` | `inject('keycloak')` in `setup()` |

Do NOT mix component ordering conventions between apps.

### 3.3 Component Conventions

#### Script Setup Only

Every component uses `<script setup lang="ts">`. No Options API. No `defineComponent()`.

#### SFC Section Order

**Vite SPAs (seller-portal, admin-dashboard):**

```vue
<template>
  <!-- template -->
</template>

<script setup lang="ts">
// 1. Imports (Vue core -> third-party -> internal @/ paths)
// 2. Props & emits (defineProps, defineEmits, defineModel)
// 3. Composables (useAuth, useApi, useRouter)
// 4. Reactive state (ref, reactive, computed)
// 5. Functions
// 6. Lifecycle hooks (onMounted, onUnmounted)
// 7. defineExpose (if needed)
</script>

<style scoped>
/* styles */
</style>
```

**Nuxt (buyer-web):**

```vue
<template>
  <!-- template -->
</template>

<script setup lang="ts">
// Same internal ordering as above
import type { Lot } from '~/types/auction'

const props = defineProps<{ lot: Lot }>()
</script>
```

#### Component Naming

| Category | Pattern | Example |
|----------|---------|---------|
| View (page) | `{Feature}View.vue` | `DashboardView.vue`, `LotDetailView.vue` |
| Layout | `{Name}Layout.vue` | `AppLayout.vue`, `PublicLayout.vue` |
| Single-instance | `The{Name}.vue` | `TheHeader.vue`, `TheSidebar.vue` |
| Base/shared | `Base{Name}.vue` | `BaseButton.vue`, `BaseInput.vue` |
| Feature-specific | `{Feature}{Role}.vue` | `BidPanel.vue`, `LotCard.vue`, `SidebarNav.vue` |

All component names are multi-word (Vue style guide Priority B).

#### Template Rules

```vue
<!-- v-for: always provide unique :key -->
<div v-for="lot in lots" :key="lot.id">

<!-- Events: use handle* prefix -->
<button @click="handleDelete(lot.id)" />

<!-- v-html: ONLY with sanitized content, always disable ESLint rule explicitly -->
<!-- eslint-disable-next-line vue/no-v-html -->
<span v-html="sanitizedContent" />
```

### 3.4 TypeScript Conventions

#### Strict Mode (Non-Negotiable)

```json
{
  "compilerOptions": {
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true
  }
}
```

#### `any` Type is STRICTLY FORBIDDEN

Use proper interfaces/types for everything. The ONLY exception is raw API response data before it is normalized (e.g., `raw` from `get<any>('/endpoint')`) -- and even then, normalize into a typed object immediately.

#### Types vs Interfaces

```typescript
// interface: for object shapes (props, API responses, domain models)
interface Lot {
  id: string
  title: string
  status: LotStatus
}

// type: for unions, intersections, aliases, utility types
type LotStatus = 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'ACTIVE' | 'SOLD'
type Nullable<T> = T | null
```

**No enums.** Use string literal union types instead: `type Severity = 'success' | 'info' | 'warn' | 'error'`.

#### Props and Emits

```vue
<script setup lang="ts">
// Type-based props with withDefaults
const props = withDefaults(defineProps<{
  title: string
  count?: number
}>(), {
  count: 0,
})

// Type-based emits with tuple syntax (Vue 3.3+)
const emit = defineEmits<{
  submit: [data: FormData]
  cancel: []
}>()
</script>
```

#### Centralized Type Definitions

- **buyer-web:** Types in `types/` directory (e.g., `types/auction.ts`)
- **seller-portal / admin-dashboard:** Types co-located in the composable file, or in `src/types/` if shared across composables

### 3.5 State Management (Pinia)

#### Setup Stores Only

```typescript
export const useFooStore = defineStore('foo', () => {
  // -- State --
  const items = ref<Foo[]>([])
  const current = ref<Foo | null>(null)

  // -- Getters --
  const itemCount = computed(() => items.value.length)

  // -- Actions --
  function setItems(newItems: Foo[]) {
    items.value = newItems
  }

  function clear() {
    items.value = []
    current.value = null
  }

  return {
    items: readonly(items),
    current: readonly(current),
    itemCount,
    setItems,
    clear,
  }
})
```

**Rules:**
- Use `ref()` for state, `computed()` for getters, plain functions for actions.
- Return only what should be publicly accessible.
- Type refs explicitly: `ref<Foo[]>([])`, `ref<Foo | null>(null)`.
- **State MUST NOT be mutated directly from components.** Expose state as `readonly()` and provide explicit action functions for mutations.

#### When Store vs Composable

| Need | Use |
|------|-----|
| Global shared state (auth, theme, locale) | Pinia store |
| Component-instance-scoped state (form, loading) | Composable |
| Shared business logic without global state | Composable |
| State that needs devtools debugging | Pinia store |

### 3.6 Composables

```typescript
export function useFoo() {
  const loading = ref(false)
  const error = ref<string | null>(null)
  const items = ref<Foo[]>([])

  async function fetchItems(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const raw = await get<any>('/foos')
      items.value = (raw?.data?.items ?? []).map(normalizeFoo)
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to load'
    } finally {
      loading.value = false
    }
  }

  return {
    loading: readonly(loading),
    error: readonly(error),
    items,
    fetchItems,
  }
}
```

**Rules:**
- Always prefix with `use`: `useAuth`, `useLots`, `useBids`.
- Return a plain object of refs and functions (not a reactive wrapper).
- Standardize on the `loading` / `error` / `data` triple for async operations.
- Expose `loading` and `error` as `readonly()`.
- Clean up in `onUnmounted` if the composable sets up event listeners or timers.

### 3.7 API Layer

**Vite SPAs** use an axios-based `useApi()` composable:

```typescript
export function useApi() {
  const keycloak = inject<Keycloak>('keycloak')  // MUST be in setup()
  // ... axios instance with interceptors for token refresh + 401 handling
  return { get, post, put, patch, del, loading, error }
}
```

**Key rules:**
- `inject('keycloak')` MUST be captured during `setup()` -- never inside async callbacks or interceptors.
- Token refresh: `keycloak.updateToken(30)` called before every request; if refresh fails, redirect to login.
- 401 response triggers `keycloak.login()` redirect.

**Nuxt (buyer-web)** uses `$fetch` with `useNuxtApp().$keycloak` plugin.

#### API Response Unwrapping

All backends return `ApiResponse<T>` wrapper `{ data: T }`. Frontend MUST unwrap:

```typescript
// Paginated: raw.data.items
const response = raw?.data && typeof raw.data === 'object' ? raw.data : raw
const items = response.items ?? []

// Single item: raw.data
const item = raw?.data ?? raw
```

#### Backend Field Normalization

Catalog-service uses flat fields (`locationCity`, `locationCountry`). Frontends use nested `location` object. Always transform:

```typescript
function normalizeLot(data: any): Lot {
  return {
    ...data,
    location: data.location ?? {
      address: data.locationAddress ?? '',
      city: data.locationCity ?? '',
      country: data.locationCountry ?? '',
    },
  }
}

function toBackendPayload(data: LotFormData) {
  return {
    locationCity: data.location?.city ?? '',
    locationCountry: data.location?.country ?? '',
    // ... flat fields for backend
  }
}
```

### 3.8 Router

**buyer-web:** Nuxt file-based routing in `pages/`.

**seller-portal / admin-dashboard:** Vue Router in `src/router/index.ts`.

```typescript
// Lazy-load ALL route components
component: () => import('@/views/DashboardView.vue')

// Type-safe route meta
declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    requiresAuth?: boolean
    roles?: string[]
  }
}

// Guard uses to.path (NOT to.fullPath) to avoid Keycloak redirect_uri overflow
keycloak.login({ redirectUri: window.location.origin + to.path })
```

### 3.9 i18n

**buyer-web** uses i18n with translation files in `i18n/locales/`.

- Dot-separated, feature-scoped keys: `auction.timeRemaining`, `common.save`.
- Named interpolation: `t('auction.errors.bidFailed', { reason: err.message })`.
- Never hardcode user-facing strings in templates or TypeScript.
- Use `t()` in `<script>`, `{{ t('key') }}` in templates.

### 3.10 CSS & Styling (Tailwind)

This project uses **Tailwind CSS** (utility-first), not BEM.

**Design tokens:**
- `primary` -- blue (#1e40af)
- `secondary` -- green (#059669)
- `accent` -- amber (#d97706)
- `warning` -- red (#dc2626)
- Font: Inter (body), JetBrains Mono (code)

**Reusable classes** defined in `@layer components`:
```css
.btn-primary { @apply bg-primary-600 text-white hover:bg-primary-700 ...; }
.card        { @apply rounded-xl border border-gray-200 bg-white p-6 shadow-sm; }
.input       { @apply block w-full rounded-lg border border-gray-300 ...; }
.badge       { @apply inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium; }
```

**Rules:**
- **Scoped styles** on every component: `<style scoped>`.
- Use Tailwind utility classes in templates; extract to `@layer components` only when reused 3+ times.
- **Deep selectors**: `:deep(.some-child)` only when styling third-party component internals.
- **Responsive**: Mobile-first breakpoints (`sm:`, `md:`, `lg:`).
- No CSS preprocessor: Plain CSS with Tailwind directives.

### 3.11 Linting & Formatting

```
Prettier: no semicolons, single quotes, 100-char width, trailing commas
ESLint:   flat config, Vue recommended + TS recommended
```

---

## 4. Kotlin + Quarkus 3 Backend

### 4.1 Package Structure (Hexagonal Architecture)

Every service follows this layout under `eu.auctionplatform.<service>`:

```
api/
  dto/              # *Request, *Response data classes, DtoMappers.kt
  v1/resource/      # JAX-RS @Path resources
application/
  service/          # @ApplicationScoped use-case orchestration
domain/
  model/            # Immutable data classes, enums, value objects
  event/            # Domain events (event-sourced services only)
  exception/        # Sealed exception hierarchies extending DomainException
  command/          # Command objects (CQRS services only)
infrastructure/
  persistence/
    entity/         # JPA @Entity classes with toDomain() / fromDomain()
    repository/     # PanacheRepositoryBase<Entity, UUID>
  messaging/        # NATS publishers/consumers
  config/           # Service-specific config classes
```

Do NOT deviate from this structure. New classes go in the correct package.

### 4.2 Kotlin Style

- **Prefer `val` over `var`** -- declare everything as `val` unless mutation is genuinely needed. Entities are an exception (Hibernate requires mutable properties).
- **Expression bodies** for single-expression functions: `fun status(): String = "OK"`. Block bodies for side effects or multiple statements.
- **Data classes** for DTOs and value objects. Default values for optional fields: `= null`, `= emptyList()`. Data classes are NEVER CDI beans.
- **Enums** with trailing comma after last entry. Add `companion object { fun from(value: String) }` for string-to-enum lookup.
- **Nullable handling**: `user?.name ?: "Unknown"`, `value.takeIf { it.isNotBlank() }`.
- **Scope functions**: Use `use {}` for auto-closing resources. Use `let`, `also`, `apply` sparingly and consistently.

#### Class Layout Order

1. Property declarations and `init` blocks
2. Secondary constructors
3. Methods
4. `companion object`
5. Nested/inner classes

### 4.3 CDI / Dependency Injection

#### Constructor Injection (Preferred for Services and Repositories)

```kotlin
@ApplicationScoped
class LotService(
    private val lotRepository: LotRepository,
    private val eventPublisher: EventPublisher,
)
```

When there is exactly one constructor, `@Inject` is not required.

#### Field Injection (Required for JAX-RS Resources)

```kotlin
@Path("/api/v1/lots")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class LotResource {
    @Inject lateinit var lotService: LotService
    @Inject lateinit var securityContext: SecurityContext
}
```

Use `@Inject lateinit var` in JAX-RS resources because Quarkus RESTEasy requires a no-arg constructor.

#### CDI Producers for Complex Initialization

```kotlin
@ApplicationScoped
class NatsConnectionProducer {
    @Produces @ApplicationScoped
    fun natsConnection(): Connection =
        Nats.connect(options)
}
```

#### Lifecycle Hooks

- `@Observes StartupEvent` / `@Observes ShutdownEvent` for application-scoped init/cleanup.
- `@PostConstruct` for bean-scoped initialization.
- `@PreDestroy` for resource cleanup.

### 4.4 Naming (Backend-Specific)

| Element | Pattern | Example |
|---------|---------|---------|
| Request DTOs | `*Request` suffix | `CreateLotRequest` |
| Response DTOs | `*Response` suffix | `LotResponse` |
| Entities | `*Entity` suffix | `LotEntity` |
| Repositories | `*Repository` suffix | `LotRepository` |
| Resources | `*Resource` suffix | `LotResource` |
| Services | `*Service` suffix | `LotService` |
| Error codes | SCREAMING_SNAKE_CASE strings | `"LOT_NOT_FOUND"` |

See section 2.1 for cross-stack naming rules. DB naming in section 7.1.

### 4.5 REST Resources

```kotlin
@Path("/api/v1/<plural-noun>")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class FooResource {

    @Inject
    lateinit var fooService: FooService

    companion object {
        private val LOG: Logger = Logger.getLogger(FooResource::class.java)
    }

    @GET
    @Path("/{id}")
    @RolesAllowed("admin_ops", "admin_super")
    fun getById(@PathParam("id") id: UUID): Response {
        val item = fooService.getById(id)
        return Response.ok(ApiResponse.ok(item.toResponse())).build()
    }
}
```

**Rules:**
- All paths under `/api/v1/`.
- Responses wrapped in `ApiResponse.ok(data)` -- never return raw objects.
- Paginated responses use `PagedResponse<T>` inside `ApiResponse`.
- Use `@RolesAllowed` or `@PermitAll` on every endpoint -- roles: `buyer_active`, `seller_verified`, `broker_active`, `admin_ops`, `admin_super`.
- Bearer token extraction via `@HeaderParam("Authorization")` + extension functions from `SecurityContext.kt`.
- Return `Response.ok(...)` for 200, `Response.status(CREATED).entity(...)` for 201, `Response.noContent()` for 204.
- Group endpoints with `// --- Section Name ---` comments.

### 4.6 DTOs

- **Request DTOs**: optional fields are nullable with `= null` default. Required fields have no default.
- **Response DTOs**: all fields required (no defaults).
- Use `@JsonAlias` when frontend field names differ from backend.
- DTO mappers are extension functions in `DtoMappers.kt`: `fun Foo.toResponse(): FooResponse = FooResponse(...)`.

### 4.7 Application Services

- `@ApplicationScoped` on all services.
- Constructor injection for dependencies (not field injection).
- `@Transactional` on mutating methods only (not reads).
- Services return domain models, never entities or DTOs.
- Throw structured exceptions with `code` + `message`.

### 4.8 Repositories

- Extend `PanacheRepositoryBase<Entity, UUID>`.
- Queries use Panache HQL with camelCase field names (not SQL column names).
- Finder methods return `Entity?` (nullable).
- Search/list methods return `Pair<List<Entity>, Long>` for items + total count.
- Pagination is 0-based internally.

### 4.9 Entities

- Mutable classes (required by Quarkus/Hibernate).
- Always include `toDomain()` instance method and `fromDomain()` companion factory.
- Enums stored as `EnumType.STRING`.
- Timestamps use `Instant`.
- UUIDs as primary keys with `gen_random_uuid()` default in DB.

### 4.10 IDs

- Use `IdGenerator.generateUUIDv7()` for all new IDs.
- Typed value objects in auction-engine: `AuctionId`, `BidId`, `LotId`, `UserId`.
- Plain `UUID` in other services.

### 4.11 Logging

Logger in companion object named `LOG`:

```kotlin
companion object {
    private val LOG: Logger = Logger.getLogger(MyClass::class.java)
}
```

- Use JBoss `org.jboss.logging.Logger` (NOT SLF4J, NOT java.util.logging).
- Use `LOG.infof(...)`, `LOG.warnf(...)`, `LOG.errorf(...)` with format strings.
- Include context: `LOG.infof("Lot approved: lotId=%s approvedBy=%s", lotId, userId)`.

### 4.12 Configuration

- Profile-based overrides in `application.yml` via `"%dev":`, `"%prod":` prefixes.
- Environment variable substitution: `${ENV_VAR:default_value}`.
- Use `@ConfigProperty(name = "...", defaultValue = "...")` for injection.
- Duration notation: `5s`, `24h`, `60m`, `14d`.

### 4.13 Build

Gradle Kotlin DSL with `kotlin("jvm")`, `kotlin("plugin.allopen")`, `id("io.quarkus")`. The `allOpen` plugin opens classes annotated with `@Path`, `@ApplicationScoped`, `@Entity`, `@QuarkusTest`.

---

## 5. REST API Design

> **Note:** This project uses REST (JAX-RS) exclusively. gRPC is not currently used but could be added for inter-service communication in the future if needed.

### 5.1 URL Structure

```
GET    /api/v1/lots              # List
POST   /api/v1/lots              # Create
GET    /api/v1/lots/{id}         # Get one
PUT    /api/v1/lots/{id}         # Full replace
PATCH  /api/v1/lots/{id}         # Partial update
DELETE /api/v1/lots/{id}         # Delete
```

- Plural nouns for collections: `/lots`, `/auctions`, `/users`.
- Kebab-case for multi-word paths: `/lot-categories`.
- `camelCase` for JSON property names.
- Nest sub-resources only one level deep: `/auctions/{id}/lots`.
- Action endpoints use verbs: `/lots/{id}/approve`, `/auctions/{id}/close`.

### 5.2 HTTP Status Codes

| Scenario | Code |
|----------|------|
| Success with body | `200 OK` |
| Created | `201 Created` (include `Location` header) |
| No content | `204 No Content` (DELETE, empty PUT/PATCH) |
| Bad request | `400 Bad Request` (validation failures) |
| Unauthorized | `401 Unauthorized` (missing/expired token) |
| Forbidden | `403 Forbidden` (valid token, insufficient permissions) |
| Not found | `404 Not Found` |
| Conflict | `409 Conflict` (duplicate creation, state conflict) |
| Unprocessable entity | `422 Unprocessable Entity` (invalid input data) |
| Internal error | `500 Internal Server Error` |

### 5.3 Pagination

```
GET /api/v1/lots?page=0&size=25&sort=createdAt,desc
```

Response wrapped in `ApiResponse<PagedResponse<T>>`:

```json
{
  "data": {
    "items": [...],
    "totalItems": 150,
    "page": 0,
    "size": 25,
    "totalPages": 6
  }
}
```

Default `size`: 25. Maximum `size`: 100. Pagination is **0-based** internally (page 0 = first page).

### 5.4 Filtering

```
GET /api/v1/lots?status=APPROVED&category=machinery&sellerId=...
```

Use query parameters for simple filters. For complex filtering, accept a JSON body in POST to a `/search` endpoint.

---

## 6. Error Contract

### 6.1 Exception Hierarchy

Exception hierarchy in `shared/kotlin-commons`:

| Exception | HTTP | Use for |
|-----------|------|---------|
| `NotFoundException` | 404 | Resource not found |
| `ConflictException` | 409 | Duplicate / state conflict |
| `ValidationException` | 422 | Invalid input (carries field error map) |
| `ForbiddenException` | 403 | Insufficient permissions |

### 6.2 REST Errors: RFC 9457 Problem Details

Every REST error response follows RFC 9457:

```json
{
  "status": 404,
  "title": "Not Found",
  "code": "LOT_NOT_FOUND",
  "message": "Lot '550e8400-...' does not exist.",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
}
```

### 6.3 Domain-Specific Errors

For domain-specific errors, use sealed classes:

```kotlin
sealed class AuctionException(code: String, message: String)
    : DomainException(code, message) {
    class AuctionNotFound(id: UUID) : AuctionException("AUCTION_NOT_FOUND", "Auction '$id' not found")
    class BidBelowMinimum(amount: BigDecimal, min: BigDecimal)
        : AuctionException("BID_BELOW_MINIMUM", "Bid $amount is below minimum $min")
    class AuctionClosed(id: UUID) : AuctionException("AUCTION_CLOSED", "Auction '$id' is closed")
}
```

### 6.4 Error Code Table

| Category | REST Status | Error Code Pattern |
|----------|-------------|-------------------|
| Validation | 400/422 | `INVALID_*`, `MISSING_*` |
| Auth missing | 401 | `AUTH_*` |
| Auth insufficient | 403 | `PERMISSION_*`, `FORBIDDEN_*` |
| Not found | 404 | `*_NOT_FOUND` |
| Conflict | 409 | `*_ALREADY_EXISTS`, `*_CONFLICT` |
| Internal | 500 | `INTERNAL_*` |

**Rules:**
- Never leak stack traces in production responses.
- Always include a human-readable `message`.
- Include `traceId` for correlation with distributed tracing.
- Check permissions BEFORE resource existence to avoid information leakage (403 before 404).

---

## 7. Database Conventions

### 7.1 Naming

| Object | Convention | Example |
|--------|-----------|---------|
| Schema | Always `app` | Never use `public` |
| Table | singular, `snake_case` | `lot`, `auction_event`, `bid` |
| Column | `snake_case` | `created_at`, `seller_id`, `location_country` |
| Primary key | `{table}_pkey` | `lot_pkey` |
| Foreign key | `fk_{from}_{to}_{col}` | `fk_bid_auction_auction_id` |
| Index | `idx_{table}_{columns}` | `idx_lot_seller_id_created_at` |
| Unique constraint | `uq_{table}_{columns}` | `uq_user_email` |

### 7.2 Standard Columns

Every table includes:

```sql
id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
```

### 7.3 Column Type Conventions

| Data | Type | Notes |
|------|------|-------|
| UUID primary keys | `UUID` | Default `gen_random_uuid()` |
| Timestamps | `TIMESTAMPTZ` | Always timezone-aware |
| Money/amounts | `NUMERIC(19,4)` | Never use `FLOAT` or `DOUBLE` |
| Status columns | `VARCHAR(20) NOT NULL DEFAULT 'DRAFT'` | Stored as enum strings |
| Country codes | `VARCHAR(3)` | ISO alpha-2/3 codes ("NL", "DE"), NOT full names |
| Currency codes | `VARCHAR(3)` | ISO 4217 ("EUR", "USD") |

### 7.4 Migration Naming (Flyway)

File naming: `V001__create_lots.sql`, `V002__add_column_to_lots.sql`

**Rules:**
- All tables in `app` schema.
- Never modify a migration after it has been applied to any environment.
- Use `IF NOT EXISTS` / `IF EXISTS` guards for defensive safety.
- Migrations must be backward-compatible: add columns before removing old ones.
- Always parameterized queries in application code (`?` placeholders, never string concatenation).

### 7.5 Database Layout

Each microservice owns its own PostgreSQL database and schema:

| Service | Database | Schema |
|---------|----------|--------|
| All services | `{service-name}` | `app` |

Migrations in `src/main/resources/db/migration/`. Auto-migration on startup: `quarkus.flyway.migrate-at-start=true`.

---

## 8. Authentication & Authorization

### 8.1 Auth Flow

```
Browser -> Keycloak (Authorization Code + PKCE S256)
  -> SPA gets JWT (access_token + refresh_token)
  -> Fetch to Gateway (Bearer token in Authorization header)
  -> Gateway proxies to backend services
  -> Each service validates JWT via Quarkus OIDC
  -> Casbin RBAC enforces fine-grained permissions
```

### 8.2 Auth Rules

| Rule | Implementation |
|------|---------------|
| SPA: Authorization Code + PKCE only | `keycloak-js` with `pkceMethod: 'S256'` |
| SPA: Never persist tokens in localStorage | Keycloak manages session via SSO cookies |
| SPA: Refresh before expiry | `keycloak.updateToken(30)` refreshes if < 30s remaining |
| Backend: Validate JWT per request | Quarkus OIDC extension with JWKS validation |
| Backend: Role extraction | `quarkus.oidc.roles.role-claim-path=realm_access/roles` |
| Backend: Principal is UUID | `quarkus.oidc.token.principal-claim=sub` |
| Backend: `@RolesAllowed` on every endpoint | Roles: `buyer_active`, `seller_verified`, `broker_active`, `admin_ops`, `admin_super` |

### 8.3 OIDC Configuration (CRITICAL)

All services MUST include in `application.yml`:

```yaml
quarkus:
  oidc:
    auth-server-url: ${OIDC_SERVER_URL:http://localhost:8180/realms/auction-platform}
    client-id: ${OIDC_CLIENT_ID:backend-service}
    roles:
      role-claim-path: realm_access/roles
    token:
      principal-claim: sub
```

Without `role-claim-path` and `principal-claim`, `@RolesAllowed` fails and user ID comes as email instead of UUID.

### 8.4 Test Users (Keycloak `auction-platform` realm)

| Email | Password | Role | UUID suffix |
|-------|----------|------|-------------|
| buyer@test.com | password123 | buyer_active | ...0001 |
| seller@test.com | password123 | seller_verified | ...0002 |
| broker@test.com | password123 | broker_active | ...0003 |
| admin@test.com | password123 | admin_ops | ...0004 |
| superadmin@test.com | password123 | admin_super | (auto) |

### 8.5 Casbin RBAC Policy Model

Each service has `casbin_model.conf` and `casbin_policy.csv` in resources:

```
[request_definition]
r = sub, obj, act

[policy_definition]
p = sub, obj, act, eft

[role_definition]
g = _, _

[policy_effect]
e = some(where (p.eft == allow)) && !some(where (p.eft == deny))

[matchers]
m = g(r.sub, p.sub) && keyMatch2(r.obj, p.obj) && regexMatch(r.act, p.act)
```

Role hierarchy: `buyer_active` < `seller_verified` < `broker_active` < `admin_ops` < `admin_super`.

### 8.6 Auto-Registration Pattern

User-service and seller-service auto-create profiles on first `/me` access from JWT claims (email, given_name, family_name). New services with `/me` endpoints MUST follow this pattern using `getOrCreateUser()`.

---

## 9. Observability

### 9.1 Structured Logging Format

All services output JSON logs in production with these mandatory fields:

```json
{
  "timestamp": "2026-02-27T10:30:00.000Z",
  "level": "INFO",
  "loggerName": "eu.auctionplatform.catalog.api.v1.resource.LotResource",
  "message": "Lot approved: lotId=550e8400 approvedBy=admin",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7"
}
```

| Service | Logger | Format |
|---------|--------|--------|
| Vue SPAs | `console` (dev) | Browser console |
| Quarkus services | JBoss Logger + OpenTelemetry | JSON in prod, pattern in dev |

### 9.2 Logging Conventions

- Use JBoss `Logger` in companion object named `LOG` (see section 4.11).
- Use format-string methods: `LOG.infof("message: param=%s", value)`.
- Log levels: `debug` for internal flow, `info` for business events, `warn` for recoverable issues, `error` for failures.
- Never log sensitive data (passwords, tokens, PII).

### 9.3 Distributed Tracing (OpenTelemetry)

- W3C Trace Context for propagation.
- Quarkus: `quarkus-opentelemetry` extension.
- Console format includes trace context: `traceId=%X{traceId}, spanId=%X{spanId}`.

### 9.4 Metrics (Prometheus)

- Quarkus: `quarkus-micrometer-registry-prometheus` at `/q/metrics`.
- RED metrics: Rate, Errors, Duration per endpoint.
- Grafana dashboards at port 3333.

### 9.5 Health Checks

- Quarkus: `quarkus-smallrye-health` at `/q/health/ready` and `/q/health/live`.
- Every Docker Compose service MUST have a health check.
- Use `depends_on` with `condition: service_healthy` for startup ordering.

---

## 10. Event-Driven Patterns (NATS JetStream)

### 10.1 Subject Naming

Dot-notation, domain-scoped subjects:

```
auction.bid.placed
auction.lot.awarded
auction.closed
catalog.lot.created
catalog.lot.status_changed
payment.checkout.completed
user.registered
user.kyc.verified
media.image.uploaded
compliance.gdpr.erasure
```

### 10.2 Stream Configuration

Eight JetStream streams, one per domain, initialized by `NatsStreamInitializer`:

| Stream | Subject Pattern | Purpose |
|--------|----------------|---------|
| `AUCTION` | `auction.>` | Bids, awards, closings, extensions |
| `CATALOG` | `catalog.>` | Lot lifecycle events |
| `PAYMENT` | `payment.>` | Checkout, settlements, deposits |
| `USER` | `user.>` | Registration, KYC |
| `MEDIA` | `media.>` | Image uploads, processing |
| `NOTIFY` | `notify.>` | Notification triggers |
| `COMPLIANCE` | `compliance.>` | GDPR, AML events |
| `CO2` | `co2.>` | Emission calculations |

Default stream config:
- Retention: `Limits`
- Storage: `File`
- Replicas: 1
- Max age: 7 days

### 10.3 Event Schema

All domain events extend `BaseEvent` from `shared/nats-events` with fields: `eventId` (UUIDv7), `eventType` (dot-notation, e.g. `"auction.bid.placed"`), `aggregateId`, `aggregateType`, `timestamp` (Instant), `version` (monotonic), `metadata` (optional trace context). Jackson polymorphic deserialization via `@JsonTypeInfo` on `eventType` field.

### 10.4 Event Rules

- Use explicit ACK mode (never auto-ack in production).
- New fields with defaults; never remove or rename fields.
- Consumers MUST ignore unknown fields (`@JsonIgnoreProperties(ignoreUnknown = true)`).
- Publishers write events to an outbox table transactionally, then publish via a background poller (outbox pattern).
- Use `NatsConsumer` base class with `waitForStream()` retry logic.

### 10.5 Event Sourcing (auction-engine only)

The auction-engine service uses event sourcing + CQRS:
- Events stored in `auction_events` table, projected to read models.
- Optimistic concurrency via event stream versioning.
- Manual `EVENT_TYPE_REGISTRY` in `AuctionEventEntity` for Jackson deserialization.

---

## 11. Testing Strategy

### 11.1 Testing Pyramid

```
         +-----------+
         |    E2E    |  Very few: critical user paths
        -+-----------+-
        | Integration |  Per-service: real DB, real NATS
       -+-------------+-
      |   Unit Tests    |  Broad base: business logic, utilities
     -+-----------------+-
```

### 11.2 Per-Service Testing

| Layer | Framework | What to test |
|-------|-----------|-------------|
| Kotlin unit | JUnit 5 + Mockito | Domain logic, services, mappers |
| Kotlin integration | `@QuarkusTest` + RestAssured + Testcontainers | REST endpoints, DB queries, event publishing |
| Vue unit | Vitest + Vue Test Utils | Components, composables, stores |
| Vue E2E | Playwright (Chromium) | Critical user paths per frontend app |

### 11.3 Kotlin Testing Conventions

- `@QuarkusTest` + `@InjectMock` for resource tests.
- Backtick-delimited test names: `` fun `should return 404 when lot not found`() ``
- RestAssured for HTTP assertions: `given().when().get("/api/v1/lots/$id").then().statusCode(200)`.
- Testcontainers with `QuarkusTestResourceLifecycleManager` for PostgreSQL -- never H2.

### 11.4 Vue Testing Conventions

- Mock Keycloak with `vi.hoisted()` + `vi.mock()`.
- Test behavior, not implementation: assert emitted events, rendered content, computed state.
- Use `data-testid` attributes for reliable selectors.

---

## 12. Docker & Infrastructure

### 12.1 Docker Compose Organization

```
docker/compose/
  docker-compose-infrastructure.yaml  # All infra: PostgreSQL, NATS, Keycloak, Redis, MinIO, ES, monitoring
  docker-compose-services.yaml        # All 13 Quarkus microservices
  docker-compose-full.yaml            # Umbrella: includes all
  services-dependencies.yaml          # Service dependency ordering
  .env                                # Environment variables
```

### 12.2 Service Ports

**Frontends:** buyer-web=3000, seller-portal=3001, admin-dashboard=3002

**Backend services (sequential):** gateway=8080, auction=8081, catalog=8082, user=8083, payment=8084, notification=8085, media=8086, search=8087, seller=8088, broker=8089, analytics=8090, compliance=8091, co2=8092

**Infrastructure:** PostgreSQL=5432, NATS=4222, Redis=6379, Keycloak=8180, MinIO=9000/9001, Elasticsearch=9200, Prometheus=9090, Grafana=3333, MailHog=8025

### 12.3 Container Naming

All containers prefixed with the `PROJECT_NAME` variable from `.env`.

### 12.4 Network

All services share the Docker bridge network defined in the infrastructure compose file. When recreating containers manually, always include `--network-alias <service-name>` for gateway DNS resolution.

### 12.5 Health Checks

Every service in Docker Compose MUST have a health check. Use `depends_on` with `condition: service_healthy` for startup ordering.

### 12.6 Docker Build Rules

- Base image: `eclipse-temurin:21-jre-alpine` with Quarkus fast-jar.
- Multi-stage builds: build in one stage, copy fast-jar to runtime stage.
- Never include source code, build tools, or test data in production images.

---

## 13. Git & CI/CD

### 13.1 Commit Messages: Conventional Commits

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

| Type | Purpose | Example |
|------|---------|---------|
| `feat` | New feature | `feat(catalog): add lot approval workflow` |
| `fix` | Bug fix | `fix(auction-engine): handle null bid amount` |
| `docs` | Documentation | `docs: update CONVENTIONS.md` |
| `refactor` | Code change, no feature/fix | `refactor(user-service): extract auto-registration` |
| `test` | Tests | `test(catalog): add LotResource integration tests` |
| `build` | Build system | `build: upgrade to Quarkus 3.31` |
| `ci` | CI configuration | `ci: add lint step to GitHub Actions` |
| `chore` | Maintenance | `chore: update dependencies` |

**Scopes**: service names (`catalog`, `auction-engine`, `buyer-web`, `seller-portal`, `admin-dashboard`), `shared`, `compose`, `infra`.

### 13.2 Branching

```
main (protected, always deployable)
  +-- feat/TICKET-123-add-lot-versioning
  +-- fix/TICKET-456-bid-timeout
  +-- chore/TICKET-789-update-deps
```

- Feature branches live < 2 days.
- Rebase onto `main` before merging.
- Branch naming: `{type}/{ticket}-{short-description}` in kebab-case.

### 13.3 Pull Request Convention

```markdown
## Summary
- [1-3 bullet points]

## Test Plan
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing: [steps]

## Breaking Changes
None | [Description]
```

Target: 200-400 lines of changed code per PR.

### 13.4 CI Pipeline Stages

```
lint -> test -> security-scan -> build -> deploy-staging -> deploy-production
```

GitHub Actions pipeline (`.github/workflows/ci.yml`). Use path filters to run only affected service pipelines.

---

## 14. Security

### 14.1 OWASP Top 10 Mitigations

| Risk | Mitigation |
|------|-----------|
| Broken Access Control | Casbin RBAC + `@RolesAllowed` on every endpoint; check permissions before resource existence |
| Injection | Parameterized queries only (Panache HQL); never string-concatenate SQL |
| Cryptographic Failures | TLS in production; JWT validation via JWKS; secrets in env variables |
| Security Misconfiguration | Disable unused Keycloak flows; remove default credentials before prod |
| XSS | Vue auto-escapes `{{ }}`; never use `v-html` with unsanitized content |
| Authentication Failures | PKCE for SPA; short token lifetimes; account lockout in Keycloak |
| SSRF | Validate/allowlist URLs for media uploads; MinIO only accessible via internal network |

### 14.2 CSP Headers

Gateway-service should set security headers:

```
Content-Security-Policy: default-src 'self'
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Referrer-Policy: strict-origin-when-cross-origin
```

### 14.3 CORS

```yaml
quarkus:
  http:
    cors:
      origins: http://localhost:3000,http://localhost:3001,http://localhost:3002
      methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
      headers: Authorization,Content-Type,Accept
```

**Never** use wildcard `*` with `Allow-Credentials: true`. Allowlist specific origins.

### 14.4 Dependency Scanning

- **Kotlin**: `dependency-check-gradle` plugin (`./gradlew dependencyCheckAnalyze`) + Trivy on Docker image.
- **Vue**: `npm audit` + Trivy.

---

## 15. Documentation Standards

### 15.1 Architecture Decision Records (ADRs)

Store in `docs/adr/` using the Nygard format:

```markdown
# ADR-NNNN: Title

## Status
Proposed | Accepted | Deprecated | Superseded by ADR-NNNN

## Context
What is the issue?

## Decision
What is the change?

## Consequences
What becomes easier or harder?
```

### 15.2 API Documentation

- Quarkus: `quarkus-smallrye-openapi` auto-generates OpenAPI spec. Swagger UI at `/q/swagger-ui` in dev mode.
- Every REST endpoint MUST have: summary, request/response schema, error responses, auth requirements.

### 15.3 Code Comments

- `/** KDoc */` for exported function documentation (Kotlin).
- `// --- Section Header ---` styled delimiters for visual section breaks in resources.
- Inline `//` comments only where the logic is not self-evident.
- Do NOT add comments that restate what the code already says.
- **KDoc safety:** Never put `*/` inside KDoc blocks (e.g., `/me/*` will close the comment). Use backtick code spans instead.

---

## 16. Critical Gotchas

These are real bugs that were found and fixed. AI agents MUST be aware of them:

1. **Catalog pagination is 0-based** -- `page=0` is the first page. Frontends showing page 1 must send `page=0`.
2. **Country codes are VARCHAR(3)** -- use ISO codes ("NL", "DE"), never full names ("Netherlands").
3. **`sellerId` in catalog-service is Keycloak UUID** -- not the internal user-service UUID.
4. **LotSummaryResponse vs LotResponse** -- summary (list view) has fewer fields than detail. If you need a field in lists, add it to both the DTO and the mapper.
5. **Auto-registration pattern** -- user-service and seller-service auto-create profiles on first `/me` access from JWT claims. New services with `/me` endpoints should follow this pattern.
6. **OIDC config required** -- all services need `roles.role-claim-path: realm_access/roles` and `token.principal-claim: sub` in application.yml, otherwise `@RolesAllowed` fails.
7. **KDoc comments** -- never put `*/` inside KDoc blocks (e.g., `/me/*` will close the comment). Use backtick code spans instead.
8. **Docker network aliases** -- when recreating containers manually, always include `--network-alias <service-name>` for gateway DNS resolution.
9. **Keycloak password** -- test user password is `password123`, not `test`.
10. **Port mapping** -- gateway-service is 8080, auction-engine is 8081, then sequential. See section 12.2 for the full list.
11. **Keycloak inject timing** -- `inject('keycloak')` MUST be captured during `setup()`, never inside async callbacks or axios interceptors.
12. **Lot field normalization** -- backend uses flat fields (`locationCity`, `locationCountry`), frontend expects nested `location` object. Always use `normalizeLot()` and `toBackendPayload()` transforms.
13. **Auction-engine field names** -- returns `auctionId` not `id`, `endTime` not `endDate`. Needs normalization on the frontend.

---

## 17. What NOT To Do

- Do NOT add unnecessary error handling for scenarios that can't happen.
- Do NOT add docstrings/comments to code you did not change.
- Do NOT create helper abstractions for one-time operations.
- Do NOT use `any` type in TypeScript -- it is **strictly forbidden**. The only exception is raw API response deserialization before normalization.
- Do NOT return entities from services -- always return domain models.
- Do NOT use `public` schema in PostgreSQL -- always `app`.
- Do NOT use constructor injection in JAX-RS resources -- use `@Inject lateinit var`. (Constructor injection IS preferred for services and repositories.)
- Do NOT mix component ordering conventions between apps (script-first for Vite, template-first for Nuxt).
- Do NOT hardcode user-facing strings -- use i18n keys (buyer-web) or at minimum keep strings in constants.
- Do NOT persist auth tokens in localStorage -- Keycloak SSO cookies manage the session.
- Do NOT use `var` in Kotlin unless mutation is genuinely required (entities excepted).
- Do NOT use SLF4J or java.util.logging -- use JBoss `org.jboss.logging.Logger`.
- Do NOT auto-ack NATS messages in production consumers.
- Do NOT modify Flyway migrations after they have been applied to any environment.
- Do NOT use string concatenation in SQL queries -- always use parameterized queries.
- Do NOT use wildcard CORS origins (`*`) with credentials.

---

## Appendix A: Quick Reference -- What Goes Where

| I need to... | Frontend | Backend |
|-------------|----------|---------|
| Add a new page | `pages/*.vue` (Nuxt) or `src/views/{Feature}View.vue` + route (Vite) | n/a |
| Add a new REST endpoint | n/a | `api/v1/resource/{Feature}Resource.kt` |
| Add a new domain entity | `src/types/` or composable file | `domain/model/` + Flyway `V{n}__*.sql` |
| Add a composable | `src/composables/use{Feature}.ts` or `composables/` (Nuxt) | n/a |
| Add a Pinia store | `src/stores/{feature}.ts` | n/a |
| Add an API wrapper | `src/composables/useApi.ts` (extend) | n/a |
| Add a service class | n/a | `application/service/{Feature}Service.kt` |
| Add a repository | n/a | `infrastructure/persistence/repository/{Entity}Repository.kt` |
| Add an entity | n/a | `infrastructure/persistence/entity/{Entity}Entity.kt` |
| Add a DTO | n/a | `api/dto/{Feature}Request.kt` / `{Feature}Response.kt` |
| Add a domain event | n/a | `shared/nats-events/.../events/{domain}/` + register in `BaseEvent` |
| Add a Casbin policy | n/a | Update `casbin_policy.csv` in service resources |
| Add a DB migration | n/a | `src/main/resources/db/migration/V{n}__{description}.sql` |
| Add a translation key | `i18n/locales/en.json` (buyer-web only) | n/a |

## Appendix B: Key Architectural Decisions Summary

| Decision | Rationale |
|----------|-----------|
| Panache ORM over raw JDBC | Reduced boilerplate for CRUD; HQL for custom queries; aligns with Quarkus ecosystem |
| Database per service | Service autonomy; independent schema evolution; no cross-service joins |
| Event sourcing in auction-engine only | Bidding requires complete audit trail and replay; other services use simpler CRUD |
| Outbox pattern for events | Transactional consistency between DB writes and NATS publishing |
| REST (JAX-RS) over gRPC | Simpler debugging, browser-compatible, sufficient for current scale. gRPC could be added for high-throughput inter-service calls |
| Casbin + Keycloak RBAC | Keycloak for authentication (OIDC/JWT); Casbin for fine-grained resource-level authorization |
| No token persistence (Vue) | Keycloak SSO cookies manage session; avoids token theft from localStorage |
| Three separate frontends | Separation of concerns by user role; independent deployability; different SSR needs (buyer SEO vs admin SPA) |
| Axios for Vite SPAs, $fetch for Nuxt | Axios provides interceptors for token management; $fetch is Nuxt-native with SSR support |
| Tailwind CSS over BEM | Utility-first approach reduces context-switching; design tokens via Tailwind config |
| Composable layer enforcement | Views never import services directly; composables provide testable, swappable data access |
| Field injection in JAX-RS resources | Quarkus RESTEasy requires no-arg constructor; constructor injection for all other beans |
| UUIDv7 for all IDs | Time-sortable; no coordination required; index-friendly |
| `to.path` not `to.fullPath` in auth guards | Avoids Keycloak `redirect_uri` query string overflow |

---

## Appendix C: Sources and References

### Vue / TypeScript
- [Vue.js Official Docs](https://vuejs.org/) -- Composition API, TypeScript, Performance, Security
- [Vue.js Style Guide](https://vuejs.org/style-guide/)
- [Pinia Documentation](https://pinia.vuejs.org/)
- [Nuxt 3 Documentation](https://nuxt.com/docs)
- [eslint-plugin-vue](https://eslint.vuejs.org/)
- [Tailwind CSS Documentation](https://tailwindcss.com/docs)

### Kotlin / Quarkus
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Quarkus Guides](https://quarkus.io/guides/) -- REST, Security, Testing, Config, Flyway, Panache
- [Gradle Kotlin DSL Best Practices](https://kotlinlang.org/docs/gradle-best-practices.html)

### Cross-Cutting
- [RFC 9457: Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457.html)
- [Microsoft REST API Guidelines](https://github.com/microsoft/api-guidelines)
- [12-Factor App](https://12factor.net/)
- [OpenTelemetry Documentation](https://opentelemetry.io/)
- [OWASP Top 10:2025](https://owasp.org/Top10/2025/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Casbin RBAC Documentation](https://casbin.org/docs/rbac/)
- [NATS JetStream Documentation](https://docs.nats.io/nats-concepts/jetstream)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
