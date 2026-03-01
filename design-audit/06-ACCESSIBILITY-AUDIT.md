# Accessibility Audit (WCAG 2.1 AA)

## Legal Context

The **European Accessibility Act (EAA)** came into force on June 28, 2025, making WCAG 2.2 Level AA compliance a legal requirement for EU-based B2B e-commerce platforms. As an EU B2B auction platform operating across multiple member states, full accessibility compliance is not just a best practice -- it's a legal obligation.

---

## Current State Summary

| Area | buyer-web | seller-portal | admin-dashboard |
|------|-----------|---------------|-----------------|
| Semantic HTML | Partial | Partial | Partial |
| ARIA attributes | Some | Minimal | Minimal |
| Keyboard navigation | SearchBar only | None | None |
| Focus management | Basic | Focus-visible rings | Focus rings (not focus-visible) |
| Screen reader support | Not tested | Not tested | Not tested |
| Color contrast | Likely passes | Likely passes | Likely passes |
| Form labels | Partial | Partial | Partial |
| Error identification | Minimal | Basic | Minimal |
| Skip navigation | Missing | Missing | Missing |
| Focus trapping (modals) | Missing | Missing | Missing |

---

## Critical Issues (Must Fix)

### 1. No Skip Navigation Link

**All 3 apps** lack a "Skip to main content" link.

**Fix:**
```html
<!-- Add as first child of <body> -->
<a href="#main-content" class="sr-only focus:not-sr-only focus:fixed focus:top-4 focus:left-4 focus:z-50 focus:bg-white focus:px-4 focus:py-2 focus:rounded-lg focus:shadow-lg">
  Skip to main content
</a>

<!-- On main content area -->
<main id="main-content" tabindex="-1">
```

### 2. No Focus Trapping in Modals/Dialogs

**seller-portal** custom ConfirmDialog and **admin-dashboard** custom ConfirmDialog lack focus trapping. Users can tab out of modals into background content.

**Fix:** Replace with PrimeVue `<Dialog>` and `<ConfirmDialog>` which have built-in focus trapping and `aria-modal="true"`.

### 3. Missing ARIA Labels on Icon-Only Buttons

Multiple icon-only buttons across all apps lack accessible labels.

**Examples:**
```vue
<!-- BAD: No accessible label -->
<button @click="toggleSidebar">
  <svg>...</svg>
</button>

<!-- GOOD: With PrimeVue -->
<Button icon="pi pi-bars" aria-label="Toggle sidebar" text rounded />
```

### 4. Missing aria-live Regions for Dynamic Content

**buyer-web** OverbidToast notifications appear/disappear without announcing to screen readers.

**Fix:**
```html
<div aria-live="polite" aria-atomic="true" class="sr-only" id="notifications-live">
  <!-- Updated dynamically when notifications change -->
</div>

<!-- Toast container -->
<div role="alert" aria-live="assertive">
  <Toast />
</div>
```

### 5. Auction Timer Not Accessible

AuctionTimer component changes color but doesn't announce time to screen readers.

**Fix:**
```vue
<div
  role="timer"
  :aria-label="`Auction closes in ${timeRemainingText}`"
  aria-live="polite"
  aria-atomic="true"
>
  {{ formattedTime }}
</div>
```

### 6. Form Validation Errors Not Linked

Forms across all apps display error messages that aren't programmatically linked to their inputs.

**Fix:**
```vue
<label for="bid-amount">Bid Amount (EUR)</label>
<InputNumber
  id="bid-amount"
  name="bidAmount"
  aria-describedby="bid-help bid-error"
  :aria-invalid="!!errors.bidAmount"
/>
<small id="bid-help">Minimum: {{ formatCurrency(minBid) }}</small>
<small id="bid-error" v-if="errors.bidAmount" class="text-danger-500">
  {{ errors.bidAmount }}
</small>
```

---

## Major Issues (Should Fix)

### 7. Keyboard Navigation Gaps

| Component | Keyboard Support |
|-----------|-----------------|
| buyer-web SearchBar | Arrow keys, Enter, Escape |
| buyer-web Navbar dropdowns | Tab only, no arrow keys |
| seller-portal SidebarNav | Tab only |
| seller-portal TopBar notifications | Not keyboard accessible |
| admin-dashboard DataTable | No keyboard navigation |
| admin-dashboard StatusBadge | Not focusable |

