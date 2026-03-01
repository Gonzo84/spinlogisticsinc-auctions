# PrimeVue Adoption Strategy

## Decision: Styled Mode + Tailwind CSS (NOT Volt)

### Why Styled Mode over Unstyled/Volt

| Criterion | Styled Mode | Volt (Unstyled + Tailwind) |
|-----------|-------------|---------------------------|
| Setup effort | Low (install + configure preset) | High (add each component individually) |
| Maintenance | PrimeVue handles component CSS | You maintain all component styles |
| Dark mode | Built-in via colorScheme tokens | Manual Tailwind dark: classes |
| Theme consistency | Guaranteed by token system | Depends on your Tailwind discipline |
| Accessibility | Built-in ARIA + keyboard nav | Inherited from PrimeVue core |
| Learning curve | Lower | Higher (need to understand PT system) |
| Bundle size | Slightly larger | Slightly smaller |

**Recommendation:** Styled Mode. The team is already using Tailwind for layout; PrimeVue's styled mode with `cssLayer` enabled allows Tailwind to coexist and override when needed. This approach gives the fastest path to a professional, consistent UI while Tailwind handles layout and spacing.

---

## Shared Theme Package Architecture

### Package Structure

```
frontend/
  shared/
    design-tokens/
      package.json
      index.ts                  # Main exports
      preset.ts                 # definePreset(Aura, {...})
      tokens/
        primitive.ts            # Brand color primitives
        semantic.ts             # Semantic token mappings (light + dark)
        components.ts           # Component-level token overrides
      tailwind.preset.ts        # Shared Tailwind preset (colors, fonts, etc.)
      pt.ts                     # Global Pass-Through defaults
```

### `package.json`

```json
{
  "name": "@auction-platform/design-tokens",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "main": "index.ts",
  "exports": {
    ".": "./index.ts",
    "./tailwind": "./tailwind.preset.ts"
  },
  "peerDependencies": {
    "@primeuix/themes": "^2.0.3",
    "primevue": "^4.5.4"
  }
}
```

### `preset.ts` - Unified PrimeVue Theme

```typescript
import { definePreset } from '@primeuix/themes'
import Aura from '@primeuix/themes/aura'

export const AuctionPlatformPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '{blue.50}',
      100: '{blue.100}',
      200: '{blue.200}',
      300: '{blue.300}',
      400: '{blue.400}',
      500: '{blue.500}',
      600: '{blue.600}',
      700: '{blue.700}',
      800: '{blue.800}',
      900: '{blue.900}',
      950: '{blue.950}',
    },
    colorScheme: {
      light: {
        primary: {
          color: '{primary.600}',
          contrastColor: '#ffffff',
          hoverColor: '{primary.700}',
          activeColor: '{primary.800}',
        },
        highlight: {
          background: '{primary.50}',
          focusBackground: '{primary.100}',
          color: '{primary.700}',
          focusColor: '{primary.800}',
        },
        surface: {
          0: '#ffffff',
          50: '{slate.50}',
          100: '{slate.100}',
          200: '{slate.200}',
          300: '{slate.300}',
          400: '{slate.400}',
          500: '{slate.500}',
          600: '{slate.600}',
          700: '{slate.700}',
          800: '{slate.800}',
          900: '{slate.900}',
          950: '{slate.950}',
        },
      },
      dark: {
        primary: {
          color: '{primary.400}',
          contrastColor: '{surface.900}',
          hoverColor: '{primary.300}',
          activeColor: '{primary.200}',
        },
        highlight: {
          background: 'color-mix(in srgb, {primary.400}, transparent 84%)',
          focusBackground: 'color-mix(in srgb, {primary.400}, transparent 76%)',
          color: 'rgba(255,255,255,0.87)',
          focusColor: 'rgba(255,255,255,0.87)',
        },
        surface: {
          0: '#ffffff',
          50: '{zinc.50}',
          100: '{zinc.100}',
          200: '{zinc.200}',
          300: '{zinc.300}',
          400: '{zinc.400}',
          500: '{zinc.500}',
          600: '{zinc.600}',
          700: '{zinc.700}',
          800: '{zinc.800}',
          900: '{zinc.900}',
          950: '{zinc.950}',
        },
      },
    },
  },
  components: {
    button: {
      borderRadius: '{border.radius.lg}',
    },
    card: {
      borderRadius: '{border.radius.xl}',
      shadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1)',
    },
    datatable: {
      headerCell: {
        borderColor: '{surface.200}',
      },
    },
    dialog: {
      borderRadius: '{border.radius.xl}',
    },
    inputtext: {
      borderRadius: '{border.radius.lg}',
    },
    select: {
      borderRadius: '{border.radius.lg}',
    },
    tag: {
      borderRadius: '{border.radius.xl}',
    },
  },
})

export const themeConfig = {
  preset: AuctionPlatformPreset,
  options: {
    prefix: 'p',
    darkModeSelector: '.app-dark',
    cssLayer: {
      name: 'primevue',
      order: 'theme, base, primevue',
    },
  },
}
```

