# Authentication Isolation: Implementation Plan

## Problem

All three frontends (buyer-web, seller-portal, admin-dashboard) share a single Keycloak
realm (`auction-platform`). Keycloak SSO sessions are realm-scoped: once a user logs in
to any client, the browser's SSO session cookie at `localhost:8180` is reused by all other
clients. This causes:

- Logging into seller-portal as `seller@test.com`, then clicking "Login" on buyer-web,
  auto-authenticates as the seller without showing the Keycloak login page
- The user is now inside buyer-web with a seller identity — wrong role, wrong data
- Same issue affects every combination: buyer ↔ seller ↔ admin ↔ broker

**Root cause:** Keycloak's SSO cookie is shared across all clients in the same realm.
When `keycloak.login()` redirects to Keycloak, Keycloak finds an active session and
issues tokens immediately, skipping the login form entirely.

---

## Recommended Architecture: Direct Access + Role-Aware SSO

Based on how real B2B auction platforms work (TBAuctions/Troostwijk, Ritchie Bros, Amazon
Seller Central), the pattern is:

- **Each app keeps its own URL** — users navigate directly to the app they need
- **Same Keycloak realm** — single user identity, SSO across apps
- **Each app validates user roles** after authentication
- **Multi-role users benefit from SSO** — a user with both `buyer_active` and
  `seller_verified` logs in once, both apps work seamlessly
- **Wrong-role users see a helpful page** with "Switch Account" option

### Why NOT a central login portal?

- Buyer-web needs anonymous browsing (SEO, public marketplace)
- Adds an extra app to maintain and an extra click for every user
- The "app launcher" pattern fits internal enterprise tools (Okta, Cloudflare Access),
  not public-facing marketplaces

### Why NOT separate Keycloak realms?

- Users who are both buyers and sellers would need two separate accounts
- No SSO across apps (realms are fully isolated)
- Double the user management, KYC, and admin overhead

---

## User Experience: What Each User Type Sees

### Scenario 1: Buyer visits buyer-web (happy path)

```
1. User navigates to buyer-web (localhost:3000)
2. Browses lots anonymously — no login required
3. Clicks "Login" → redirected to Keycloak
4. Keycloak login page shows "Sign in to EU Auction Marketplace"
5. User logs in as buyer@test.com (has buyer_active role)
6. Redirected back to buyer-web → role check passes → app works normally
```

### Scenario 2: Seller visits buyer-web (SSO cross-contamination — CURRENT BUG)

**Before (broken):**
```
1. Seller is already logged into seller-portal
2. Opens buyer-web in another tab, clicks "Login"
3. Keycloak SSO cookie auto-authenticates as seller — no login page shown
4. Buyer-web loads with seller identity — WRONG ROLE, confusing UX
```

**After (fixed):**
```
1. Seller is already logged into seller-portal
2. Opens buyer-web in another tab, clicks "Login"
3. Keycloak SSO cookie auto-authenticates as seller — no login page shown
4. buyer-web checks roles → seller@test.com has NO buyer role
5. User sees a "Wrong Account" page:

   ┌─────────────────────────────────────────────────────┐
   │                                                     │
   │              Signed in as seller@test.com            │
   │              (Seller account)                        │
   │                                                     │
   │   This marketplace requires a buyer account to       │
   │   place bids and make purchases.                    │
   │                                                     │
   │   [ Switch Account ]   [ Register as Buyer ]        │
   │                                                     │
   │   ─── or ───                                        │
   │                                                     │
   │   Go to → Seller Portal                             │
   │                                                     │
   └─────────────────────────────────────────────────────┘

6a. "Switch Account" → keycloak.login({ prompt: 'login' })
    → forces Keycloak to show login page regardless of SSO
    → user enters buyer credentials → redirected back → works

6b. "Register as Buyer" → keycloak.register()
    → user creates a buyer account → redirected back → works

6c. "Go to Seller Portal" → navigates to seller-portal URL
```

### Scenario 3: Multi-role user (buyer + seller) — SSO works as a FEATURE

```
1. User logs into seller-portal (has both seller_verified AND buyer_active)
2. Opens buyer-web, clicks "Login"
3. Keycloak SSO auto-authenticates — no login page shown
4. buyer-web checks roles → user has buyer_active → PASS
5. App works normally — SSO was helpful here, not a bug
```

### Scenario 4: Seller visits seller-portal directly

```
1. User navigates to seller-portal (localhost:5174)
2. onLoad: 'login-required' → redirected to Keycloak immediately
3. Keycloak login page shows "Sign in to Seller Portal"
4. User logs in as seller@test.com (has seller_verified role)
5. Redirected back → role check passes → app works normally
```