**Fix:** PrimeVue components provide comprehensive keyboard navigation out of the box:
- `<Menu>`: Arrow keys, Enter/Space, Home/End
- `<DataTable>`: Arrow keys for cell navigation, Enter/Space for selection
- `<Select>`: Arrow keys, typing to filter
- `<Dialog>`: Tab cycling, Escape to close

### 8. Color-Only Status Indicators

StatusBadge in admin-dashboard uses color alone to communicate status. Users with color vision deficiency cannot distinguish statuses.

**Fix:** Always pair color with text or icon:
```vue
<Tag :value="status" :severity="getSeverity(status)" :icon="getIcon(status)" />
<!-- e.g., Active = green + checkmark, Rejected = red + X icon -->
```

### 9. Missing Document Language

buyer-web sets `lang` via i18n but seller-portal and admin-dashboard have hardcoded `lang="en"`.

**Fix:** Set `<html lang>` dynamically based on user preference.

### 10. Insufficient Focus Indicators

admin-dashboard uses `focus:ring-2` instead of `focus-visible:ring-2`. This means focus rings appear on mouse clicks, not just keyboard navigation.

**Fix:** Use `focus-visible` consistently across all apps. PrimeVue components handle this automatically.

---

## Minor Issues (Nice to Fix)

### 11. Table Headers Not Associated with Cells

Custom table implementations lack proper `<th scope="col">` attributes.

**Fix:** PrimeVue `<DataTable>` automatically generates proper table semantics.

### 12. Image Alt Text

Lot images may lack descriptive alt text.

**Fix:**
```vue
<img :src="lot.imageUrl" :alt="`${lot.title} - ${lot.category} industrial equipment`" />
```

### 13. Consistent Heading Hierarchy

Some pages skip heading levels (h1 -> h3) or use headings for styling rather than structure.

**Fix:** Enforce strict h1 > h2 > h3 hierarchy. Use CSS classes for visual styling, not heading elements.

### 14. Touch Target Sizes

Some buttons and interactive elements may be smaller than the WCAG 2.2 minimum of 24x24 CSS pixels.

**Fix:** Ensure all interactive elements have `min-h-[44px] min-w-[44px]` for touch devices.

---

## Accessibility Testing Plan

### Automated Testing

```bash
# Install axe-core for Playwright/Vitest
npm install @axe-core/playwright --save-dev
```

```typescript
// e2e accessibility test
import { test, expect } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'

test('homepage has no accessibility violations', async ({ page }) => {
  await page.goto('/')
  const results = await new AxeBuilder({ page }).analyze()
  expect(results.violations).toEqual([])
})
```

### Manual Testing Checklist

- [ ] All pages navigable with keyboard only (Tab, Shift+Tab, Enter, Space, Escape, Arrow keys)
- [ ] All interactive elements have visible focus indicators
- [ ] All modals/dialogs trap focus
- [ ] All images have appropriate alt text
- [ ] All form fields have associated labels
- [ ] All error messages are announced by screen readers
- [ ] Color is not the sole means of conveying information
- [ ] Text content has minimum 4.5:1 contrast ratio
- [ ] Large text has minimum 3:1 contrast ratio
- [ ] Page has proper heading hierarchy (h1 > h2 > h3)
- [ ] Dynamic content changes are announced via aria-live
- [ ] Auction timer is accessible to screen readers
- [ ] All pages have a skip navigation link
- [ ] Document language is correctly set

### Screen Reader Testing

Test with:
- **NVDA** (Windows, free)
- **VoiceOver** (macOS, built-in)
- Verify: all interactive elements announced with role + label + state

---

## PrimeVue Accessibility Advantages

Adopting PrimeVue components provides significant accessibility improvements for free:

| Feature | PrimeVue Built-in |
|---------|-------------------|
| ARIA roles | Automatic on all components |
| Keyboard navigation | Full arrow key, Tab, Enter, Space, Escape support |
| Focus trapping | Built-in for Dialog, Drawer, Menu |
| Screen reader labels | Configurable via `aria.` locale API |
| Focus management | Auto-focus first element in dialogs |
| Selection announcements | `aria-selected`, `aria-checked` |
| Expanded/collapsed state | `aria-expanded` on toggleable components |
| Sort announcements | `aria-sort` on DataTable columns |
| Live regions | Built-in for Toast notifications |
| Disabled state | Proper `aria-disabled` + visual indicator |
