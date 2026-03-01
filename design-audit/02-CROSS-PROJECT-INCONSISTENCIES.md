# Cross-Project Inconsistencies

## 1. Color System Divergence

### Primary Blue

| Shade | buyer-web | seller-portal | admin-dashboard |
|-------|-----------|---------------|-----------------|
| 50 | `#eff6ff` | `#eff6ff` | `#eff6ff` |
| 100 | `#dbeafe` | `#dbeafe` | `#dbeafe` |
| 200 | `#bfdbfe` | `#bfdbfe` | `#bfdbfe` |
| 300 | `#93c5fd` | `#93c5fd` | `#93c5fd` |
| 400 | `#60a5fa` | `#60a5fa` | `#60a5fa` |
| 500 | `#3b82f6` | `#3b82f6` | `#3b82f6` |
| 600 | `#2563eb` | `#2563eb` | `#2563eb` |
| **700** | **`#1e40af`** | **`#1d4ed8`** | **`#1d4ed8`** |
| 800 | `#1e3a8a` | `#1e40af` | `#1e40af` |
| 900 | `#1e3163` | `#1e3a8a` | `#1e3a8a` |
| 950 | - | `#172554` | `#172554` |
| DEFAULT | `#1e40af` | - | - |

**Issues:**
- buyer-web primary-700 is `#1e40af` while seller/admin use `#1d4ed8` (different blue)
- buyer-web primary-800 = seller/admin primary-700 (shifted by one shade)
- buyer-web primary-900 is `#1e3163` (unique color, not in Tailwind blue palette)
- buyer-web has DEFAULT, others don't
- buyer-web stops at 900, others go to 950

### Green/Success Tokens

| App | Token Name | 500 Value | Scale |
|-----|-----------|-----------|-------|
| buyer-web | `secondary` | `#10b981` (emerald) | Full 50-900 |
| seller-portal | `seller` | `#22c55e` (green) | Full 50-900 |
| admin-dashboard | `success` | `#22c55e` (green) | Sparse (50, 500, 700) |

