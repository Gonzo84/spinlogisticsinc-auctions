# Code Conventions & AI Agent Guidance

> This file is the single source of truth for code style, patterns, and constraints.
> AI agents MUST follow these conventions when generating or modifying code.
> Maintained by the project owner — updates here override any conflicting defaults.

---

## General Rules

- **Indent:** 2 spaces for Kotlin, TypeScript, Vue, JSON, YAML, CSS, HTML. 4 spaces for everything else.
- **Line endings:** LF everywhere.
- **Max line length:** 120 characters (Kotlin), no hard limit for frontend but keep readable.
- **Final newline:** Always.
- **No trailing whitespace** (except Markdown).
- **Commit style:** Conventional Commits — `feat:`, `fix:`, `refactor:`, `chore:`, `docs:`. Short imperative description.

---

## Backend (Kotlin / Quarkus)

### Package Structure (Hexagonal Architecture)

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

### Naming

| Thing | Convention | Example |
|-------|-----------|---------|
| Classes | PascalCase | `UserService`, `LotEntity`, `BidPlacedEvent` |
| Functions | camelCase | `getUserById()`, `placeBid()` |
| Constants | SCREAMING_SNAKE_CASE | `LOG`, `NIL_UUID`, `MAX_RETRIES` |
| Request DTOs | `*Request` suffix | `CreateLotRequest`, `RegisterUserRequest` |
| Response DTOs | `*Response` suffix | `LotResponse`, `UserResponse` |
| Entities | `*Entity` suffix | `UserEntity`, `LotEntity` |
| Repositories | `*Repository` suffix | `UserRepository`, `LotRepository` |
| Resources | `*Resource` suffix | `UserResource`, `AuctionResource` |
| Error codes | SCREAMING_SNAKE_CASE strings | `"USER_NOT_FOUND"`, `"BID_BELOW_MINIMUM"` |
| DB columns | snake_case | `created_at`, `seller_id`, `location_country` |
| DB schema | Always `app` | Never use `public` |

### REST Resources

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

Rules:
- All paths under `/api/v1/`
- Responses wrapped in `ApiResponse.ok(data)` — never return raw objects
- Paginated responses use `PagedResponse<T>` inside `ApiResponse`
- Use `@RolesAllowed` or `@PermitAll` on every endpoint — roles: `buyer_active`, `seller_verified`, `broker_active`, `admin_ops`, `admin_super`
- Bearer token extraction via `@HeaderParam("Authorization")` + extension functions from `SecurityContext.kt`
- Return `Response.ok(...)` for 200, `Response.status(CREATED).entity(...)` for 201, `Response.noContent()` for 204
- Group endpoints with `// --- Section Name ---` comments

### DTOs

```kotlin
// Request — nullable optionals with defaults
data class CreateFooRequest(
    val name: String,
    val description: String? = null,
    val amount: BigDecimal = BigDecimal.ONE
)

// Response — all fields explicit
data class FooResponse(
    val id: UUID,
    val name: String,
    val createdAt: Instant
)
```

- Request DTOs: optional fields are nullable with `= null` default
- Response DTOs: all fields required (no defaults)
- Use `@JsonAlias` when frontend field names differ from backend
- DTO mappers are extension functions in `DtoMappers.kt`:
  ```kotlin
  fun Foo.toResponse(): FooResponse = FooResponse(id = id, name = name, ...)
  ```

### Application Services

```kotlin
@ApplicationScoped
class FooService {
    @Inject lateinit var fooRepository: FooRepository

    companion object {
        private val LOG: Logger = Logger.getLogger(FooService::class.java)
    }

    @Transactional
    fun create(request: CreateFooRequest): Foo {
        // validation, business logic
        return fooRepository.save(entity).toDomain()
    }

    fun getById(id: UUID): Foo {
        return fooRepository.findById(id)?.toDomain()
            ?: throw NotFoundException(code = "FOO_NOT_FOUND", message = "Foo '$id' not found.")
    }
}
```

- `@ApplicationScoped` on all services
- `@Transactional` on mutating methods only (not reads)
- Services return domain models, never entities or DTOs
- Throw structured exceptions with `code` + `message`

### Error Handling

Exception hierarchy in `shared/kotlin-commons`:

| Exception | HTTP | Use for |
|-----------|------|---------|
| `NotFoundException` | 404 | Resource not found |
| `ConflictException` | 409 | Duplicate / state conflict |
| `ValidationException` | 422 | Invalid input (carries field error map) |
| `ForbiddenException` | 403 | Insufficient permissions |

