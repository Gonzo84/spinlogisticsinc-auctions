# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
npm install              # Install deps (postinstall runs patch-pinia.mjs automatically)
npm run dev              # Nuxt dev server with HMR
npm run build            # Production build
npm run preview          # Preview production build locally
npm run lint             # ESLint
npm run typecheck        # TypeScript check via Nuxt
```

No test framework is configured — there are no unit or e2e tests.

## Architecture

Nuxt 3 SSR application — the buyer-facing frontend for an EU B2B industrial equipment auction platform.

### Key Tech

- **Nuxt 3.14** with hybrid SSR rendering (see route rules below)
- **Pinia** for state management (4 stores: auth, auction, cart, notifications)
- **Keycloak-js** for OIDC authentication (PKCE flow, `buyer-web` client)
- **TailwindCSS** for styling
- **@nuxtjs/i18n** — 7 languages (en, nl, de, fr, pl, it, ro), lazy-loaded, `prefix_except_default` strategy
- **Chart.js + vue-chartjs** for analytics charts

### SSR Route Rules

| Route | Rendering | Reason |
|-------|-----------|--------|
| `/` | Pre-rendered | Static homepage |
| `/search` | SWR (5 min cache) | Cached search results |
| `/lots/**`, `/my/**`, `/checkout/**`, `/auth/**`, `/profile/**` | Client-only (`ssr: false`) | Requires interactivity or auth |

### Runtime Config (Environment Variables)

All API URLs configurable via `NUXT_PUBLIC_*` env vars, defaulting to local dev:

- `NUXT_PUBLIC_API_BASE_URL` → `http://localhost:8080/api/v1` (gateway-service)
- `NUXT_PUBLIC_WS_BASE_URL` → `ws://localhost:8080/ws`
- `NUXT_PUBLIC_KEYCLOAK_URL` → `http://localhost:8180`
- `NUXT_PUBLIC_KEYCLOAK_REALM` → `auction-platform`
- `NUXT_PUBLIC_KEYCLOAK_CLIENT_ID` → `buyer-web`

## Code Organization

```
composables/       # Business logic (API calls, auth, WebSocket)
stores/            # Pinia stores (auth, auction, cart, notifications)
plugins/           # Client-only plugins: api.client.ts ($api), keycloak.client.ts ($keycloak)
pages/             # File-based routing (Nuxt conventions)
components/        # UI components organized by domain (auction/, search/, shared/, notifications/)
layouts/           # default.vue (Navbar + slot + Footer)
i18n/locales/      # Translation JSON files per language
scripts/           # patch-pinia.mjs (SSR hydration fix)
```

### API Layer

All HTTP calls go through `$api` (a `$fetch` wrapper from `plugins/api.client.ts`):
- Automatically attaches `Authorization: Bearer <token>` from auth store
- Handles 401 (redirect to Keycloak login), 403 (redirect to home), 5xx (logs error)
- Access in composables: `const { $api } = useNuxtApp()`

### Composables

| Composable | Purpose |
|-----------|---------|
| `useAuth` | Login/logout/register via Keycloak, token refresh, profile update, role checks |
| `useAuction` | Fetch lots from catalog-service, map backend fields to frontend `Auction` type, WebSocket subscriptions |
| `useBid` | Place bids, set/cancel auto-bids |
| `useSearch` | Full-text search via search-service, autocomplete suggestions |
| `useNotifications` | Fetch/mark-read notifications, real-time WebSocket subscription |
| `useWebSocket` | WebSocket lifecycle, event-based subscriptions (bid_placed, auction_extended, auction_closed, overbid) |

### Backend Field Mapping (Critical)

Backend services return different field names than the frontend expects. `useAuction.ts` contains `mapAuctionResponse()` and `unwrapApiResponse()` to handle this:

- **Auction-engine** returns `auctionId` (not `id`), `currentHighBid` (not `currentBid`), `startingBid` (not `startingPrice`)
- **Catalog-service** returns flat location fields (`locationCity`, `locationCountry`) — frontend composes these into a location string
- **API responses** are wrapped in `ApiResponse<T>` (`{data: T}`) — must call `unwrapApiResponse()` to unwrap
- **Catalog-service pagination** is 0-based (`page=0` = first page)
- **Bid increments** are tiered based on current bid amount — `computeMinIncrement()` mirrors backend's `AuctionConstants`

### Authentication Flow

1. `plugins/keycloak.client.ts` initializes Keycloak with PKCE on app mount (client-only)
2. Tokens stored in `localStorage` (`kc_token`, `kc_refresh_token`) and synced to auth store
3. Auto-refresh every 30 seconds via `setInterval` + `keycloak.updateToken(60)`
4. `public/silent-check-sso.html` used for silent SSO check iframe
5. Auth guard: composable `useAuth().requireAuth()` — no middleware files

### Stores

- **auth** — User session (token, user profile, roles). Getters: `fullName`, `initials`, `isBusiness`, `hasRole(role)`
- **auction** — Current lot being viewed + bid history. Actions: `addBid`, `extendAuction`, `closeAuction`, `setAutoBid`
- **cart** — Won lots awaiting payment. Calculates subtotal + buyer premium + VAT
- **notifications** — Unread count + recent notifications (max 50). Filtered getters for overbid alerts

### Pinia SSR Fix

`scripts/patch-pinia.mjs` patches Pinia's `shouldHydrate()` to use `Object.prototype.hasOwnProperty.call()` instead of `obj.hasOwnProperty()`. This fixes SSR hydration crashes caused by null-prototype objects from `@nuxtjs/i18n`. Runs automatically on `npm install`.
