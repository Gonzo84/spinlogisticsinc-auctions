# Buyer-Web Refactoring Plan

## Context

The buyer-web frontend is a Nuxt 3 SSR application that works but has accumulated technical debt across several areas: missing type safety, duplicated logic, hardcoded strings, memory leaks, inconsistent data fetching patterns, and no shared utilities layer. This plan applies the Architecture Guidance principles (convention over configuration, modular architecture, Composition API best practices, proper data fetching, state immutability) to bring the codebase to modern Nuxt 3 standards.

Analysis was performed by 4 parallel research agents covering: all 9 components, 6 composables, 4 stores, 2 plugins, all pages, configs, and i18n files.

---

## Step 1: Create `types/` directory with shared TypeScript interfaces

**Problem:** Types are scattered across composables and stores, duplicated or missing. API responses typed as `Record<string, unknown>` with unsafe property access chains.

**Files to create:**
- `types/api.ts` — `ApiResponse<T>`, `PagedResponse<T>`, API error types
- `types/auction.ts` — `Auction`, `Bid`, `AuctionListParams`, `AuctionListResult` (move from `stores/auction.ts` and `composables/useAuction.ts`)
- `types/search.ts` — `SearchFilters`, `SearchResult`, `SearchSuggestion` (move from `composables/useSearch.ts`)
- `types/notification.ts` — `Notification` type (move from `stores/notifications.ts`)
- `types/cart.ts` — `CartLot` type (move from `stores/cart.ts`)
- `types/user.ts` — `User` type (move from `stores/auth.ts`)
- `types/index.ts` — barrel re-export

**Impact:** All composables, stores, and components import from `~/types/` instead of defining inline.

---

## Step 2: Create `utils/` directory for stateless helpers

**Problem:** Utility functions duplicated across composables (unwrapApiResponse in 4 files, formatCurrency in 3 components, country flags in 2 components, computeMinIncrement in useAuction).

**Files to create:**
- `utils/api-response.ts` — `unwrapApiResponse<T>()` (currently duplicated in useAuction, useSearch, useNotifications, useAuth)
- `utils/format.ts` — `formatCurrency()`, `formatTimeAgo()` (currently inline in Navbar, BidPanel, LotCard, etc.)
- `utils/constants.ts` — Country flags map, category list, price ranges, distance options (currently hardcoded in FilterSidebar + LotCard + homepage)
- `utils/auction-mapper.ts` — `mapAuctionResponse()`, `computeMinIncrement()` (move from useAuction.ts)

**Impact:** Single source of truth for shared logic. Components and composables import from `~/utils/`.

---

## Step 3: Create `middleware/auth.ts` route middleware

**Problem:** Auth guards are manually called via `useAuth().requireAuth()` in 5+ pages (my/bids, my/watchlist, my/purchases, profile, checkout). Repetitive and error-prone.

**Files to create:**
- `middleware/auth.ts` — Nuxt route middleware that checks auth state and redirects to Keycloak login

**Files to modify:**
- `pages/my/bids.vue` — Replace manual `requireAuth()` with `definePageMeta({ middleware: 'auth' })`
- `pages/my/watchlist.vue` — Same
- `pages/my/purchases.vue` — Same
- `pages/profile/index.vue` — Same
- `pages/checkout/[id].vue` — Same

---

## Step 4: Create `error.vue` error boundary

**Problem:** No global error page. Unhandled errors show raw Nuxt error screen.

**Files to create:**
- `error.vue` — Global error boundary with i18n support, back-to-home button, different layouts for 404 vs 500

---

## Step 5: Fix i18n — add missing locale keys + hardcoded strings

**Problem:** All 6 non-English locales are missing the `bids` and `watchlistPage` translation objects (29 keys total). Several hardcoded English strings exist in components.

**Files to modify:**
- `i18n/locales/nl.json` — Add `bids` and `watchlistPage` objects
- `i18n/locales/de.json` — Same
- `i18n/locales/fr.json` — Same
- `i18n/locales/pl.json` — Same
- `i18n/locales/it.json` — Same
- `i18n/locales/ro.json` — Same
- `components/shared/Navbar.vue` — Replace hardcoded `formatTimeAgo()` strings ("just now", "m ago", "h ago", "d ago") with `$t()` keys
- `components/auction/BidPanel.vue` — Replace hardcoded "Minimum bid is..." error with i18n key
- `pages/my/watchlist.vue` — Replace hardcoded "Starting bid" with `$t()` key
- `i18n/locales/en.json` — Add keys for error page, timeAgo strings

---

## Step 6: Fix store immutability — auction and notifications stores

**Problem:** `stores/auction.ts` and `stores/notifications.ts` directly mutate nested state (`this.currentAuction.currentBid = x`, `notification.read = true`). Anti-pattern that can break reactivity tracking.

