# Unified Design System Specification

## 1. Color System

### Primary Palette (Blue)

Used for primary actions, links, active navigation, focus rings.

| Token | Value | Usage |
|-------|-------|-------|
| `primary-50` | `#eff6ff` | Selected/active backgrounds |
| `primary-100` | `#dbeafe` | Hover backgrounds, highlight |
| `primary-200` | `#bfdbfe` | Light borders |
| `primary-300` | `#93c5fd` | Disabled primary |
| `primary-400` | `#60a5fa` | Dark mode primary |
| `primary-500` | `#3b82f6` | Focus rings |
| `primary-600` | `#2563eb` | **Primary buttons, links** |
| `primary-700` | `#1d4ed8` | Hover primary |
| `primary-800` | `#1e40af` | Active/pressed primary |
| `primary-900` | `#1e3a8a` | Dark text on primary bg |
| `primary-950` | `#172554` | Darkest primary |

### Success Palette (Green)

Used for success states, positive KPI changes, "sold", "paid", "approved".

| Token | Value |
|-------|-------|
| `success-50` | `#f0fdf4` |
| `success-100` | `#dcfce7` |
| `success-200` | `#bbf7d0` |
| `success-300` | `#86efac` |
| `success-400` | `#4ade80` |
| `success-500` | `#22c55e` |
| `success-600` | `#16a34a` |
| `success-700` | `#15803d` |
| `success-800` | `#166534` |
| `success-900` | `#14532d` |

### Warning Palette (Amber)

Used for warnings, pending states, "processing", time-sensitive alerts.

| Token | Value |
|-------|-------|
| `warning-50` | `#fffbeb` |
| `warning-100` | `#fef3c7` |
| `warning-200` | `#fde68a` |
| `warning-300` | `#fcd34d` |
| `warning-400` | `#fbbf24` |
| `warning-500` | `#f59e0b` |
| `warning-600` | `#d97706` |
| `warning-700` | `#b45309` |
| `warning-800` | `#92400e` |
| `warning-900` | `#78350f` |

### Danger Palette (Red)

Used for errors, destructive actions, "unsold", "rejected", "fraud".

| Token | Value |
|-------|-------|
| `danger-50` | `#fef2f2` |
| `danger-100` | `#fee2e2` |
| `danger-200` | `#fecaca` |
| `danger-300` | `#fca5a5` |
| `danger-400` | `#f87171` |
| `danger-500` | `#ef4444` |
| `danger-600` | `#dc2626` |
| `danger-700` | `#b91c1c` |
| `danger-800` | `#991b1b` |
| `danger-900` | `#7f1d1d` |

### Surface / Neutral Palette

Mapped via PrimeVue's `surface` semantic tokens. Uses Slate (light mode), Zinc (dark mode).

| Token | Light | Dark | Usage |
|-------|-------|------|-------|
| `surface-0` | `#ffffff` | `#ffffff` | Page background |
| `surface-50` | `{slate.50}` | `{zinc.50}` | Subtle backgrounds (cards) |
| `surface-100` | `{slate.100}` | `{zinc.100}` | Table headers, hover |
| `surface-200` | `{slate.200}` | `{zinc.200}` | Borders, dividers |
| `surface-300` | `{slate.300}` | `{zinc.300}` | Disabled borders |
| `surface-400` | `{slate.400}` | `{zinc.400}` | Placeholder text |
| `surface-500` | `{slate.500}` | `{zinc.500}` | Muted text |
| `surface-600` | `{slate.600}` | `{zinc.600}` | Secondary text |
| `surface-700` | `{slate.700}` | `{zinc.700}` | Body text |
| `surface-800` | `{slate.800}` | `{zinc.800}` | Headings |
| `surface-900` | `{slate.900}` | `{zinc.900}` | High contrast text |
| `surface-950` | `{slate.950}` | `{zinc.950}` | Maximum contrast |

### App-Specific Accent Colors

Used sparingly for visual distinction between apps:

| App | Accent Color | Token | Usage |
|-----|-------------|-------|-------|
| buyer-web | Blue (primary) | `primary-*` | No separate accent needed |
| seller-portal | Green | `success-*` | Seller-specific KPIs, nav accent |
| admin-dashboard | Purple | `purple-*` | Admin-specific KPIs, nav accent |

---

## 2. Typography

### Font Stack

```css
--font-sans: 'Inter', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
--font-mono: 'JetBrains Mono', ui-monospace, 'Cascadia Code', 'Fira Code', monospace;
```

**Inter** must be loaded consistently across all apps via:
```html
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
```

### Type Scale