### Scenario 5: Buyer visits seller-portal (wrong app)

```
1. Buyer is logged in on buyer-web
2. Navigates to seller-portal URL
3. Keycloak SSO auto-authenticates → tokens issued
4. seller-portal checks roles → buyer has NO seller role
5. User sees "Wrong Account" page:

   ┌─────────────────────────────────────────────────────┐
   │                                                     │
   │           Signed in as buyer@test.com               │
   │           (Buyer account)                           │
   │                                                     │
   │   The Seller Portal requires a verified seller      │
   │   account to manage listings and auctions.          │
   │                                                     │
   │   [ Switch Account ]   [ Apply to Become Seller ]   │
   │                                                     │
   │   ─── or ───                                        │
   │                                                     │
   │   Go to → Buyer Marketplace                         │
   │                                                     │
   └─────────────────────────────────────────────────────┘
```

### Scenario 6: Non-admin visits admin-dashboard

```
1. Any non-admin user navigates to admin-dashboard URL
2. Keycloak SSO auto-authenticates (or shows login)
3. admin-dashboard checks roles → no admin_ops or admin_super
4. User sees "Access Denied" page (already partially implemented):

   ┌─────────────────────────────────────────────────────┐
   │                                                     │
   │           Access Denied                              │
   │           Signed in as buyer@test.com               │
   │                                                     │
   │   The Admin Dashboard requires administrator        │
   │   privileges. Contact your system administrator     │
   │   if you believe this is an error.                  │
   │                                                     │
   │   [ Switch Account ]   [ Sign Out ]                 │
   │                                                     │
   └─────────────────────────────────────────────────────┘
```

### Scenario 7: Cross-app navigation for multi-role users

A user who has both `buyer_active` and `seller_verified` sees cross-links:

**In buyer-web Navbar (user dropdown):**
```
   ┌──────────────────────────┐
   │ John Doe                 │
   │ john@company.com         │
   ├──────────────────────────┤
   │ My Bids                  │
   │ Watchlist                │
   │ My Purchases             │
   │ Profile                  │
   ├──────────────────────────┤
   │ → Seller Portal          │  ← only shown if user has seller_verified
   ├──────────────────────────┤
   │ Sign Out                 │
   └──────────────────────────┘
```

**In seller-portal TopBar (profile dropdown):**
```
   ┌──────────────────────────┐
   │ John Doe                 │
   │ Company Name             │
   ├──────────────────────────┤
   │ Settings                 │
   │ → Buyer Marketplace      │  ← only shown if user has buyer_active
   ├──────────────────────────┤
   │ Sign out                 │
   └──────────────────────────┘
```

**In admin-dashboard Sidebar:**
```
   ├──────────────────────────┤
   │ → Buyer Marketplace      │  ← always shown (admins often test)
   │ → Seller Portal          │  ← always shown
   ├──────────────────────────┤
```

Clicking these links simply navigates to the other app's URL. SSO handles the rest —
no re-login, no token exchange. The browser navigates to e.g. `localhost:5174` and
Keycloak's SSO session seamlessly authenticates the user.

---

## Implementation Phases

### Phase 1: Frontend Role Gates (Core Fix)

Add role validation after Keycloak authentication in each frontend. This is the minimum
change that solves the problem.

**Role requirements per app:**

| App              | Allowed Roles (any of)                          |
|------------------|------------------------------------------------|
| buyer-web        | `buyer_active`, `buyer_pending_kyc`            |
| seller-portal    | `seller_verified`, `seller_pending`            |
| admin-dashboard  | `admin_ops`, `admin_super`                     |

#### 1A. buyer-web — `plugins/keycloak.client.ts`

buyer-web is unique: it allows anonymous browsing. The role gate only triggers when the
user clicks "Login" and Keycloak successfully authenticates them.

**Current flow:**
```
keycloak.init() → if authenticated → setupAuthSession() → app continues
```

**New flow:**
```
keycloak.init() → if authenticated → check buyer role →
  if has buyer role → setupAuthSession() → app continues
  if missing buyer role → set wrongRoleState in auth store → DON'T setup session
```

Changes:
- `plugins/keycloak.client.ts`: After line 154 (`if (authenticated)`), before calling
  `setupAuthSession()`, check `keycloak.tokenParsed.realm_access.roles` for buyer roles.
  If missing, set a `wrongRole` state on the auth store with the user's email and their
  actual roles, then skip `setupAuthSession()`.
- `stores/auth.ts`: Add `wrongRole` ref (`{ email, roles, name } | null`) and
  `clearWrongRole()` action.
- `composables/useAuth.ts`: Add `switchAccount()` function that calls
  `keycloak.login({ prompt: 'login' })` to force the login page.
  Add `wrongRole` computed from auth store.