### `tailwind.preset.ts` - Shared Tailwind Preset

```typescript
import type { Config } from 'tailwindcss'

export default {
  theme: {
    extend: {
      colors: {
        // Semantic colors (matches PrimeVue tokens for consistency)
        brand: {
          50: '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          300: '#93c5fd',
          400: '#60a5fa',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
          800: '#1e40af',
          900: '#1e3a8a',
          950: '#172554',
        },
        success: {
          50: '#f0fdf4',
          100: '#dcfce7',
          200: '#bbf7d0',
          300: '#86efac',
          400: '#4ade80',
          500: '#22c55e',
          600: '#16a34a',
          700: '#15803d',
          800: '#166534',
          900: '#14532d',
        },
        warning: {
          50: '#fffbeb',
          100: '#fef3c7',
          200: '#fde68a',
          300: '#fcd34d',
          400: '#fbbf24',
          500: '#f59e0b',
          600: '#d97706',
          700: '#b45309',
          800: '#92400e',
          900: '#78350f',
        },
        danger: {
          50: '#fef2f2',
          100: '#fee2e2',
          200: '#fecaca',
          300: '#fca5a5',
          400: '#f87171',
          500: '#ef4444',
          600: '#dc2626',
          700: '#b91c1c',
          800: '#991b1b',
          900: '#7f1d1d',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
        mono: ['JetBrains Mono', 'ui-monospace', 'monospace'],
      },
      animation: {
        'pulse-fast': 'pulse 1s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'count-up': 'countUp 2s ease-out forwards',
        'slide-in': 'slideIn 0.3s ease-out forwards',
        'fade-in': 'fadeIn 0.2s ease-out forwards',
      },
      keyframes: {
        countUp: {
          '0%': { opacity: '0', transform: 'translateY(10px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        slideIn: {
          '0%': { opacity: '0', transform: 'translateX(-10px)' },
          '100%': { opacity: '1', transform: 'translateX(0)' },
        },
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
      },
    },
  },
} satisfies Partial<Config>
```

---

## Per-App Integration

### buyer-web (Nuxt 3)

```bash
npm install primevue @primeuix/themes @primevue/nuxt-module primeicons
```

```typescript
// nuxt.config.ts
import { themeConfig } from '@auction-platform/design-tokens'

export default defineNuxtConfig({
  modules: ['@primevue/nuxt-module', '@nuxtjs/tailwindcss', /* existing modules */],
  primevue: {
    options: { theme: themeConfig },
    autoImport: true,
  },
  css: ['primeicons/primeicons.css'],
})
```

### seller-portal (Vue 3 SPA)

```typescript
// main.ts
import PrimeVue from 'primevue/config'
import ConfirmationService from 'primevue/confirmationservice'
import ToastService from 'primevue/toastservice'
import { themeConfig } from '@auction-platform/design-tokens'
import 'primeicons/primeicons.css'

app.use(PrimeVue, { theme: themeConfig })
app.use(ConfirmationService)
app.use(ToastService)
```

### admin-dashboard (Vue 3 SPA)

```typescript
// main.ts
import PrimeVue from 'primevue/config'
import ConfirmationService from 'primevue/confirmationservice'
import ToastService from 'primevue/toastservice'
import { themeConfig } from '@auction-platform/design-tokens'
import 'primeicons/primeicons.css'

app.use(PrimeVue, { theme: themeConfig })
app.use(ConfirmationService)
app.use(ToastService)
```

