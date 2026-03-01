# Current State Analysis

## 1. buyer-web (Nuxt 3 SSR)

### Stack
- **Framework:** Nuxt 3.14 with hybrid SSR rendering
- **Styling:** TailwindCSS 3.4 (via @nuxtjs/tailwindcss module)
- **UI Library:** NONE - all custom components
- **State:** Pinia (4 stores: auth, auction, cart, notifications)
- **Auth:** Keycloak OIDC PKCE (client-only plugin)
- **i18n:** @nuxtjs/i18n (7 languages: en, nl, de, fr, pl, it, ro)
- **Charts:** Chart.js 4.4 + vue-chartjs 5.3
- **Icons:** Inline SVGs (Heroicons-style, no icon library)
- **Fonts:** Inter (configured in Tailwind, NOT loaded via Google Fonts or @font-face)

### Component Inventory (13 components)
| Category | Components |
|----------|-----------|
| **Auction** | AuctionTimer, BidPanel, LotCard |
| **Home** | HeroCarousel, CategoryGrid, CO2Counter, HowItWorks |
| **Search** | SearchBar, FilterSidebar |
| **Notifications** | OverbidToast |
| **Shared** | Navbar, Footer, CO2Badge |

### Pages (10 routes)
| Route | Rendering | Description |
|-------|-----------|-------------|
| `/` | Pre-rendered | Hero + carousel + categories + CO2 counter |
| `/search` | SWR 5min | Filter sidebar + lot grid with skeletons |
| `/lots/[id]` | Client-only | Lot detail + BidPanel + timer |
| `/checkout/[id]` | Client-only | Payment flow |
| `/auth/callback` | Client-only | OIDC callback |
| `/auth/register` | Client-only | Registration |
| `/my/bids` | Client-only | User bid history |
| `/my/watchlist` | Client-only | Saved lots |
| `/my/purchases` | Client-only | Won lots |
| `/profile` | Client-only | User profile |