New file:
- `pages/auth/wrong-account.vue`: Full-page "Wrong Account" screen. Shows the user's
  email, which role they have, what role is required, and provides buttons:
  - "Switch Account" → calls `useAuth().switchAccount()`
  - "Register as Buyer" → calls `useAuth().register()`
  - Link to the correct portal based on their actual role

The `auth` middleware and the callback page should redirect to `/auth/wrong-account`
when `wrongRole` is set.

#### 1B. seller-portal — `src/main.ts`

seller-portal uses `onLoad: 'login-required'`, so the role check happens in `main.ts`
immediately after `keycloak.init()` succeeds — same pattern as the existing admin role
check in admin-dashboard.

Changes to `src/main.ts` (after line 20, after `keycloak.init()` succeeds):
```typescript
const realmRoles: string[] = keycloak.tokenParsed?.realm_access?.roles ?? []
const hasSellerRole = realmRoles.includes('seller_verified') || realmRoles.includes('seller_pending')
if (!hasSellerRole) {
  // Render "Wrong Account" page with Switch Account + portal links
  // Include user's email and their actual roles in the HTML
  document.body.innerHTML = `...wrong account page HTML...`
  throw new Error('Access denied: user does not have seller role')
}
```

The "Wrong Account" HTML should include:
- User's email (from `keycloak.tokenParsed.email`)
- A "Switch Account" button that redirects to Keycloak login with `prompt=login`
- A "Register as Seller" link (or "Apply to Become Seller")
- A link to buyer-web if the user has `buyer_active` role

The "Switch Account" button URL:
```
{keycloak.authServerUrl}/realms/{realm}/protocol/openid-connect/auth
  ?client_id=seller-portal
  &redirect_uri={window.location.origin}
  &response_type=code
  &scope=openid
  &prompt=login
```

#### 1C. admin-dashboard — `src/main.ts`

Admin-dashboard already has a role gate (lines 34-47). It just needs the "Switch Account"
button added to the existing "Access Denied" page.

Changes to `src/main.ts` (line 38-46, the `!hasAdminRole` block):
- Add a "Switch Account" button alongside the existing "Sign out & return" button
- The "Switch Account" button URL uses `prompt=login` to force the Keycloak login page

---

### Phase 2: Cross-App Navigation Links

Add portal-switching links in each app's navigation for multi-role users. These are
simple `<a>` tags that navigate to the other app's URL — SSO handles authentication
automatically.

#### 2A. buyer-web Navbar — `components/shared/Navbar.vue`

In the user dropdown menu (after "Profile", before the divider/logout), conditionally
show cross-links:

```vue
<!-- Cross-app links for multi-role users -->
<template v-if="hasRole('seller_verified') || hasRole('seller_pending')">
  <div class="border-t my-1" />
  <a href="http://localhost:5174" class="...">
    <ExternalLinkIcon />
    Seller Portal
  </a>
</template>
<template v-if="hasRole('admin_ops') || hasRole('admin_super')">
  <a href="http://localhost:5175" class="...">
    <ExternalLinkIcon />
    Admin Dashboard
  </a>
</template>
```

The URLs should come from runtime config (env vars) so they work in production:
- `NUXT_PUBLIC_SELLER_PORTAL_URL` defaulting to `http://localhost:5174`
- `NUXT_PUBLIC_ADMIN_DASHBOARD_URL` defaulting to `http://localhost:5175`

Changes:
- `nuxt.config.ts`: Add `sellerPortalUrl` and `adminDashboardUrl` to `runtimeConfig.public`
- `components/shared/Navbar.vue`: Add conditional cross-links in the user dropdown

#### 2B. seller-portal TopBar — `components/layout/TopBar.vue`

In the profile dropdown (before "Sign out"), conditionally show:

```vue
<router-link v-if="hasRole('buyer_active')" ...>
  → Buyer Marketplace
</router-link>
<router-link v-if="hasRole('admin_ops') || hasRole('admin_super')" ...>
  → Admin Dashboard
</router-link>
```

Changes:
- `src/components/layout/TopBar.vue`: Add cross-links in profile dropdown
- Use `import.meta.env.VITE_BUYER_WEB_URL || 'http://localhost:3000'` for URLs

#### 2C. admin-dashboard Sidebar

Add cross-links at the bottom of the sidebar navigation (always visible for admins):

```vue
<div class="border-t mt-auto pt-2">
  <a href="..." class="sidebar-link">→ Buyer Marketplace</a>
  <a href="..." class="sidebar-link">→ Seller Portal</a>
</div>
```

Changes:
- Sidebar component: Add cross-links section at the bottom

---

