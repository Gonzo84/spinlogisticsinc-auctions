# SPC Auctions - Design & UX Audit

## Executive Summary

**Date:** 2026-02-28
**Scope:** buyer-web (Nuxt 3 SSR), seller-portal (Vue 3 SPA), admin-dashboard (Vue 3 SPA)
**Objective:** Comprehensive design and UX analysis with improvement roadmap aligned to PrimeVue best practices

---

## Critical Findings

### 1. PrimeVue is Severely Underutilized

| App | PrimeVue Installed? | Components Used | Assessment |
|-----|---------------------|-----------------|------------|
| buyer-web | NO | 0 | Not installed at all |
| seller-portal | YES (4.5.4) | 1 (ConfirmDialog only) | 99% unused |
| admin-dashboard | NO | 0 | Not installed at all |

**Impact:** All 3 apps are built with raw TailwindCSS utility classes and custom component classes. This means:
- No unified component library
- 300+ lines of custom `.btn`, `.card`, `.input`, `.badge` classes duplicated across apps
- Inconsistent component behavior (e.g., different focus rings, hover states, disabled styles)
- Missing enterprise-grade features: virtual scrolling, lazy DataTables, rich form validation, accessible dialogs

### 2. Inconsistent Design Token System

The 3 apps define **incompatible color palettes** in their Tailwind configs:

| Token | buyer-web | seller-portal | admin-dashboard |
|-------|-----------|---------------|-----------------|
| Primary blue-700 | `#1e40af` | `#1d4ed8` | `#1d4ed8` |
| Primary DEFAULT | `#1e40af` | - | - |
| Green semantic | `secondary` | `seller` | `success` |
| Amber semantic | `accent` | - | `warning` |
| Red semantic | `warning` | - | `danger` |
| Purple accent | - | - | `admin` |
| Full palette? | 50-900 + DEFAULT | 50-950 | 50-950 (sparse) |

**Impact:** Same concepts use different names, different shade values, and different completeness levels.

### 3. No Shared Design System

- Zero shared CSS, tokens, or component libraries between apps
- Each app reinvents `.btn`, `.card`, `.input`, `.badge`, `.table-wrapper` classes
- Subtle differences in padding, border-radius, focus styles, transition durations
- No dark mode support in any app

### 4. Visual & UX Issues Observed

From screenshot analysis of buyer-web (the only app renderable without Keycloak):

- **Mobile:** Hero title text clips ("Buy Industrial Equipment at Auct...") at 375px viewport
- **Country browser:** Horizontal scroll on mobile with a visible scrollbar (ugly)
- **Search page:** Skeleton loaders are well-implemented (good pattern)
- **Empty states:** Featured Auctions / Newly Listed sections show nothing when no data
- **Navigation:** No breadcrumbs, no "back to results" from lot detail
- **Authentication error pages:** seller-portal and admin-dashboard show raw inline HTML when Keycloak is unavailable

---

## Improvement Priority Matrix

| Priority | Area | Impact | Effort |
|----------|------|--------|--------|
| P0 | Create shared design token package | Foundation | Medium |
| P0 | Adopt PrimeVue/Volt across all 3 apps | Consistency + Features | High |
| P1 | Unified icon system (PrimeIcons/Lucide) | Consistency | Low |
| P1 | Form validation framework (@primevue/forms) | UX Quality | Medium |
| P1 | Dark mode support | Modern Standards | Medium |
| P2 | Accessibility overhaul (WCAG 2.1 AA) | Compliance | High |
| P2 | Responsive design fixes | Mobile UX | Medium |
| P3 | Micro-interactions & animations | Polish | Low |
| P3 | Skeleton loaders everywhere | Perceived Performance | Low |

---

## Recommended Architecture

```
frontend/
  shared/
    design-tokens/           # Shared Tailwind preset + PrimeVue theme
      tailwind.preset.ts     # Common colors, fonts, animations
      primevue-theme.ts      # definePreset(Aura, {...}) with custom tokens
      package.json
    ui-components/           # Shared Volt components (optional phase 2)
      Button.vue
      DataTable.vue
      ...
  buyer-web/                 # Consumes shared design tokens
  seller-portal/             # Consumes shared design tokens
  admin-dashboard/           # Consumes shared design tokens
```

---

## Documents in This Audit

| File | Description |
|------|-------------|
| [01-CURRENT-STATE-ANALYSIS.md](./01-CURRENT-STATE-ANALYSIS.md) | Detailed per-app analysis of current design implementation |
| [02-CROSS-PROJECT-INCONSISTENCIES.md](./02-CROSS-PROJECT-INCONSISTENCIES.md) | Side-by-side comparison of all inconsistencies |
| [03-PRIMEVUE-ADOPTION-STRATEGY.md](./03-PRIMEVUE-ADOPTION-STRATEGY.md) | PrimeVue 4 + Volt adoption plan with code examples |
| [04-DESIGN-SYSTEM-SPECIFICATION.md](./04-DESIGN-SYSTEM-SPECIFICATION.md) | Unified design token & component specification |
| [05-UX-IMPROVEMENTS.md](./05-UX-IMPROVEMENTS.md) | Per-page UX improvement recommendations |
| [06-ACCESSIBILITY-AUDIT.md](./06-ACCESSIBILITY-AUDIT.md) | WCAG 2.1 AA compliance gaps and fixes |
| [07-IMPLEMENTATION-ROADMAP.md](./07-IMPLEMENTATION-ROADMAP.md) | Phased implementation plan |
| [screenshots/](./screenshots/) | Visual captures of current state |