**Issues:**
- Different token names: `secondary` vs `seller` vs `success`
- buyer-web uses emerald-500 while others use green-500
- admin-dashboard only defines 3 shades (can't use 100, 200, 300, etc.)

### Amber/Warning Tokens

| App | Token Name | DEFAULT/500 | Scale |
|-----|-----------|-------------|-------|
| buyer-web | `accent` | `#d97706` (amber-600) | Full 50-900 |
| seller-portal | - | Not defined | - |
| admin-dashboard | `warning` | `#f59e0b` (amber-500) | Sparse (50, 500, 700) |

**Issues:**
- seller-portal has NO amber/warning color defined
- buyer-web DEFAULT is amber-600, admin uses amber-500
- buyer-web calls it `accent`, admin calls it `warning`

### Red/Danger Tokens

| App | Token Name | DEFAULT/500 | Scale |
|-----|-----------|-------------|-------|
| buyer-web | `warning` | `#dc2626` (red-600) | Full 50-900 |
| seller-portal | - | Not defined (uses Tailwind `red-*`) | - |
| admin-dashboard | `danger` | `#ef4444` (red-500) | Sparse (50, 500, 700) |

**Issues:**
- buyer-web calls red `warning` (confusing: red = warning?)
- seller-portal has no custom red (falls back to Tailwind defaults)
- admin-dashboard calls it `danger`
- Different base shades: red-600 vs red-500

### App-Specific Colors

| App | Token | Values |
|-----|-------|--------|
| seller-portal | `seller` (green) | Full palette |
| admin-dashboard | `admin` (purple) | Full palette |
| buyer-web | `secondary` (green), `accent` (amber) | Full palettes |

---

## 2. Button Component Differences

### Base `.btn` Class

| Property | buyer-web | seller-portal | admin-dashboard |
|----------|-----------|---------------|-----------------|
| Definition | NONE (inline utilities) | @layer components | @layer components |
| Padding | Varies by component | `px-4 py-2.5` | `px-4 py-2` |
| Border radius | Varies | `rounded-lg` | `rounded-lg` |
| Gap | Varies | `gap-2` | Not defined |
| Transition | Varies | `transition-all duration-200` | `transition-colors duration-150` |
| Focus style | Varies | `focus-visible:ring-2 ring-offset-2` | `focus:ring-2 ring-offset-2` |
| Disabled | Varies | `disabled:cursor-not-allowed disabled:opacity-50` | `disabled:opacity-50 disabled:cursor-not-allowed` |

**Key Differences:**
- buyer-web has NO .btn class; each button is styled individually
- seller-portal uses `focus-visible` (better a11y) while admin uses `focus`
- seller-portal buttons are 2px taller (py-2.5 vs py-2)
- Transition properties differ (all vs colors) and durations (200ms vs 150ms)

### Button Variants

| Variant | buyer-web | seller-portal | admin-dashboard |
|---------|-----------|---------------|-----------------|
| `.btn-primary` | - | `bg-primary-600` | `bg-primary-600` |
| `.btn-secondary` | - | `border bg-white text-gray-700` | `bg-gray-100 text-gray-700 border` |
| `.btn-success` | - | `bg-seller-600` | `bg-green-600` |
| `.btn-danger` | - | `bg-red-600` | `bg-red-600` |
| `.btn-warning` | - | Not defined | `bg-amber-500` |
| `.btn-ghost` | - | `text-gray-600 hover:bg-gray-100` | Not defined |
| `.btn-lg` | - | `px-6 py-3 text-base` | Not defined |

**Key Differences:**
- `.btn-secondary` is white in seller-portal but gray-100 in admin (visual mismatch)
- `.btn-success` uses `seller-600` in one app and `green-600` in another
- `.btn-ghost` and `.btn-lg` only exist in seller-portal
- `.btn-warning` only exists in admin-dashboard

---

## 3. Card Component Differences

| Property | buyer-web | seller-portal | admin-dashboard |
|----------|-----------|---------------|-----------------|
| Definition | Inline utilities | `.card` class | `.card` class |
| Border radius | Varies (rounded-lg, rounded-xl) | `rounded-xl` | `rounded-xl` |
| Shadow | Varies | `shadow-sm` | `shadow-sm` |
| Border | Varies | `border-gray-200` | `border-gray-200` |
| Padding | Varies (p-4, p-6, p-8) | `p-6` | `p-6` |

---

## 4. Input Component Differences

| Property | seller-portal | admin-dashboard |
|----------|---------------|-----------------|
| Padding | `px-4 py-2.5` | `px-3 py-2` |
| Shadow | None | `shadow-sm` |
| Focus ring | `ring-2 ring-primary-500/20` | `ring-1 ring-primary-500` |
| Background | `bg-white` explicit | Not specified |
| Disabled bg | `bg-gray-100 text-gray-500` | Not specified |

**buyer-web** has no `.input` class; inputs are styled inline per-component.

---

## 5. Table Component Differences

| Property | seller-portal | admin-dashboard |
|----------|---------------|-----------------|
| Wrapper class | `.table-wrapper` | `.table-container` |
| Header bg | `bg-gray-50` (inline) | `bg-gray-50` (class) |
| Header text | Inline styling | `.table-header` with uppercase tracking |
| Cell class | Inline styling | `.table-cell` with whitespace-nowrap |
| Row hover | `hover:bg-gray-50` (inline) | `.table-row` with transition |
| Dividers | `divide-y divide-gray-100` | `border-t border-gray-100` |

---

## 6. Typography Inconsistencies

| Element | buyer-web | seller-portal | admin-dashboard |
|---------|-----------|---------------|-----------------|
| H1 | Not standardized | Not standardized | `text-2xl font-bold text-gray-900` |
| H2 | Not standardized | Not standardized | `text-xl font-semibold text-gray-800` |
| H3 | Not standardized | Not standardized | `text-lg font-semibold text-gray-800` |
| Body text colors | `text-gray-600`, `text-gray-700` | `text-gray-600`, `text-gray-500` | `text-gray-700`, `text-gray-800`, `text-gray-900` |
| Font family | Inter (Tailwind config only) | Inter (Tailwind + Google Fonts link) | Inter (Tailwind + Google Fonts link) |
| Mono font | JetBrains Mono | Not configured | Not configured |

**Issues:**
- buyer-web loads NO font (relies on system fallbacks)
- seller/admin load Inter via Google Fonts `<link>` but buyer-web doesn't
- Only admin-dashboard has base heading styles
- Text color hierarchy differs across apps

---

## 7. Layout Architecture Differences

| Aspect | buyer-web | seller-portal | admin-dashboard |
|--------|-----------|---------------|-----------------|
| Layout type | Navbar + Content + Footer | Sidebar + TopBar + Content | Sidebar + TopBar + Content |
| Sidebar | None | Collapsible (lg breakpoint) | Collapsible (lg breakpoint) |
| Mobile nav | Navbar (always visible) | Hamburger + slide-in sidebar | Hamburger + slide-in sidebar |
| Page transitions | None | Fade (150ms) | None |
| Scroll behavior | Not configured | Not configured | Not configured |

---

## 8. Icon System

**All 3 apps use inline SVGs** with no icon library. Each component embeds its own SVG icons.

**Impact:**
- 100+ inline SVG blocks scattered across the codebase
- No easy way to change icon style consistently
- Heroicons stroke-style used but not from the actual Heroicons package
- Inconsistent icon sizes: h-4, h-5, h-6, h-8 used interchangeably
- Some icons have `title` attributes, most don't (accessibility gap)

---

## 9. Loading/Error/Empty State Patterns

| Pattern | buyer-web | seller-portal | admin-dashboard |
|---------|-----------|---------------|-----------------|
| Loading spinner | `animate-spin` SVG | `animate-spin` SVG | `animate-spin` SVG |
| Skeleton loader | Yes (search page) | No | No |
| Error card | Varies | `border-red-200 bg-red-50` | Inline red text |
| Empty state | No content shown | Icon + message | Icon + message |
| Error retry | Some pages | Yes (retry button) | No |

---

## 10. Form Handling

| Aspect | buyer-web | seller-portal | admin-dashboard |
|--------|-----------|---------------|-----------------|
| Validation | Manual (bid amount check) | Manual (validate() function) | None |
| Library | None | None | None |
| Error display | Inline text | `<p v-if>` paragraphs | Varies |
| Confirm dialog | Browser confirm() | PrimeVue ConfirmDialog + window.confirm() | Custom ConfirmDialog |

---

## 11. PrimeVue Configuration

| Aspect | buyer-web | seller-portal | admin-dashboard |
|--------|-----------|---------------|-----------------|
| PrimeVue installed | No | Yes | No |
| Version | - | 4.5.4 | - |
| Theme | - | Aura (via @primeuix/themes) | - |
| Components used | - | ConfirmDialog only | - |
| Services | - | ConfirmationService | - |

---

## 12. Authentication Error UX

Both seller-portal and admin-dashboard display raw inline HTML when Keycloak fails:

```html
<!-- Identical pattern in both apps -->
<div style="display:flex;align-items:center;justify-content:center;height:100vh;font-family:sans-serif">
  <div style="text-align:center;max-width:400px">
    <h2 style="margin-bottom:8px">Authentication Unavailable</h2>
    <p style="color:#666">Unable to connect...</p>
    <button onclick="location.reload()" style="...">Retry</button>
  </div>
</div>
```

**Issues:**
- Bypasses all TailwindCSS styling
- Uses hardcoded inline styles
- Different from the app's design language
- No logo, no brand identity
- Not responsive (no media queries)