### Phase 3: Per-Client Keycloak Login Themes (Optional Enhancement)

Create custom Keycloak login themes so each app shows a branded login page. This helps
users understand which application they're authenticating for.

| Client           | Login Page Title                    |
|------------------|-------------------------------------|
| buyer-web        | "Sign in to EU Auction Marketplace" |
| seller-portal    | "Seller Portal — Sign In"           |
| admin-dashboard  | "Admin Console — Sign In"           |

This requires:
- Creating theme directories under `infrastructure/config/keycloak/themes/`
- Each theme overrides `messages/messages_en.properties` with custom `loginTitleHtml`
- Mounting themes into the Keycloak Docker container
- Setting per-client login theme in the realm JSON

**This phase is optional for the initial fix.** The role gate (Phase 1) is sufficient
to solve the problem. Login themes are a UX polish.

---

## File Change Summary

### Phase 1 (Core Fix) — 7 files modified, 1 new file

| File | Change |
|------|--------|
| `frontend/buyer-web/plugins/keycloak.client.ts` | Add role check after authentication |
| `frontend/buyer-web/stores/auth.ts` | Add `wrongRole` state |
| `frontend/buyer-web/composables/useAuth.ts` | Add `switchAccount()`, `wrongRole` computed |
| `frontend/buyer-web/pages/auth/wrong-account.vue` | **NEW** — wrong account page |
| `frontend/buyer-web/pages/auth/callback.vue` | Redirect to wrong-account if wrongRole set |
| `frontend/seller-portal/src/main.ts` | Add seller role check after keycloak.init() |
| `frontend/admin-dashboard/src/main.ts` | Add "Switch Account" button to access denied page |

### Phase 2 (Cross-App Links) — 4 files modified

| File | Change |
|------|--------|
| `frontend/buyer-web/nuxt.config.ts` | Add portal URL env vars |
| `frontend/buyer-web/components/shared/Navbar.vue` | Add cross-links in user dropdown |
| `frontend/seller-portal/src/components/layout/TopBar.vue` | Add cross-links in profile dropdown |
| `frontend/admin-dashboard/src/components/layout/SidebarNav.vue` | Add cross-links |

### Phase 3 (Login Themes) — 3 new directories + realm JSON update

| File | Change |
|------|--------|
| `infrastructure/config/keycloak/themes/buyer-theme/` | **NEW** directory |
| `infrastructure/config/keycloak/themes/seller-theme/` | **NEW** directory |
| `infrastructure/config/keycloak/themes/admin-theme/` | **NEW** directory |
| `infrastructure/config/keycloak/auction-platform-realm.json` | Add login theme per client |
| `docker/compose/infrastructure.yml` | Mount themes directory into Keycloak container |

---

## Key Technical Details

### The `prompt=login` Parameter

This is the core mechanism for "Switch Account". When added to the Keycloak authorization
request, it forces Keycloak to show the login page even if the user has an active SSO
session. The existing session is not destroyed — the user simply re-authenticates.

In keycloak-js:
```typescript
keycloak.login({ prompt: 'login' })
```

Or as a manual URL:
```
{keycloakUrl}/realms/{realm}/protocol/openid-connect/auth
  ?client_id={clientId}
  &redirect_uri={currentAppUrl}
  &response_type=code
  &scope=openid
  &prompt=login
```

### Role Check Logic

Each app needs to check `keycloak.tokenParsed.realm_access.roles` (an array of strings).
The check is a simple `Array.includes()` — no Keycloak extensions or server-side
configuration needed.

```typescript
const roles: string[] = keycloak.tokenParsed?.realm_access?.roles ?? []
const hasBuyerRole = roles.includes('buyer_active') || roles.includes('buyer_pending_kyc')
```

### Production URL Configuration

Cross-app links must use environment variables so they work across environments:

| Env Var | Dev Default | Production Example |
|---------|-------------|-------------------|
| `BUYER_WEB_URL` | `http://localhost:3000` | `https://auction.example.eu` |
| `SELLER_PORTAL_URL` | `http://localhost:5174` | `https://seller.auction.example.eu` |
| `ADMIN_DASHBOARD_URL` | `http://localhost:5175` | `https://admin.auction.example.eu` |

---

## What This Plan Does NOT Change

- **Keycloak realm structure** — stays as single realm `auction-platform` (correct)
- **SSO behavior** — SSO still works across apps (it's a feature for multi-role users)
- **Authentication flow** — still PKCE Authorization Code flow
- **Token structure** — no changes to protocol mappers or scopes
- **Backend services** — no changes needed (they validate tokens independently)
- **`fullScopeAllowed`** — left as `true` for now (could be tightened later as a
  defense-in-depth measure by using Role Scope Mappings per client)