---

## Component Migration Map

### Phase 1: Replace Custom Components with PrimeVue (High Impact)

| Current Custom | Replace With | Priority | Notes |
|----------------|-------------|----------|-------|
| Custom `.btn` classes | `<Button>` | P0 | Label, icon, severity, outlined, text, rounded |
| Custom `.input` class | `<InputText>`, `<InputNumber>`, `<Textarea>` | P0 | Built-in validation support |
| Custom `.card` class | `<Card>` | P0 | Header, content, footer slots |
| Custom DataTable (admin) | PrimeVue `<DataTable>` | P0 | Lazy, virtual scroll, sort, filter, export |
| Custom ConfirmDialog (admin) | PrimeVue `<ConfirmDialog>` | P0 | Focus trap, keyboard nav built-in |
| Custom StatusBadge | PrimeVue `<Tag>` + `<Badge>` | P1 | Severity prop maps to colors |
| Manual form validation | `@primevue/forms` + Zod | P1 | Schema-based validation |
| Inline SVG icons | PrimeIcons (`pi pi-*`) | P1 | 200+ consistent icons |
| Custom `.select` class | PrimeVue `<Select>` | P1 | Keyboard nav, filtering |
| Custom sidebar | PrimeVue `<Drawer>` + `<Menu>` | P2 | Responsive, a11y built-in |
| Custom TopBar | PrimeVue `<Menubar>` | P2 | Breakpoint collapse, slots |

### Phase 2: New PrimeVue Features (Medium Impact)

| Feature | PrimeVue Component | Benefit |
|---------|-------------------|---------|
| Breadcrumbs | `<Breadcrumb>` | Navigation context |
| Toast notifications | `<Toast>` | Consistent feedback |
| Skeleton loading | `<Skeleton>` | Perceived performance |
| Progress indicators | `<ProgressBar>`, `<ProgressSpinner>` | Loading states |
| Tab views | `<Tabs>` | Replace manual tab switching |
| Image gallery | `<Galleria>` | Lot image viewer |
| File upload | `<FileUpload>` | Replace custom ImageUploader |
| Timeline | `<Timeline>` | Activity feeds |
| Chip | `<Chip>` | Tag/filter display |
| Avatar | `<Avatar>` | User profile images |
| Dark mode toggle | `<ToggleSwitch>` | Theme switching |

### Phase 3: Advanced Features (Enhancement)

| Feature | PrimeVue Component | Benefit |
|---------|-------------------|---------|
| Virtual scrolling | `<DataTable virtualScrollerOptions>` | 10,000+ row performance |
| Column resize | `<Column resizable>` | User-controlled layouts |
| Data export | `<DataTable exportFunction>` | CSV/PDF export |
| Tree table | `<TreeTable>` | Category hierarchies |
| Organization chart | `<OrganizationChart>` | Company structures |
| Rich text editor | `<Editor>` | Lot descriptions |
| Date/time picker | `<DatePicker>` | Auction scheduling |
| Stepper wizard | `<Stepper>` | Multi-step lot creation |

---

## Coexistence Strategy: PrimeVue + Existing Tailwind

With `cssLayer` enabled, PrimeVue styles sit in a named CSS layer. Your Tailwind utilities automatically have higher specificity because they're in the default (unnamed) layer.

```css
/* PrimeVue styles (lower priority - in @layer primevue) */
.p-button { background: var(--p-button-background); }

/* Your Tailwind classes (higher priority - no layer) */
.bg-brand-600 { background-color: #2563eb; }
```

This means you can:
1. Use PrimeVue components as-is for standard patterns
2. Override specific styles with Tailwind classes when needed
3. Gradually migrate from custom `.btn`/`.card` classes to PrimeVue components
4. Keep existing Tailwind layout utilities (grid, flex, spacing)

### Example: Gradual Button Migration

```vue
<!-- Before: Custom Tailwind button -->
<button class="btn-primary">Submit</button>

<!-- After: PrimeVue Button (same visual, better a11y + features) -->
<Button label="Submit" />

<!-- Hybrid: PrimeVue Button with Tailwind override -->
<Button label="Submit" class="w-full mt-4" />
```