| Name | Size | Weight | Color | Usage |
|------|------|--------|-------|-------|
| **Display** | 36px (text-4xl) | 700 (bold) | surface-900 | Hero headings |
| **H1** | 30px (text-3xl) | 700 (bold) | surface-900 | Page titles |
| **H2** | 24px (text-2xl) | 700 (bold) | surface-900 | Section titles |
| **H3** | 20px (text-xl) | 600 (semibold) | surface-800 | Subsection titles |
| **H4** | 18px (text-lg) | 600 (semibold) | surface-800 | Card titles |
| **Body** | 14px (text-sm) | 400 (normal) | surface-700 | Default text |
| **Body Large** | 16px (text-base) | 400 (normal) | surface-700 | Emphasized body |
| **Caption** | 12px (text-xs) | 500 (medium) | surface-500 | Labels, timestamps |
| **Overline** | 12px (text-xs) | 600 (semibold) | surface-500 | Uppercase labels |
| **KPI Value** | 30px (text-3xl) | 700 (bold) | surface-900 | Dashboard numbers |
| **Currency** | 14px (text-sm) | 400 mono | surface-700 | Money values |

---

## 3. Spacing System

Use Tailwind's default 4px base spacing scale consistently:

| Token | Value | Usage |
|-------|-------|-------|
| `gap-1` / `p-1` | 4px | Tight spacing (icon gaps) |
| `gap-2` / `p-2` | 8px | Button icon-label gap |
| `gap-3` / `p-3` | 12px | Compact element spacing |
| `gap-4` / `p-4` | 16px | Standard element spacing |
| `gap-6` / `p-6` | 24px | **Card padding (standard)** |
| `gap-8` / `p-8` | 32px | Section spacing |

### Page Layout Spacing

| Area | Spacing |
|------|---------|
| Page padding | `p-6` (24px) on desktop, `p-4` (16px) on mobile |
| Section gap | `gap-6` (24px) between sections |
| Card padding | `p-6` (24px) always |
| Form field gap | `gap-4` (16px) between fields |
| Button gap | `gap-2` (8px) between button group items |
| Table cell padding | `px-4 py-3` |

---

## 4. Border Radius

| Token | Value | Usage |
|-------|-------|-------|
| `rounded-md` | 6px | Small elements (badges, chips) |
| `rounded-lg` | 8px | **Buttons, inputs, selects** |
| `rounded-xl` | 12px | **Cards, dialogs, modals** |
| `rounded-full` | 9999px | Avatars, circular badges |

---

## 5. Shadows

| Token | Value | Usage |
|-------|-------|-------|
| `shadow-sm` | `0 1px 2px rgba(0,0,0,0.05)` | Cards, inputs |
| `shadow-md` | `0 4px 6px -1px rgba(0,0,0,0.1)` | Hover cards, dropdowns |
| `shadow-lg` | `0 10px 15px -3px rgba(0,0,0,0.1)` | Modals, popovers |

---

## 6. Component Specifications

### Buttons

Use PrimeVue `<Button>` with standard severities:

| Variant | PrimeVue Prop | Visual |
|---------|--------------|--------|
| Primary | `(default)` | Blue bg, white text |
| Secondary | `severity="secondary"` | Gray bg, dark text |
| Success | `severity="success"` | Green bg, white text |
| Warning | `severity="warn"` | Amber bg, white text |
| Danger | `severity="danger"` | Red bg, white text |
| Ghost | `text` | Text only, hover bg |
| Outlined | `outlined` | Border, no fill |
| Small | `size="small"` | Reduced padding + text |
| Large | `size="large"` | Increased padding + text |

### Status Badges

Use PrimeVue `<Tag>` with standard severity mapping:

| Status | Severity | Visual |
|--------|----------|--------|
| active, approved, paid | `success` | Green |
| pending, processing, review | `warn` | Amber |
| draft, inactive | `secondary` | Gray |
| sold, completed | `info` | Blue |
| rejected, unsold, fraud | `danger` | Red |

### Form Fields

Use PrimeVue form components with `@primevue/forms` + Zod validation:

| Input Type | PrimeVue Component |
|-----------|-------------------|
| Text | `<InputText>` |
| Number/Currency | `<InputNumber mode="currency" currency="EUR">` |
| Select | `<Select>` |
| Multi-select | `<MultiSelect>` |
| Date | `<DatePicker>` |
| Textarea | `<Textarea>` |
| Checkbox | `<Checkbox>` |
| Toggle | `<ToggleSwitch>` |
| File upload | `<FileUpload>` |

### Data Tables

Use PrimeVue `<DataTable>` with:
- `lazy` for server-side pagination
- `dataKey="id"` always
- `filterDisplay="row"` for inline filters
- `responsiveLayout="scroll"` for mobile
- Column templates for formatted cells

### Navigation

| Area | Component | Notes |
|------|-----------|-------|
| buyer-web top nav | `<Menubar>` | Logo, search, language, auth |
| seller/admin sidebar | `<Drawer>` + `<Menu>` | Collapsible, responsive |
| seller/admin top bar | `<Toolbar>` | Search, notifications, profile |
| Breadcrumbs | `<Breadcrumb>` | All apps, all pages |
| Tab navigation | `<Tabs>` | Profile settings, lot filters |

