# Implementation Roadmap

## Phase 0: Foundation (1-2 days)

### 0.1 Create Shared Design Token Package

**Files to create:**
```
frontend/shared/design-tokens/
  package.json
  index.ts
  preset.ts
  tailwind.preset.ts
  pt.ts
```

**Steps:**
1. Create `frontend/shared/design-tokens/` directory
2. Write `preset.ts` with `definePreset(Aura, {...})` using unified color tokens
3. Write `tailwind.preset.ts` with unified colors, fonts, animations
4. Write `pt.ts` with global Pass-Through defaults
5. Write `index.ts` exporting `themeConfig`, Tailwind preset, and PT config
6. Add workspace reference in each app's `package.json`

### 0.2 Install PrimeVue Across All Apps

**buyer-web:**
```bash
cd frontend/buyer-web
npm install primevue @primeuix/themes @primevue/nuxt-module primeicons
```
Update `nuxt.config.ts` to add `@primevue/nuxt-module` with `themeConfig`.

**seller-portal:**
```bash
cd frontend/seller-portal
# Already has primevue & @primeuix/themes - just add primeicons
npm install primeicons
```
Update `main.ts` to use shared `themeConfig` instead of raw Aura import.

**admin-dashboard:**
```bash
cd frontend/admin-dashboard
npm install primevue @primeuix/themes primeicons
```
Update `main.ts` to add PrimeVue with shared `themeConfig`.

### 0.3 Unify Tailwind Configuration

**All 3 apps:**
- Replace custom color definitions with import from shared preset
- Add Google Fonts `<link>` to buyer-web (seller-portal and admin already have it)
- Set `darkMode: 'selector'` in all Tailwind configs

### 0.4 Add PrimeIcons

Replace inline SVG icon usage with PrimeIcons class. This can be done incrementally but the import should be added now.

**Deliverables:**
- [ ] Shared design token package created
- [ ] PrimeVue installed and configured in all 3 apps
- [ ] Tailwind configs unified
- [ ] PrimeIcons available in all apps
- [ ] All apps render correctly with new theme

---

## Phase 1: Core Component Migration (3-5 days)

### 1.1 Buttons (All Apps)

Replace custom `.btn-*` classes with PrimeVue `<Button>`:

```vue
<!-- Before -->
<button class="btn-primary">Save</button>

<!-- After -->
<Button label="Save" />
```

**Scope:**
- buyer-web: ~30 button instances (inline Tailwind)
- seller-portal: ~40 button instances (`.btn-*` classes)
- admin-dashboard: ~50 button instances (`.btn-*` classes)

### 1.2 Form Inputs (All Apps)

Replace custom `.input`, `.label`, `.select` classes with PrimeVue components:

| Before | After |
|--------|-------|
| `<input class="input">` | `<InputText>` |
| `<input type="number">` | `<InputNumber>` |
| `<select class="select">` | `<Select>` |
| `<textarea>` | `<Textarea>` |
| `<label class="label">` | `<label>` (Tailwind styled) |

### 1.3 Cards (All Apps)

Replace custom `.card` class with PrimeVue `<Card>`:

```vue
<!-- Before -->
<div class="card">
  <h3>Title</h3>
  <p>Content</p>
</div>

<!-- After -->
<Card>
  <template #title>Title</template>
  <template #content><p>Content</p></template>
</Card>
```

### 1.4 Data Tables (admin-dashboard, seller-portal)

Replace custom `DataTable.vue` and inline `<table>` with PrimeVue `<DataTable>`:

```vue
<DataTable
  :value="items"
  :lazy="true"
  :paginator="true"
  :rows="25"
  :totalRecords="totalRecords"
  :loading="loading"
  dataKey="id"
  filterDisplay="row"
  @page="onPage"
  @sort="onSort"
  @filter="onFilter"
>
  <Column field="id" header="ID" sortable />
  <!-- More columns -->
</DataTable>
```

### 1.5 Dialogs & Confirmations (All Apps)

Replace custom ConfirmDialog and `window.confirm()` with PrimeVue:

```typescript
// At app root (once)
<ConfirmDialog />
<Toast />

// In components
const confirm = useConfirm()
const toast = useToast()

confirm.require({
  message: 'Are you sure?',
  header: 'Confirm Action',
  icon: 'pi pi-exclamation-triangle',
  acceptClass: 'p-button-danger',
  accept: () => {
    // action
    toast.add({ severity: 'success', summary: 'Done', life: 3000 })
  }
})
```