Response format (RFC 7807):
```json
{"status": 404, "title": "Not Found", "code": "FOO_NOT_FOUND", "message": "..."}
```

For domain-specific errors, use sealed classes:
```kotlin
sealed class FooException(code: String, message: String)
    : DomainException(code, message) {
    class FooNotFound(id: UUID) : FooException("FOO_NOT_FOUND", "Foo '$id' not found")
}
```

### Repositories

- Extend `PanacheRepositoryBase<Entity, UUID>`
- Queries use Panache HQL with camelCase field names (not SQL column names)
- Finder methods return `Entity?` (nullable)
- Search/list methods return `Pair<List<Entity>, Long>` for items + total count
- Pagination is 0-based internally

### Entities

- Mutable classes (required by Quarkus/Hibernate)
- Always include `toDomain()` instance method and `fromDomain()` companion factory
- Enums stored as `EnumType.STRING`
- Timestamps use `Instant`
- UUIDs as primary keys with `gen_random_uuid()` default in DB

### Database Migrations (Flyway)

- File naming: `V001__create_foos.sql`, `V002__add_column_to_foos.sql`
- All tables in `app` schema
- Column names: `snake_case`
- Country codes: `VARCHAR(3)` (ISO alpha-2/3)
- Status columns: `VARCHAR(20) NOT NULL DEFAULT 'DRAFT'`
- Timestamps: `TIMESTAMPTZ`

### IDs

- Use `IdGenerator.generateUUIDv7()` for all new IDs
- Typed value objects in auction-engine: `AuctionId`, `BidId`, `LotId`, `UserId`
- Plain `UUID` in other services

### Logging

- Logger in companion object named `LOG`:
  ```kotlin
  companion object {
      private val LOG: Logger = Logger.getLogger(MyClass::class.java)
  }
  ```
- Use `LOG.infof(...)`, `LOG.warnf(...)`, `LOG.errorf(...)` with format strings

---

## Frontend (Vue 3 / Nuxt 3 / TypeScript)

### Project Layout

| App | Framework | Style |
|-----|-----------|-------|
| `buyer-web` | Nuxt 3 (SSR) | Template-first, `$fetch`, `$keycloak` plugin |
| `seller-portal` | Vite SPA | Script-first, axios, `inject('keycloak')` |
| `admin-dashboard` | Vite SPA | Script-first, axios, `inject('keycloak')` |

### Component Conventions

```
Components:  PascalCase files — BidPanel.vue, LotCard.vue, SidebarNav.vue
Views:       *View.vue suffix — LotDetailView.vue, DashboardView.vue
Composables: use*.ts prefix — useLots.ts, useAuth.ts, useApi.ts
```

#### Component structure

**Vite SPAs (seller-portal, admin-dashboard):** `<template>` first, then `<script setup>`, then `<style>`

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

const props = withDefaults(defineProps<{
  title: string
  count?: number
}>(), {
  count: 0,
})

const emit = defineEmits<{
  submit: [data: FormData]
  cancel: []
}>()
</script>

<template>
  <!-- ... -->
</template>
```

**Nuxt (buyer-web):** `<template>` first, then `<script setup>`

```vue
<template>
  <!-- ... -->
</template>

<script setup lang="ts">
import type { Lot } from '~/types/auction'

const props = defineProps<{ lot: Lot }>()
</script>
```

### TypeScript

- `strict: true` always
- **`any` type is STRICTLY FORBIDDEN.** Use proper interfaces/types for everything. The ONLY exception is raw API response data before it is normalized (e.g., `raw` from `get<any>('/endpoint')`) — and even then, normalize into a typed object immediately.
- Props: `defineProps<T>()` with TypeScript generics — never use `PropType`
- Emits: `defineEmits<{ event: [payload] }>()` typed form
- Use `withDefaults()` for default prop values
- `reactive()` for form objects, `ref()` for individual values
- `computed()` for derived state
- `readonly()` when exposing refs from composables

### Composable Pattern

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

- Named `use*`
- Return plain objects (not reactive)
- Expose `loading` and `error` as `readonly()`
- Types co-located in the composable file (seller-portal, admin-dashboard) or in `types/` directory (buyer-web)

### API Response Unwrapping

All backends return `ApiResponse<T>` wrapper `{ data: T }`. Frontend MUST unwrap:

```typescript
// Paginated: raw.data.items
const response = raw?.data && typeof raw.data === 'object' ? raw.data : raw
const items = response.items ?? []