**Files to modify:**
- `stores/auction.ts` — Use spread operators for state updates:
  - `addBid()`: `this.currentAuction = { ...this.currentAuction, currentBid: bid.amount, bidCount: this.bids.length }`
  - `extendAuction()`: `this.currentAuction = { ...this.currentAuction, endTime, status: 'extended' }`
  - `closeAuction()`: `this.currentAuction = { ...this.currentAuction, status: 'closed' }`
- `stores/notifications.ts` — Use `map()` instead of direct mutation:
  - `markAsRead()`: `this.recentNotifications = this.recentNotifications.map(n => n.id === id ? { ...n, read: true } : n)`
  - `markAllAsRead()`: Same pattern

---

## Step 7: Fix WebSocket memory leaks

**Problem:** `useAuction.ts` registers event handlers via `ws.onBidPlaced()`, `ws.onAuctionExtended()`, `ws.onAuctionClosed()` but never removes them on unsubscribe. Each page visit accumulates handlers.

**Files to modify:**
- `composables/useWebSocket.ts` — Add `off(event, handler)` method that removes specific handlers from the `eventHandlers` Map
- `composables/useAuction.ts` — Store handler references and call `ws.off()` in `unsubscribeFromAuction()`

---

## Step 8: Refactor composables to use shared utils

**Problem:** After creating `types/` and `utils/`, composables need to import from them instead of defining inline.

**Files to modify:**
- `composables/useAuction.ts` — Import types from `~/types/`, move `unwrapApiResponse`/`mapAuctionResponse`/`computeMinIncrement` to `~/utils/`, import from there
- `composables/useSearch.ts` — Import types from `~/types/`, use shared `unwrapApiResponse` from `~/utils/api-response`
- `composables/useNotifications.ts` — Import types from `~/types/`, use shared `unwrapApiResponse`
- `composables/useAuth.ts` — Import types from `~/types/`, use shared `unwrapApiResponse`
- `composables/useBid.ts` — Import types from `~/types/`

---

## Step 9: Refactor homepage (pages/index.vue)

**Problem:** 1300+ lines with hardcoded arrays (countries, categories, how-it-works steps), inline carousel logic, and CO2 counter animation all in one file.

**Changes:**
- `pages/index.vue` — Extract:
  - Move hardcoded data arrays to `~/utils/constants.ts` (already created in Step 2)
  - Extract hero carousel to `components/home/HeroCarousel.vue`
  - Extract category grid to `components/home/CategoryGrid.vue`
  - Extract "how it works" section to `components/home/HowItWorks.vue`
  - Extract CO2 counter to `components/home/CO2Counter.vue`
  - Target: page under 200 lines

---

## Step 10: Improve data fetching patterns

**Problem:** Homepage uses `useAsyncData` with `server: false` (not SSR), search page uses manual `watch()` on route params. Per Nuxt best practices, pages should use `useFetch`/`useAsyncData` for SSR-compatible data.

**Files to modify:**
- `pages/index.vue` — Change from `useAsyncData(..., { server: false })` to proper `useFetch()` for featured lots (homepage is pre-rendered, data should be available at build time)
- `pages/lots/[id].vue` — Already uses `useAsyncData` (verify it's correct)

**Note:** `/my/**` and `/checkout/**` pages are correctly client-only (auth required), so `onMounted()` pattern is acceptable there.

---

## Step 11: Add `.env.example` file

**Problem:** No documentation of required environment variables. New developers must read `nuxt.config.ts` to discover them.

**Files to create:**
- `.env.example` — Document all `NUXT_PUBLIC_*` variables with defaults and descriptions

---

## Step 12: Clean up console statements and `as any` casts

**Problem:** 2 `console.log` statements in keycloak plugin (debug-level), 5 `console.error` statements acceptable for production but should use a consistent pattern. 2 `as any` casts in plugins.

**Files to modify:**
- `plugins/keycloak.client.ts` — Replace `console.log` with conditional debug logging (check `import.meta.dev`)
- `plugins/keycloak.client.ts` — Type the pinia injection properly to remove `as any`
- `plugins/api.client.ts` — Same pinia typing fix

---

## Step 13: Add `useHead`/`useSeoMeta` for SEO on key pages

**Problem:** Most pages only set `title` via `useHead()`. Missing OG tags, descriptions, and structured data on high-value pages.

**Files to modify:**
- `pages/index.vue` — Add `useSeoMeta()` with description, OG tags
- `pages/lots/[id].vue` — Add dynamic OG tags (title, description, image from lot data)
- `pages/search.vue` — Add description meta

---

## Verification

After each step:
1. `npm run lint` — Ensure no ESLint errors
2. `npm run typecheck` — Ensure no TypeScript errors
3. `npm run build` — Ensure production build succeeds
4. `npm run dev` — Manual smoke test: homepage loads, search works, lot detail renders, auth flow works

After all steps:
- Verify all 7 locale files have identical key structures
- Verify no `Record<string, unknown>` remains in composable API calls
- Verify no hardcoded English strings in components
- Verify stores use immutable update patterns
- Check browser DevTools for WebSocket handler count stability across page navigations