### Overlays

| Pattern | Component | Notes |
|---------|-----------|-------|
| Confirmations | `<ConfirmDialog>` | Single instance at app root |
| Feedback | `<Toast>` | Single instance at app root |
| Modal forms | `<Dialog>` | Bid confirm, create forms |
| Contextual info | `<Popover>` | Lot preview, user info |
| Side panels | `<Drawer>` | Filters, cart, watchlist |

---

## 7. Icon System

**Adopt PrimeIcons** as the standard icon library across all 3 apps.

```bash
npm install primeicons
```

```css
/* Import in app entry */
@import 'primeicons/primeicons.css';
```

### Usage
```vue
<!-- In PrimeVue components -->
<Button icon="pi pi-check" label="Confirm" />

<!-- Standalone -->
<i class="pi pi-search text-surface-500" />
```

### Key Icons for Auction Platform

| Action | Icon |
|--------|------|
| Search | `pi pi-search` |
| Filter | `pi pi-filter` |
| Sort | `pi pi-sort-alt` |
| Bid | `pi pi-dollar` |
| Timer | `pi pi-clock` |
| Watchlist | `pi pi-heart` |
| Cart | `pi pi-shopping-cart` |
| User | `pi pi-user` |
| Settings | `pi pi-cog` |
| Notifications | `pi pi-bell` |
| Dashboard | `pi pi-chart-bar` |
| Lots | `pi pi-box` |
| Upload | `pi pi-upload` |
| Download | `pi pi-download` |
| Edit | `pi pi-pencil` |
| Delete | `pi pi-trash` |
| Approve | `pi pi-check-circle` |
| Reject | `pi pi-times-circle` |
| Warning | `pi pi-exclamation-triangle` |
| Info | `pi pi-info-circle` |

---

## 8. Responsive Breakpoints

Use Tailwind's default breakpoints (mobile-first):

| Breakpoint | Width | Layout Changes |
|-----------|-------|----------------|
| Default | < 640px | Single column, stacked navigation |
| `sm` | 640px | 2-column grids |
| `md` | 768px | Show search bars, 2-column forms |
| `lg` | 1024px | Sidebar visible, 3-4 column grids |
| `xl` | 1280px | Maximum content width |
| `2xl` | 1536px | Wider layouts |

### Layout Rules

- **buyer-web**: `max-w-7xl mx-auto` for all page content
- **seller-portal/admin**: Sidebar (w-64/w-20 collapsed) + fluid content area
- All grids: `grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4`
- Tables: `overflow-x-auto` wrapper, horizontal scroll on mobile

---

## 9. Dark Mode Specification

### Toggle Mechanism

Class-based via `.app-dark` on `<html>`:

```typescript
// composables/useTheme.ts (shared)
export function useTheme() {
  const isDark = ref(false)

  const toggle = () => {
    isDark.value = !isDark.value
    document.documentElement.classList.toggle('app-dark', isDark.value)
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
  }

  const init = () => {
    const stored = localStorage.getItem('theme')
    if (stored === 'dark') {
      isDark.value = true
      document.documentElement.classList.add('app-dark')
    } else if (!stored) {
      isDark.value = window.matchMedia('(prefers-color-scheme: dark)').matches
      document.documentElement.classList.toggle('app-dark', isDark.value)
    }
  }

  return { isDark, toggle, init }
}
```

### Tailwind Dark Mode

```typescript
// tailwind.config.ts (all apps)
export default {
  darkMode: 'selector', // Uses .app-dark selector
  // ...
} satisfies Config
```

Then use `dark:` prefix in templates:
```html
<div class="bg-white dark:bg-surface-900 text-surface-900 dark:text-surface-0">
```

---

## 10. Animation & Transitions

### Standard Transitions

| Element | Duration | Easing | Property |
|---------|----------|--------|----------|
| Buttons | 150ms | ease | colors |
| Cards (hover) | 200ms | ease | shadow |
| Sidebar collapse | 300ms | ease | width, transform |
| Dropdowns open | 200ms | ease-out | opacity, transform |
| Page transitions | 200ms | ease | opacity |
| Toast enter | 300ms | ease-out | transform, opacity |
| Toast exit | 200ms | ease-in | opacity |

### Auction-Specific Animations

| Animation | Duration | Usage |
|-----------|----------|-------|
| `pulse-fast` | 1s | Anti-sniping timer (< 2 min) |
| `count-up` | 2s | CO2 counter number |
| `slide-in` | 300ms | Overbid toast |
| `fade-in` | 200ms | Page content |
| Bid flash | 500ms | New bid highlight in table |