### Design Tokens (Tailwind)
- **Primary:** Blue (#1e40af DEFAULT, 50-900 scale)
- **Secondary:** Green (#059669 DEFAULT, 50-900 scale)
- **Accent:** Amber (#d97706 DEFAULT, 50-900 scale)
- **Warning:** Red (#dc2626 DEFAULT, 50-900 scale)
- **Font:** Inter, system-ui, -apple-system, sans-serif
- **Mono:** JetBrains Mono, monospace

### Custom CSS Classes
- NONE - buyer-web uses no @layer components CSS
- All styling is inline Tailwind utility classes in templates
- No `.btn`, `.card`, `.input` abstractions

### Strengths
- Good skeleton loaders on search page
- Proper SSR route rules (prerender home, SWR search, client-only auth pages)
- i18n support for 7 languages
- WebSocket-driven real-time bid updates
- Anti-sniping timer with color zones
- Keyboard navigation in SearchBar (arrow keys, Enter, Escape)
- Proper ARIA attributes on dropdowns

### Weaknesses
- No UI component library at all
- Hero title clips on mobile
- No dark mode
- No form validation framework
- Missing breadcrumb navigation
- Empty states show nothing (no illustration, no CTA)
- Font not actually loaded (configured in Tailwind but no import/link)
- No skip-to-content link
- OverbidToast not accessible (no aria-live)

---

## 2. seller-portal (Vue 3 SPA)

### Stack
- **Framework:** Vue 3.5 + Vue Router 4.4 + Vite 6.0
- **Styling:** TailwindCSS 3.4 + custom @layer components
- **UI Library:** PrimeVue 4.5.4 (only ConfirmDialog used)
- **Theme:** @primeuix/themes 2.0.3 (Aura preset configured but barely used)
- **State:** Pinia installed but NOT used (composable refs instead)
- **Auth:** Keycloak OIDC PKCE (top-level await in main.ts)
- **Charts:** Chart.js 4.4 + vue-chartjs 5.3
- **Icons:** Inline SVGs (Heroicons-style, no icon library)
- **Fonts:** Inter (loaded via Google Fonts `<link>` in index.html)

### Component Inventory (5 components)
| Category | Components |
|----------|-----------|
| **Layout** | SidebarNav, TopBar |
| **Charts** | RevenueChart, SellThroughChart |
| **Lots** | ImageUploader, LotForm |

### Views (9 routes)
| Route | Description |
|-------|-------------|
| `/` | Dashboard: KPI cards, revenue chart, activity feed |
| `/lots` | Lot list with tabs, filters, bulk actions |
| `/lots/create` | Create lot form |
| `/lots/:id` | Lot detail with bid history |
| `/lots/:id/edit` | Edit lot form |
| `/settlements` | Payment/settlement table |
| `/analytics` | Multi-chart analytics |
| `/co2-report` | CO2 impact report |
| `/profile` | 3-tab profile settings |

### Design Tokens (Tailwind)
- **Primary:** Blue (#3b82f6 at 500, scale 50-950)
- **Seller:** Green (#22c55e at 500, scale 50-900)
- **Font:** Inter, system-ui, -apple-system, sans-serif

### Custom CSS Classes (main.css - 171 lines)
Defines `@layer components` with:
- `.btn`, `.btn-primary`, `.btn-secondary`, `.btn-success`, `.btn-danger`, `.btn-ghost`, `.btn-sm`, `.btn-lg`
- `.card`, `.card-hover`
- `.input`, `.label`
- `.badge`, `.badge-draft`, `.badge-pending`, `.badge-active`, `.badge-sold`, `.badge-unsold`, `.badge-paid`, `.badge-processing`
- `.table-wrapper`
- `.kpi-card`, `.kpi-value`, `.kpi-label`, `.kpi-change-up`, `.kpi-change-down`
- `.sidebar-link`, `.sidebar-link-active`
- `.scrollbar-thin` utility

### Strengths
- Well-organized @layer components
- Consistent loading/error/empty states
- Accessible focus-visible rings
- Smooth sidebar collapse transition
- KPI card pattern is well-designed
- Image upload with drag-drop, progress, reordering

### Weaknesses
- PrimeVue installed but only ConfirmDialog used (wasted dependency)
- Pinia installed but unused
- Mixed confirm patterns: PrimeVue ConfirmDialog AND window.confirm()
- No form validation library (manual validation)
- No dark mode
- Notification dropdown may overflow on mobile
- Color palette limited (no warning/danger tokens)
- 60+ inline SVG icons without any icon library

---

## 3. admin-dashboard (Vue 3 SPA)

### Stack
- **Framework:** Vue 3.5 + Vue Router 4.4 + Vite 6.0
- **Styling:** TailwindCSS 3.4 + custom @layer components
- **UI Library:** NONE
- **State:** Pinia installed but NOT used (composable refs instead)
- **Auth:** Keycloak OIDC PKCE + role check (admin_ops/admin_super)
- **Charts:** Chart.js 4.4 + vue-chartjs 5.3
- **Icons:** Inline SVGs (Heroicons-style, no icon library)
- **Fonts:** Inter (loaded via Google Fonts `<link>` in index.html)

### Component Inventory (7 components)
| Category | Components |
|----------|-----------|
| **Layout** | AdminSidebar, AdminTopBar |
| **Common** | StatusBadge, DataTable, ConfirmDialog |
| **Charts** | LiveBidChart, RevenueChart |

### Views (12 routes)
| Route | Description |
|-------|-------------|
| `/` | Dashboard: KPI cards, live bid chart, alerts |
| `/auctions` | Auction management list |
| `/auctions/create` | Create auction form |
| `/auctions/:id` | Auction detail + lot management |
| `/lots/approval` | Lot approval queue |
| `/users` | User management table |
| `/users/:id` | User detail (KYC, bids, payments) |
| `/payments` | Payment oversight |
| `/fraud` | Fraud detection alerts |
| `/gdpr` | GDPR request management |
| `/analytics` | Platform analytics charts |
| `/system` | System health monitoring |

### Design Tokens (Tailwind)
- **Primary:** Blue (#3b82f6 at 500, scale 50-950)
- **Admin:** Purple (#a855f7 at 500, scale 50-900)
- **Success:** Green (sparse: 50, 500, 700 only)
- **Warning:** Amber (sparse: 50, 500, 700 only)
- **Danger:** Red (sparse: 50, 500, 700 only)
- **Font:** Inter, system-ui, -apple-system, sans-serif

### Custom CSS Classes (main.css - 95 lines)
Defines `@layer components` with:
- `.btn`, `.btn-primary`, `.btn-secondary`, `.btn-danger`, `.btn-success`, `.btn-warning`, `.btn-sm`
- `.card`
- `.input`, `.label`, `.select`
- `.table-container`, `.table-header`, `.table-cell`, `.table-row`
- `.page-header`, `.page-title`, `.section-title`

### Strengths
- StatusBadge component handles 40+ status types (reusable)
- Custom DataTable with sort, filter, pagination
- ConfirmDialog with variant styling (danger/warning/info)
- Proper role-based access control at app init
- Clean composable patterns with readonly returns

### Weaknesses
- No PrimeVue at all (builds everything from scratch)
- Pinia installed but unused
- StatusBadge has 60+ hardcoded color mappings (not scalable)
- Custom DataTable lacks virtual scrolling, column resize, export
- Custom ConfirmDialog lacks focus trap
- Inconsistent pagination (DataTable vs views)
- No form validation library
- No dark mode
- Accessibility gaps: no aria-label, no keyboard navigation, no focus management in modals
- Uses template literals for router links instead of named routes