### 1.6 Status Badges

Replace custom StatusBadge (admin) and `.badge-*` classes (seller) with PrimeVue `<Tag>`:

```vue
<Tag :value="status" :severity="statusSeverityMap[status]" />
```

**Deliverables:**
- [ ] All buttons migrated to `<Button>`
- [ ] All form inputs migrated to PrimeVue components
- [ ] All cards migrated to `<Card>`
- [ ] DataTables migrated with lazy loading
- [ ] Dialogs and confirmations unified
- [ ] Status badges migrated to `<Tag>`
- [ ] Custom `.btn-*`, `.card`, `.input`, `.badge-*` CSS classes removed

---

## Phase 2: Navigation & Layout (2-3 days)

### 2.1 buyer-web Navbar

Migrate to PrimeVue `<Menubar>`:
- Logo in `#start` slot
- Navigation items as model
- Search, language, auth in `#end` slot
- Add PrimeVue `<Breadcrumb>` to all pages

### 2.2 seller-portal / admin-dashboard Sidebar

Migrate to PrimeVue `<Drawer>` + `<Menu>`:
- Responsive collapse (lg breakpoint)
- Active route highlighting via Menu model
- Consistent animation

### 2.3 TopBar

Migrate to PrimeVue `<Toolbar>`:
- Hamburger button for mobile sidebar toggle
- Search `<InputText>` with icon
- Notification bell with `<Badge>` overlay
- User `<Avatar>` with `<Menu>` popup

### 2.4 Tab Navigation

Replace manual tab implementations with PrimeVue `<Tabs>`:
- seller-portal profile tabs
- seller-portal lot status tabs
- admin-dashboard user detail tabs

**Deliverables:**
- [ ] buyer-web Navbar → `<Menubar>`
- [ ] seller/admin Sidebar → `<Drawer>` + `<Menu>`
- [ ] TopBar → `<Toolbar>`
- [ ] Breadcrumbs added to all pages
- [ ] Tab navigation → `<Tabs>`

---

## Phase 3: Form Validation & UX Polish (2-3 days)

### 3.1 Install @primevue/forms + Zod

```bash
npm install @primevue/forms zod
```

### 3.2 Add Schema Validation

Create Zod schemas for all forms:
- buyer-web: bid form, registration, profile
- seller-portal: lot creation/edit, profile, bank settings
- admin-dashboard: auction creation, user actions

### 3.3 Loading States

Add PrimeVue `<Skeleton>` loaders to all data-fetching pages:
- Replace generic spinners with content-shaped skeletons
- Add `<ProgressBar>` for file uploads
- Add `<ProgressSpinner>` for button loading states

### 3.4 Empty States

Design consistent empty state patterns:
- Illustration or icon
- Descriptive message
- Action CTA button

### 3.5 Error States

Standardize error handling:
- Network errors: `<Message>` with retry button
- Validation errors: Field-level messages via `@primevue/forms`
- 404: Full-page error with navigation options
- Authentication errors: Styled error page (replace inline HTML)

**Deliverables:**
- [ ] Zod schemas for all forms
- [ ] `@primevue/forms` integration
- [ ] Skeleton loaders on all data pages
- [ ] Empty state components
- [ ] Error handling standardized

---

## Phase 4: Dark Mode (1-2 days)

### 4.1 Theme Toggle

Add `useTheme()` composable (shared package):
- Read from `localStorage`
- Fall back to system preference
- Toggle `.app-dark` class on `<html>`

### 4.2 Tailwind Dark Classes

Add `dark:` prefixes to custom layout CSS:
- Background: `bg-white dark:bg-surface-900`
- Text: `text-surface-900 dark:text-surface-0`
- Borders: `border-surface-200 dark:border-surface-700`

### 4.3 Toggle UI

Add `<ToggleSwitch>` in user settings and top bar:
```vue
<ToggleSwitch v-model="isDark" @change="toggle" />
```

**Deliverables:**
- [ ] Dark mode toggle composable
- [ ] PrimeVue dark mode via colorScheme tokens
- [ ] Tailwind dark: classes for layout
- [ ] Toggle switch in UI

---

## Phase 5: Accessibility (2-3 days)

### 5.1 Critical Fixes (EAA Compliance)