// Single item: raw.data
const item = raw?.data ?? raw
```

### Backend Field Normalization

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

### Pinia Stores

Composition API (Setup Stores) style:

```typescript
export const useFooStore = defineStore('foo', () => {
  const items = ref<Foo[]>([])
  const current = ref<Foo | null>(null)

  const itemCount = computed(() => items.value.length)

  function setItems(newItems: Foo[]) {
    items.value = newItems
  }

  function clear() {
    items.value = []
    current.value = null
  }

  return { items, current, itemCount, setItems, clear }
})
```

- Use `ref()` for state, `computed()` for getters, plain functions for actions
- Return only what should be publicly accessible
- Type refs explicitly: `ref<Foo[]>([])`, `ref<Foo | null>(null)`
- **State MUST NOT be mutated directly from components.** Expose state as `readonly()` and provide explicit action functions for mutations:
  ```typescript
  return { items: readonly(items), current: readonly(current), itemCount, setItems, clear }
  ```

### Tailwind CSS

Design tokens:
- `primary` — blue (#1e40af)
- `secondary` — green (#059669)
- `accent` — amber (#d97706)
- `warning` — red (#dc2626)
- Font: Inter (body), JetBrains Mono (code)

Reusable classes defined in `@layer components`:
```css
.btn-primary { @apply bg-primary-600 text-white hover:bg-primary-700 ...; }
.card        { @apply rounded-xl border border-gray-200 bg-white p-6 shadow-sm; }
.input       { @apply block w-full rounded-lg border border-gray-300 ...; }
.badge       { @apply inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium; }
```

### Auth

| App | Auth method |
|-----|------------|
| buyer-web | `useNuxtApp().$keycloak` plugin |
| seller-portal | `inject('keycloak')` in setup() |
| admin-dashboard | `inject('keycloak')` in setup() |

Keycloak must be captured during `setup()` — never inside async callbacks or interceptors.

### Routing

- **buyer-web:** Nuxt file-based routing in `pages/`
- **seller-portal / admin-dashboard:** Vue Router in `src/router/index.ts`

### i18n (buyer-web only)

Dot-notation keys: `$t('auction.timeRemaining')`. Translation files in `i18n/locales/`.

---

## Critical Gotchas

These are real bugs that were found and fixed. AI agents MUST be aware of them:

1. **Catalog pagination is 0-based** — `page=0` is the first page. Frontends showing page 1 must send `page=0`.
2. **Country codes are VARCHAR(3)** — use ISO codes ("NL", "DE"), never full names ("Netherlands").
3. **`sellerId` in catalog-service is Keycloak UUID** — not the internal user-service UUID.
4. **LotSummaryResponse vs LotResponse** — summary (list view) has fewer fields than detail. If you need a field in lists, add it to both the DTO and the mapper.
5. **Auto-registration pattern** — user-service and seller-service auto-create profiles on first `/me` access from JWT claims. New services with `/me` endpoints should follow this pattern.
6. **OIDC config required** — all services need `roles.role-claim-path: realm_access/roles` and `token.principal-claim: sub` in application.yml, otherwise `@RolesAllowed` fails.
7. **KDoc comments** — never put `*/` inside KDoc blocks (e.g., `/me/*` will close the comment). Use backtick code spans instead.
8. **Docker network aliases** — when recreating containers manually, always include `--network-alias <service-name>` for gateway DNS resolution.
9. **Keycloak password** — test user password is `password123`, not `test`.
10. **Port mapping** — gateway-service is 8080, auction-engine is 8081, then sequential. See CLAUDE.md for the full list.

---

## What NOT To Do

- Do NOT add unnecessary error handling for scenarios that can't happen
- Do NOT add docstrings/comments to code you didn't change
- Do NOT create helper abstractions for one-time operations
- Do NOT use `any` type in TypeScript — it is **strictly forbidden**. The only exception is raw API response deserialization before normalization.
- Do NOT return entities from services — always return domain models
- Do NOT use `public` schema in PostgreSQL — always `app`
- Do NOT use constructor injection in JAX-RS resources — use `@Inject lateinit var`
- Do NOT mix component ordering conventions between apps (script-first for Vite, template-first for Nuxt)