1. Add skip navigation link (all apps)
2. Verify focus trapping in all dialogs (PrimeVue handles this)
3. Add aria-label to all icon-only buttons
4. Add aria-live regions for dynamic content (toasts, timers, bids)
5. Link form errors to inputs via aria-describedby

### 5.2 Keyboard Navigation Audit

1. Test all pages with keyboard only
2. Verify Tab order is logical
3. Ensure all interactive elements are reachable
4. Test dropdown/menu navigation with arrow keys (PrimeVue built-in)

### 5.3 Screen Reader Testing

1. Test with NVDA (Windows) or VoiceOver (macOS)
2. Verify all content is announced correctly
3. Verify dynamic updates announced via aria-live

### 5.4 Automated Testing Setup

```bash
npm install @axe-core/playwright --save-dev
```

**Deliverables:**
- [ ] Skip navigation added
- [ ] Focus trapping verified
- [ ] ARIA labels complete
- [ ] Keyboard navigation tested
- [ ] Screen reader tested
- [ ] Automated a11y tests passing

---

## Phase 6: Advanced Features (Ongoing)

### 6.1 Virtual Scrolling

Add to DataTables with 100+ rows:
```vue
<DataTable :virtualScrollerOptions="{ itemSize: 46 }" scrollable scrollHeight="600px" />
```

### 6.2 Image Gallery

Replace custom image display with PrimeVue `<Galleria>`:
- Thumbnails strip
- Fullscreen mode
- Keyboard navigation
- Touch swipe support

### 6.3 File Upload

Replace custom ImageUploader with PrimeVue `<FileUpload>`:
- Drag-drop zone
- Progress indicators
- Image preview
- Multiple file support

### 6.4 Rich Text Editor

Add PrimeVue `<Editor>` for lot descriptions (if rich formatting needed).

### 6.5 Export Functionality

Add CSV/PDF export to DataTables:
```vue
<Button label="Export CSV" icon="pi pi-download" @click="dt.exportCSV()" />
```

---

## Cleanup Tasks (Post-Migration)

### Remove Dead Code

After each phase, remove unused custom code:

**Phase 1 cleanup:**
- Remove `.btn-*` classes from `main.css` files
- Remove `.card` class
- Remove `.input`, `.label`, `.select` classes
- Remove `.badge-*` classes
- Remove `.table-wrapper` / `.table-container` classes
- Remove custom `DataTable.vue` component (admin)
- Remove custom `ConfirmDialog.vue` component (admin)
- Remove custom `StatusBadge.vue` component (admin)

**Phase 2 cleanup:**
- Remove custom sidebar components
- Remove custom TopBar dropdown logic
- Remove manual tab switching code

**Phase 3 cleanup:**
- Remove manual validation functions
- Remove custom error display patterns
- Remove spinner SVG templates

### Bundle Size Monitoring

After PrimeVue adoption, monitor bundle size:
```bash
# Nuxt
npx nuxi analyze

# Vite
npx vite-bundle-visualizer
```

If bundle grows significantly, switch from auto-import to explicit imports:
```typescript
// Instead of auto-import, use manual imports
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
```

---

## Timeline Summary

| Phase | Duration | Scope |
|-------|----------|-------|
| **Phase 0** | 1-2 days | Foundation: shared tokens, PrimeVue install, Tailwind unification |
| **Phase 1** | 3-5 days | Core components: buttons, inputs, cards, tables, dialogs, badges |
| **Phase 2** | 2-3 days | Navigation: menubar, sidebar, topbar, breadcrumbs, tabs |
| **Phase 3** | 2-3 days | Forms: validation, loading states, empty states, error handling |
| **Phase 4** | 1-2 days | Dark mode: toggle, tokens, Tailwind dark classes |
| **Phase 5** | 2-3 days | Accessibility: ARIA, keyboard nav, screen reader, automated tests |
| **Phase 6** | Ongoing | Advanced: virtual scroll, gallery, file upload, export |
| **Total** | ~12-18 days | Complete design system overhaul |

---

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Breaking existing functionality | Migrate one component type at a time, test each page |
| Bundle size increase | Use explicit imports if auto-import bundles unused components |
| PrimeVue version conflicts | Pin version across all 3 apps via shared package |
| Dark mode CSS conflicts | Enable `cssLayer` to manage specificity |
| Learning curve | Start with simple components (Button, Card, Tag) before DataTable |
| Keycloak interaction | Test auth flows after PrimeVue Toast/Dialog integration |
