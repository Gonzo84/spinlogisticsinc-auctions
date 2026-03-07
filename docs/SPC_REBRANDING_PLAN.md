# SPC Rebranding Plan

> **Comprehensive plan for rebranding the EU Auction Platform to SPC, storitveno prodajni center, d.o.o.**
> Prepared: March 2026

---

## Table of Contents

1. [Company Profile](#1-company-profile)
2. [Brand Identity Analysis](#2-brand-identity-analysis)
3. [Rebranding Requirements & Best Practices](#3-rebranding-requirements--best-practices)
4. [Color System Migration](#4-color-system-migration)
5. [Typography Migration](#5-typography-migration)
6. [Logo & Visual Assets](#6-logo--visual-assets)
7. [Content & Copy Changes](#7-content--copy-changes)
8. [Internationalization (i18n)](#8-internationalization-i18n)
9. [Component & UI Customization](#9-component--ui-customization)
10. [Product Categories & Domain Mapping](#10-product-categories--domain-mapping)
11. [Technical Infrastructure Changes](#11-technical-infrastructure-changes)
12. [SEO & Metadata](#12-seo--metadata)
13. [Keycloak & Auth Branding](#13-keycloak--auth-branding)
14. [Email & Notification Branding](#14-email--notification-branding)
15. [Social Media & External Presence](#15-social-media--external-presence)
16. [Legal & Compliance Pages](#16-legal--compliance-pages)
17. [Implementation Phases](#17-implementation-phases)
18. [File-by-File Change Inventory](#18-file-by-file-change-inventory)
19. [Verification Checklist](#19-verification-checklist)

---

## 1. Company Profile

### Legal & Financial Summary

| Field | Value |
|-------|-------|
| **Full Legal Name** | SPC, storitveno podjetje, d.o.o. |
| **Trade Name** | SPC - Storitveno Prodajni Center (Service Sales Center) |
| **Registration No.** | 6128076000 |
| **VAT ID** | SI 49185675 |
| **Founded** | 9 March 2012 |
| **Legal Form** | d.o.o. (Limited Liability Company) |
| **Registered Address** | Premrlova ulica 11, 1000 Ljubljana, Slovenia |
| **Warehouse** | Peruzzijeva ulica 175, 1000 Ljubljana (IOC Rudnik) |
| **Industry Code (SKD)** | 25.110 -- Manufacture of metal structures and parts |
| **Director/Owner** | Matej Zupan (95.83%) |
| **Co-owner** | Katarina Zagar (4.17%) |
| **Employees** | ~9 (avg. gross salary EUR 1,711/month) |
| **Phone** | +386 1 77 77 666 |
| **Email** | info@spc.si |
| **Website** | spc.si / shop.spc.si |
| **Credit Rating** | A+ (2025) -- "Low credit risk" |
| **Business Hours** | Mon-Thu 08:00-11:00 & 12:00-16:00, Fri 07:00-12:00 |

### Financial Performance

| Year | Revenue | Net Profit | EBITDA | Equity | Assets |
|------|---------|------------|--------|--------|--------|
| 2022 | EUR 3,276,912 | EUR 241,072 | EUR 486,013 | EUR 1,228,221 | EUR 2,674,409 |
| 2023 | EUR 3,575,817 | EUR 328,626 | EUR 671,333 | EUR 1,556,847 | EUR 3,008,126 |
| 2024 | EUR 4,187,226 | EUR 344,416 | EUR 651,070 | EUR 1,901,263 | EUR 3,471,278 |

**Key metrics (2024):** 17.1% YoY revenue growth, 8.2% net margin, 15.5% EBITDA margin, 54.8% equity ratio.

### Market Position

- **900+ customers** across Central Europe and the Balkans
- **85%+ recommendation rate**
- **Niche position:** Between large manufacturers (Arcont/CONTAINEX, 850+ employees) and pure trading platforms
- **Competitive advantage:** Vertically integrated -- manufactures, sells, rents, refurbishes, and delivers
- **Growth vision:** Expand from Central Europe/Balkans to full European coverage

### Core Business Lines

1. **Containers:** Office/residential (SPC-branded models), shipping (20ft/40ft), sanitary (~55 models), storage, construction, custom/modular
2. **Climate Control Equipment:** Heaters (electric, diesel, gas), dehumidifiers, AC units, air purifiers, humidifiers
3. **Construction Equipment:** Fencing (Economico, Privacy), generators, construction accessories
4. **Services:** Sale, rental, delivery, manufacturing, renovation, repair, customization

### Competitors in Slovenia

| Company | Scale | Focus |
|---------|-------|-------|
| **Arcont d.d.** | 850+ employees | CONTAINEX manufacturer, B2B export |
| **Hosekra** | Medium | Residential/office containers |
| **Container d.o.o.** | Small-medium | Container sales/services |
| **Algeco** | International | Modular space solutions |
| **CONTAINEX** | European leader | Modular space systems (Walter Group) |

---

## 2. Brand Identity Analysis

### 2.1 Current SPC Brand (from spc.si)

#### Color Palette

| Role | Hex | RGB | Usage |
|------|-----|-----|-------|
| **Primary** | `#004d71` | rgb(0, 77, 113) | Buttons, links, headers, accents |
| **Primary Hover** | `#003e5a` | rgb(0, 62, 90) | Hover states |
| **Secondary** | `#333333` | rgb(51, 51, 51) | Secondary buttons, dark UI elements |
| **Secondary Hover** | `#292929` | rgb(41, 41, 41) | Dark hover states |
| **Text Body** | `#6C6C6C` | rgb(108, 108, 108) | Paragraph text |
| **Text Headings** | `#000000` | rgb(0, 0, 0) | H1-H6 headings |
| **Light Background** | `#E7E7E7` | rgb(231, 231, 231) | Section backgrounds, dividers |
| **Light Accent** | `#eeeeee` | rgb(238, 238, 238) | Tertiary buttons, input backgrounds |
| **Border** | `#b1a6a6c2` | rgba(177, 166, 166, 0.76) | Borders, separators |
| **White** | `#ffffff` | rgb(255, 255, 255) | Card backgrounds, panels |

**Color Character:** The SPC palette is a **professional deep teal/navy** with neutral grays. It conveys **trust, stability, and industrial seriousness** -- appropriate for a B2B equipment company. No bright accents or playful colors.

#### Typography

| Role | Font Family | Weight | Size |
|------|-------------|--------|------|
| **Body text** | Open Sans | 400 (Regular) | 16px, line-height 1.96em |
| **Headings** | Raleway | 600-700 (SemiBold-Bold) | 20-42px |
| **Small text** | Open Sans | 400 | 13px |

**Typography Character:** Clean, modern sans-serif pairing. Open Sans is highly readable and universally trusted. Raleway adds distinctive elegance to headings without being flashy.

#### Logo

- **File:** `docs/presentation/assets/spc-logo.png` (73x60px, PNG with alpha)
- **Style:** Compact monochromatic mark
- **Placement:** Top-left header, links to homepage
- **Need:** Higher-resolution version (SVG preferred) for production use

#### UI Patterns (from spc.si)

| Pattern | SPC Style |
|---------|-----------|
| **Buttons** | Fully rounded (`border-radius: 9999px`), padding `calc(.667em + 2px) calc(1.333em + 2px)` |
| **Cards** | Subtle shadows, clean white backgrounds |
| **Navigation** | 4 items: O nas, Zaposlitev, Kontakt, Trgovina |
| **CTA** | "Pridobi ponudbo" (Get Quote) -- prominent, primary color |
| **Shadows** | Natural: `6px 6px 9px rgba(0,0,0,0.2)` |
| **Spacing** | 20-80px scale (0.44rem to 5.06rem) |
| **Layout** | Flexbox/grid, 0.5em gaps, max-width containers |

#### Brand Messaging & Tone

- **Tagline:** "Podjetje z vecletno tradicijo vseh vrst kontejnerjev" (A company with years of tradition in all types of containers)
- **Tone:** Professional B2B, approachable, direct, benefit-focused
- **Key themes:** Reliability, speed ("24-hour response"), quality verification, competitive pricing, one-stop-shop
- **Trust signals:** EBONITETE.SI certificate (excellent financial reliability), SafeSigned seal, A+ credit rating, 900+ customers

### 2.2 Current Platform Brand (EU Auction Platform)

| Element | Current Value |
|---------|---------------|
| **Name** | "EU Auction Platform" / "EU Auction" |
| **Primary Color** | Blue scale (`#2563eb` / Tailwind blue-600) |
| **Logo** | `pi pi-bolt` icon in blue rounded square |
| **Typography** | Inter (400-700) |
| **Surface Colors** | Slate (light mode), Zinc (dark mode) |
| **App Accents** | Seller: Green, Admin: Purple |
| **Component Style** | Rounded (lg/xl border-radius), subtle shadows |

### 2.3 Brand Gap Analysis

| Element | Current Platform | SPC Brand | Action Required |
|---------|-----------------|-----------|-----------------|
| **Primary color** | Blue `#2563eb` | Deep teal `#004d71` | Generate full teal color scale |
| **Font (body)** | Inter | Open Sans | Swap Google Fonts link |
| **Font (headings)** | Inter | Raleway | Add Raleway, configure font-family |
| **Logo** | Generic bolt icon | SPC company logo | Replace in Navbar, Footer, favicon |
| **Brand name** | "EU Auction Platform" | "SPC Aukcije" / "SPC Auctions" | Update all 35+ references |
| **Button radius** | `border-radius: lg` | `border-radius: 9999px` (pill) | Update preset.ts |
| **Languages** | en, nl, de, fr, pl, it, ro | sl, hr, de, en, it, sr, hu | Replace/add locales |
| **Countries** | NL, DE, BE, FR, PL, IT, RO | SI, HR, AT, DE, IT, BA, RS, HU | Update country list |
| **Social links** | Generic linkedin/twitter/youtube | facebook.com/SPContainer, instagram.com/spcontainers | Update Footer.vue |
| **Contact** | None | +386 1 77 77 666, info@spc.si | Add to footer/contact |

---

## 3. Rebranding Requirements & Best Practices

### 3.1 White-Label Rebranding Checklist (Industry Standard)

Based on research of platform rebranding best practices (2025-2026):

**Critical (Launch Blockers):**
- [ ] Brand name updated across all user-visible text
- [ ] Logo (SVG + PNG) in all required sizes and placements
- [ ] Color palette defined and applied to all design tokens
- [ ] Typography loaded and applied
- [ ] Favicon and app icons
- [ ] Page titles and meta descriptions
- [ ] Domain names + SSL certificates
- [ ] Auth/login page branding (Keycloak theme)

**Professional Polish (Week 1):**
- [ ] Open Graph / social sharing images (1200x630)
- [ ] PWA manifest with app name and icons (if applicable)
- [ ] Translation files updated with brand name
- [ ] Error pages and empty states branded
- [ ] Email sender address and branding
- [ ] Security headers updated with correct domains

**Growth & Marketing (Month 1):**
- [ ] Landing page content and copy tailored to SPC's products
- [ ] Legal pages (terms, privacy, cookies)
- [ ] Structured data / JSON-LD for search engines
- [ ] Social media profile assets
- [ ] Brand guidelines document

### 3.2 B2B Industrial Platform Branding Best Practices

Key findings from 2025-2026 B2B design trends:

1. **Trust signals over flash:** Industrial buyers want proof -- certifications, inspection reports, verified seller badges. SPC's EBONITETE.SI certificate and A+ rating should be prominently displayed.

2. **Calm design:** Clean layouts, generous white space, strong typographic hierarchy. The Open Sans + Raleway pairing fits this trend perfectly.

3. **Accessibility as professionalism:** WCAG AA minimum contrast ratios. SPC's `#004d71` on white provides a contrast ratio of 7.4:1 (exceeds AAA).

4. **Sustainability storytelling:** The CO2 service is a major differentiator -- SPC can market "every container resold avoids 8-12 tonnes of CO2."

5. **Self-service experiences:** The auction/bidding flow naturally serves this B2B expectation.

### 3.3 Platform Architecture Advantages

The platform's centralized design token architecture makes rebranding efficient:

```
frontend/shared/design-tokens/
  preset.ts           -- PrimeVue color/component tokens (SINGLE source of truth for colors)
  tailwind.preset.ts  -- Tailwind color scales (must sync with preset.ts)
  pt.ts               -- Global PrimeVue pass-through classes
  status-severity.ts  -- Status-to-severity mapping (unchanged)
```

PrimeVue 4's three-tier token architecture (primitive > semantic > component) means: **change the primitive-to-semantic color mapping once, and all 161+ component overrides update automatically.**

---

## 4. Color System Migration

### 4.1 SPC Primary Color Scale

SPC's primary `#004d71` is a deep teal. We need a full 50-950 scale for PrimeVue and Tailwind. Generated using HSL interpolation from `#004d71`:

```
SPC Teal Scale:
  50:  #e6f2f8   -- Lightest tint (backgrounds, highlights)
  100: #b3d9e8   -- Light (hover highlights)
  200: #80bfd8   -- Light accent
  300: #4da6c8   -- Medium light
  400: #268db5   -- Medium
  500: #0077a3   -- Medium (close to primary)
  600: #006a92   -- Standard
  700: #004d71   -- SPC PRIMARY (buttons, headers, links)
  800: #003e5a   -- SPC HOVER (dark variant)
  900: #002f44   -- Darkest usable
  950: #001f2e   -- Near-black teal
```

### 4.2 preset.ts Changes

```typescript
// frontend/shared/design-tokens/preset.ts
semantic: {
  primary: {
    50:  '#e6f2f8',
    100: '#b3d9e8',
    200: '#80bfd8',
    300: '#4da6c8',
    400: '#268db5',
    500: '#0077a3',
    600: '#006a92',
    700: '#004d71',   // SPC primary
    800: '#003e5a',   // SPC hover
    900: '#002f44',
    950: '#001f2e',
  },
  colorScheme: {
    light: {
      primary: {
        color: '{primary.700}',           // #004d71 (was .600)
        contrastColor: '#ffffff',
        hoverColor: '{primary.800}',      // #003e5a (was .700)
        activeColor: '{primary.900}',     // (was .800)
      },
      highlight: {
        background: '{primary.50}',
        focusBackground: '{primary.100}',
        color: '{primary.700}',
        focusColor: '{primary.800}',
      },
      surface: {
        // Keep slate scale -- works well with SPC's neutral grays
      },
    },
    // Dark mode: keep existing structure, adjust primary references
  },
}
```

**Key change:** Primary color maps to `{primary.700}` instead of `{primary.600}` because SPC's brand primary `#004d71` is a darker shade.

### 4.3 tailwind.preset.ts Changes

```typescript
colors: {
  primary: {
    50:  '#e6f2f8',
    100: '#b3d9e8',
    200: '#80bfd8',
    300: '#4da6c8',
    400: '#268db5',
    500: '#0077a3',
    600: '#006a92',
    700: '#004d71',
    800: '#003e5a',
    900: '#002f44',
    950: '#001f2e',
    DEFAULT: '#004d71',   // SPC primary
  },
  // success, warning, danger: KEEP AS-IS (semantic colors, not brand-specific)
  // seller accent: consider changing to SPC teal variant or keep green
  // admin accent: consider changing to SPC dark variant or keep purple
}
```

### 4.4 Semantic Colors (Unchanged)

These are universal UI semantics and should NOT be changed for branding:

| Color | Current | Keep? | Reason |
|-------|---------|-------|--------|
| Success | Green | Yes | Universal "positive" signal |
| Warning | Amber | Yes | Universal "caution" signal |
| Danger | Red | Yes | Universal "error/delete" signal |
| Seller accent | Green | Discuss | Could become SPC teal variant |
| Admin accent | Purple | Discuss | Could become SPC dark navy |

---

## 5. Typography Migration

### 5.1 Font Loading Changes

**Current:** Inter (400, 500, 600, 700) from Google Fonts
**Target:** Open Sans (400, 500, 600, 700) + Raleway (500, 600, 700) from Google Fonts

#### Files to Update

**buyer-web/nuxt.config.ts** (line 69):
```html
<!-- Current -->
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" />

<!-- New -->
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Open+Sans:wght@400;500;600;700&family=Raleway:wght@500;600;700&display=swap" />
```

**seller-portal/index.html** and **admin-dashboard/index.html** (line 8-10):
Same Google Fonts link replacement.

### 5.2 Tailwind Font Family

**tailwind.preset.ts:**
```typescript
fontFamily: {
  sans: ['Open Sans', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'sans-serif'],
  heading: ['Raleway', 'Open Sans', 'system-ui', 'sans-serif'],
  mono: ['JetBrains Mono', 'ui-monospace', 'Cascadia Code', 'Fira Code', 'monospace'],
},
```

### 5.3 Heading Font Application

Add to the shared CSS entry point (or individual app CSS files):

```css
h1, h2, h3, h4, h5, h6 {
  font-family: 'Raleway', 'Open Sans', system-ui, sans-serif;
}
```

Or use Tailwind's `@apply` in components:
```html
<h1 class="font-heading text-2xl font-bold">...</h1>
```

### 5.4 Typography Scale (SPC-aligned)

| Element | Font | Size | Weight | Line Height |
|---------|------|------|--------|-------------|
| H1 | Raleway | 42px (2.625rem) | 700 | 1.2 |
| H2 | Raleway | 36px (2.25rem) | 700 | 1.3 |
| H3 | Raleway | 24px (1.5rem) | 600 | 1.4 |
| H4 | Raleway | 20px (1.25rem) | 600 | 1.5 |
| Body | Open Sans | 16px (1rem) | 400 | 1.96 (SPC standard) |
| Small | Open Sans | 13px (0.8125rem) | 400 | 1.5 |
| UI Labels | Open Sans | 14px (0.875rem) | 500 | 1.5 |

Note: SPC uses line-height 1.96em for body text, which is unusually generous. This contributes to their clean, readable feel. Consider whether to adopt this exactly (may feel too spaced on a data-heavy auction platform) or use a compromise like 1.7em.

---

## 6. Logo & Visual Assets

### 6.1 Required Logo Variants

| Variant | Size | Format | Usage |
|---------|------|--------|-------|
| **Primary logo** | Vector | SVG | Navbar, headers (scalable) |
| **Primary logo** | 200x164px | PNG (2x) | Fallback |
| **Compact mark** | 32x32px | SVG/PNG | Navbar collapsed, mobile |
| **Favicon** | 32x32, 16x16 | ICO/PNG | Browser tab |
| **Favicon SVG** | Vector | SVG | Modern browsers |
| **Apple Touch Icon** | 180x180 | PNG | iOS home screen |
| **OG Image** | 1200x630 | PNG/JPG | Social sharing |
| **Loading spinner** | 48x48 | SVG | App loading states |

### 6.2 Current Asset Inventory

Available in `docs/presentation/assets/`:

| File | Description | Usable For |
|------|-------------|------------|
| `spc-logo.png` | Company logo (73x60px) | Reference only (too small for production) |
| `spc-containers-hero.png` | Container hero image (1024x1024) | Homepage hero, category images |
| `spc-machinery.jpg` | Machinery image (1024x1024) | Category images |
| `spc-container-product-1.jpg` | Office container photo | Demo lot images |
| `spc-container-product-2.jpg` | Container photo | Demo lot images |
| `spc-container-product-3.png` | Container/modular photo | Demo lot images |
| `spc-process.jpg` | Process illustration | About page |

### 6.3 Assets Needed from SPC

**Must request from Matej Zupan / SPC:**

1. **High-resolution logo** -- SVG format preferred, or minimum 500px wide PNG with transparency
2. **Logo on dark background** -- White/light variant for dark headers/footers
3. **Brand guidelines document** -- If one exists (may not for a company this size)
4. **Product photography** -- High-res photos of their actual container models (SPC K2-K8, sanitary units, shipping containers)
5. **Team photo** -- For about/trust page (optional)
6. **Certification logos** -- EBONITETE.SI badge, SafeSigned seal (for trust section)

### 6.4 Logo Integration Points

| Location | Current | New |
|----------|---------|-----|
| **Buyer Navbar** (`Navbar.vue:7-10`) | `pi pi-bolt` icon + "EU Auction" text | SPC logo image + "SPC Aukcije" text |
| **Buyer Footer** (`Footer.vue:8-11`) | `pi pi-bolt` icon + "EU Auction" text | SPC logo image + brand text |
| **Seller Sidebar** (layout component) | Brand icon/text | SPC logo image |
| **Admin Sidebar** (layout component) | Brand icon/text | SPC logo image |
| **Favicon** (all 3 apps) | `/favicon.svg` (doesn't exist) | SPC favicon |
| **Loading/splash screen** | None | SPC logo centered |

---

## 7. Content & Copy Changes

### 7.1 Brand Name References (35+ locations)

Every instance of "EU Auction Platform" or "EU Auction" must be replaced:

| Location | Current Text | New Text |
|----------|-------------|----------|
| `buyer-web/nuxt.config.ts:59` | "EU Auction Platform - Buy Industrial Equipment" | "SPC Aukcije - Industrijska oprema na drazbi" |
| `buyer-web/nuxt.config.ts:63` | "B2B online auction platform for industrial equipment across Europe" | "B2B platforma za spletne drazbe industrijske opreme v Evropi" |
| `buyer-web/app.vue:12` | "EU Auction Platform" (title suffix) | "SPC Aukcije" |
| `buyer-web/error.vue:70` | "EU Auction Platform" | "SPC Aukcije" |
| `seller-portal/index.html:7` | "Seller Portal - EU Auction Platform" | "Portal za prodajalce - SPC Aukcije" |
| `admin-dashboard/index.html:7` | "Admin Dashboard - EU Auction Platform" | "Nadzorna plosca - SPC Aukcije" |
| `admin-dashboard/router/index.ts:89` | "Admin Dashboard - EU Auction Platform" | "Nadzorna plosca - SPC Aukcije" |
| `Navbar.vue:10` | "EU Auction" | "SPC" (with logo) |
| `Footer.vue:11` | "EU Auction" | "SPC" (with logo) |
| `i18n/locales/en.json:329` | "EU Auction Platform. All rights reserved." | "SPC d.o.o. All rights reserved." |
| `i18n/locales/de.json:329` | "EU Auction Platform. Alle Rechte vorbehalten." | "SPC d.o.o. Alle Rechte vorbehalten." |
| (+ 5 more locale files) | Same pattern | Same pattern |
| `en.json:313` (footer.about) | "Europe's leading B2B online auction platform..." | "SPC - Vodilna B2B platforma za drazbe kontejnerjev in industrijske opreme v srednji Evropi." |

### 7.2 Hero Section Copy

**Current (en.json):**
```json
"heroTitle": "Find Industrial Equipment at Auction",
"heroSubtitle": "Thousands of machines, vehicles and tools from across Europe. Bid online, collect locally."
```

**SPC-branded:**
```json
"heroTitle": "Kontejnerji in industrijska oprema na drazbi",
"heroSubtitle": "Pisarniski, bivalni, sanitarni in ladijski kontejnerji ter gradbena oprema. Ponudite, drazite, prevzemite.",

// English variant:
"heroTitle": "Containers & Industrial Equipment at Auction",
"heroSubtitle": "Office, residential, sanitary and shipping containers plus construction equipment. Browse, bid, collect."
```

### 7.3 Platform Description Copy

**Current about text:** Generic industrial equipment marketplace language.

**SPC-specific copy** should emphasize:
- 10+ years of container expertise
- In-house manufacturing in Ljubljana
- 900+ satisfied customers
- Central Europe & Balkans focus
- Quality verification before every delivery
- Competitive pricing through auction format
- CO2 savings through equipment reuse

### 7.4 Footer Content

**Social media links** (replace generic placeholders in `Footer.vue:18-45`):

| Platform | Current | SPC |
|----------|---------|-----|
| LinkedIn | `https://linkedin.com` | Company page URL (to be created/found) |
| Twitter/X | `https://twitter.com` | Remove (SPC has no active Twitter) |
| YouTube | `https://youtube.com` | Remove (SPC has no YouTube) |
| Facebook | Not present | `https://facebook.com/SPContainer` |
| Instagram | Not present | `https://instagram.com/spcontainers` |

**Contact information** (add to footer):
- Phone: +386 1 77 77 666
- Email: info@spc.si
- Address: Premrlova ulica 11, 1000 Ljubljana

---

## 8. Internationalization (i18n)

### 8.1 Language Configuration Change

**Current languages:** en, nl, de, fr, pl, it, ro (Western/Central European focus)
**SPC target languages:** sl, hr, de, en, it, sr, hu (Central Europe/Balkans focus)

| Code | Language | Priority | Reason |
|------|----------|----------|--------|
| **sl** | Slovenian | P0 -- Default | SPC's primary language, home market |
| **hr** | Croatian | P0 | Largest Balkan market, mutually intelligible with Slovenian |
| **de** | Deutsch | P1 | Austria + Germany markets (keep existing) |
| **en** | English | P1 | International standard (keep existing) |
| **it** | Italiano | P1 | Italy border market (keep existing) |
| **sr** | Serbian | P2 | Balkans expansion |
| **hu** | Hungarian | P2 | Central Europe neighbor |

**Languages to remove:** nl (Dutch), fr (French), pl (Polish), ro (Romanian) -- not in SPC's target markets.

### 8.2 nuxt.config.ts i18n Changes

```typescript
i18n: {
  locales: [
    { code: 'sl', name: 'Slovenscina', file: 'sl.json' },  // NEW - DEFAULT
    { code: 'hr', name: 'Hrvatski', file: 'hr.json' },      // NEW
    { code: 'de', name: 'Deutsch', file: 'de.json' },       // KEEP
    { code: 'en', name: 'English', file: 'en.json' },       // KEEP
    { code: 'it', name: 'Italiano', file: 'it.json' },      // KEEP
    { code: 'sr', name: 'Srpski', file: 'sr.json' },        // NEW
    { code: 'hu', name: 'Magyar', file: 'hu.json' },        // NEW
  ],
  defaultLocale: 'sl',  // Changed from 'en'
}
```

### 8.3 Navbar Language Switcher Flags

Update `Navbar.vue` `availableLocales` array:

```typescript
const availableLocales: LocaleEntry[] = [
  { code: 'sl', name: 'Slovenscina', flag: '\u{1F1F8}\u{1F1EE}' },  // SI flag
  { code: 'hr', name: 'Hrvatski',    flag: '\u{1F1ED}\u{1F1F7}' },  // HR flag
  { code: 'de', name: 'Deutsch',     flag: '\u{1F1E9}\u{1F1EA}' },  // DE flag
  { code: 'en', name: 'English',     flag: '\u{1F1EC}\u{1F1E7}' },  // GB flag
  { code: 'it', name: 'Italiano',    flag: '\u{1F1EE}\u{1F1F9}' },  // IT flag
  { code: 'sr', name: 'Srpski',      flag: '\u{1F1F7}\u{1F1F8}' },  // RS flag
  { code: 'hu', name: 'Magyar',      flag: '\u{1F1ED}\u{1F1FA}' },  // HU flag
]
```

### 8.4 New Translation Files Needed

| File | Effort | Approach |
|------|--------|----------|
| `sl.json` | High | Full Slovenian translation (SPC's primary language) |
| `hr.json` | Medium | Adapt from Slovenian (mutually intelligible) |
| `sr.json` | Medium | Adapt from Croatian (very similar) |
| `hu.json` | High | Full Hungarian translation |

The existing `en.json` (~330 keys) serves as the template. Each new file needs all keys translated with SPC-specific product terminology (kontejnerji, drazbe, etc.).

### 8.5 Country Selector (Homepage)

Update the "Browse by Country" section from current countries to SPC's markets:

| Flag | Country | ISO Code |
|------|---------|----------|
| SI | Slovenia | SI |
| HR | Croatia | HR |
| AT | Austria | AT |
| DE | Germany | DE |
| IT | Italy | IT |
| BA | Bosnia & Herzegovina | BA |
| RS | Serbia | RS |
| HU | Hungary | HU |

---

## 9. Component & UI Customization

### 9.1 Button Style (SPC pill buttons)

SPC uses fully rounded "pill" buttons. Update `preset.ts`:

```typescript
button: {
  root: {
    borderRadius: '9999px',  // Changed from '{border.radius.lg}'
  },
},
```

### 9.2 Component Border Radius Alignment

SPC uses a mix of rounded and slightly rounded elements. Recommended mapping:

| Component | Current | SPC-aligned | Rationale |
|-----------|---------|-------------|-----------|
| **Button** | `lg` (~8px) | `9999px` (pill) | SPC's signature button style |
| **Card** | `xl` (~12px) | `xl` | Keep -- works with SPC's clean aesthetic |
| **Dialog** | `xl` | `xl` | Keep |
| **Input** | `lg` | `xl` | Slightly more rounded to match pill buttons |
| **Select** | `lg` | `xl` | Match input |
| **Tag** | `xl` | `9999px` (pill) | Match button style |
| **Badge** | `xl` | `9999px` (pill) | Match button style |

### 9.3 Shadow System

SPC uses a distinctive shadow system. Consider adopting for cards:

```typescript
card: {
  root: {
    borderRadius: '{border.radius.xl}',
    shadow: '6px 6px 9px rgba(0, 0, 0, 0.12)',  // SPC-style natural shadow
  },
},
```

### 9.4 pt.ts (Pass-Through) Adjustments

The global PrimeVue pass-through classes may need updates if Tailwind color classes reference `primary` or specific color utilities. Review `pt.ts` for any hardcoded `bg-blue-*` or `text-blue-*` references.

---

## 10. Product Categories & Domain Mapping

### 10.1 SPC Product Categories for Platform

Map SPC's actual product lines to platform auction categories:

| Platform Category | SPC Product Line | Subcategories |
|-------------------|-----------------|---------------|
| **Pisarniski in bivalni kontejnerji** | Office/Residential Containers | SPC K2-K8 single, DV5-DV8 double, TR K8 triple, Foldable |
| **Ladijski kontejnerji** | Shipping Containers | 20ft BOX, 40ft BOX, 40ft HC, Used, 1st Trip, Refurbished |
| **Sanitarni kontejnerji** | Sanitary Containers | SPC SAN A series, C series, E series |
| **Skladiscni kontejnerji** | Storage Containers | Standard, modified |
| **Gradbeni kontejnerji** | Construction Containers | Site containers, worker accommodations |
| **Modularne strukture** | Custom/Modular Buildings | Offices, medical, food service, schools, shops |
| **Klimatska oprema** | Climate Control | Electric heaters, diesel heaters, gas heaters, AC, dehumidifiers, air purifiers, humidifiers |
| **Gradbena oprema** | Construction Equipment | Fencing (Economico, Privacy), generators, accessories |
| **Ventilacija** | Ventilation | Fans, ventilators |

### 10.2 Category Icons

Map PrimeVue icons to each category:

| Category | Icon | PrimeIcon |
|----------|------|-----------|
| Office Containers | Building | `pi pi-building` |
| Shipping Containers | Box | `pi pi-box` |
| Sanitary Containers | Home | `pi pi-home` |
| Modular Structures | Table | `pi pi-th-large` |
| Climate Control | Sun/Snowflake | `pi pi-sun` |
| Construction Equipment | Wrench | `pi pi-wrench` |
| Fencing | Shield | `pi pi-shield` |

### 10.3 Currency & Pricing

- **Currency:** EUR (already configured)
- **VAT display:** "Cene brez DDV" (prices excl. VAT) -- SPC standard
- **Price format:** `0.000,00 EUR` (Slovenian locale formatting)
- **Starting bid ranges:** EUR 650 (heater) to EUR 25,000 (foldable house)

---

## 11. Technical Infrastructure Changes

### 11.1 Domain Configuration

| Frontend | Current | SPC Production |
|----------|---------|---------------|
| Buyer web | localhost:3000 | `drazbe.spc.si` or `aukcije.spc.si` |
| Seller portal | localhost:5174 | `prodajalec.spc.si` or `seller.spc.si` |
| Admin dashboard | localhost:5175 | `admin.spc.si` |
| API Gateway | localhost:8080 | `api.spc.si` |

### 11.2 Keycloak Configuration

| Setting | Current | SPC |
|---------|---------|-----|
| Realm name | `auction-platform` | `spc-aukcije` (or keep, it's internal) |
| Realm display name | (default) | "SPC Aukcije" |
| Client IDs | `buyer-web`, `seller-portal`, `admin-dashboard` | Keep (internal identifiers) |
| Login theme | Default | SPC-branded (colors, logo) |
| Email theme | Default | SPC-branded |
| Redirect URIs | localhost:* | `*.spc.si` |

### 11.3 Environment Variables

```bash
# .env changes for SPC deployment
NUXT_PUBLIC_API_BASE_URL=https://api.spc.si/api/v1
NUXT_PUBLIC_WS_BASE_URL=wss://api.spc.si/ws
NUXT_PUBLIC_KEYCLOAK_URL=https://auth.spc.si
NUXT_PUBLIC_KEYCLOAK_REALM=spc-aukcije
```

### 11.4 Nginx Configuration

Update `infrastructure/config/nginx/nginx-spa-prod.conf`:
- Server names: `drazbe.spc.si`, `prodajalec.spc.si`, `admin.spc.si`
- CSP headers: Update `connect-src`, `font-src`, `img-src` with SPC domains
- SSL certificate paths

### 11.5 Docker & Helm

| File | Change |
|------|--------|
| `docker/compose/.env` | Domain variables |
| `helm/auction-platform/values.yaml` | Ingress hosts, image names |
| `.github/workflows/ci.yml` | Docker registry, deployment targets |

### 11.6 Package Names (Optional)

These are internal and don't affect users, but for consistency:

| File | Current | New (optional) |
|------|---------|----------------|
| `frontend/package.json` | `auction-platform-frontend` | `spc-aukcije-frontend` |
| `design-tokens/package.json` | `@auction-platform/design-tokens` | `@spc-aukcije/design-tokens` |

**Recommendation:** Only rename if deploying to a separate npm registry. For internal monorepo use, the rename adds complexity with no user-facing benefit.

---

## 12. SEO & Metadata

### 12.1 Page Titles

| Page | Current | SPC |
|------|---------|-----|
| Homepage | "EU Auction Platform - Buy Industrial Equipment" | "SPC Aukcije - Kontejnerji in oprema na drazbi" |
| Search | "Search - EU Auction Platform" | "Iskanje - SPC Aukcije" |
| Lot detail | "{lot name} \| EU Auction Platform" | "{lot name} \| SPC Aukcije" |
| Seller Portal | "Seller Portal - EU Auction Platform" | "Portal za prodajalce - SPC" |
| Admin Dashboard | "Admin Dashboard - EU Auction Platform" | "Nadzorna plosca - SPC" |

### 12.2 Meta Description

```html
<meta name="description" content="SPC Aukcije - B2B platforma za spletne drazbe kontejnerjev, klimatske opreme in gradbenih strojev. Drazite in kupujte rabljeno industrijsko opremo v srednji Evropi." />
```

English fallback:
```html
<meta name="description" content="SPC Auctions - B2B online auction platform for containers, climate equipment and construction machinery. Bid and buy used industrial equipment across Central Europe." />
```

### 12.3 Open Graph Tags

```html
<meta property="og:site_name" content="SPC Aukcije" />
<meta property="og:title" content="SPC Aukcije - Kontejnerji in oprema na drazbi" />
<meta property="og:description" content="B2B platforma za spletne drazbe kontejnerjev in industrijske opreme." />
<meta property="og:image" content="https://drazbe.spc.si/og-image.png" />
<meta property="og:type" content="website" />
<meta property="og:locale" content="sl_SI" />
<meta property="og:locale:alternate" content="hr_HR" />
<meta property="og:locale:alternate" content="de_DE" />
<meta property="og:locale:alternate" content="en_GB" />
```

### 12.4 Structured Data (JSON-LD)

```json
{
  "@context": "https://schema.org",
  "@type": "Organization",
  "name": "SPC d.o.o.",
  "alternateName": "SPC - Storitveno Prodajni Center",
  "url": "https://drazbe.spc.si",
  "logo": "https://drazbe.spc.si/logo.svg",
  "contactPoint": {
    "@type": "ContactPoint",
    "telephone": "+386-1-77-77-666",
    "contactType": "sales",
    "email": "info@spc.si",
    "areaServed": ["SI", "HR", "AT", "DE", "IT", "BA", "RS", "HU"],
    "availableLanguage": ["sl", "hr", "de", "en", "it"]
  },
  "address": {
    "@type": "PostalAddress",
    "streetAddress": "Premrlova ulica 11",
    "addressLocality": "Ljubljana",
    "postalCode": "1000",
    "addressCountry": "SI"
  },
  "vatID": "SI49185675",
  "foundingDate": "2012-03-09"
}
```

---

## 13. Keycloak & Auth Branding

### 13.1 Login Page Theme

Keycloak supports custom themes. Create an SPC theme:

```
infrastructure/keycloak/themes/spc/
  login/
    theme.properties
    resources/css/spc-login.css
    resources/img/spc-logo.svg
  email/
    theme.properties
    messages/
```

**Key customizations:**
- Background color: `#004d71` gradient or SPC hero image
- Logo: SPC logo centered above login form
- Font: Open Sans
- Button style: Pill shape, `#004d71` primary
- Footer: "SPC d.o.o. | info@spc.si | +386 1 77 77 666"

### 13.2 Email Theme

Keycloak sends emails for:
- Email verification
- Password reset
- Account updates

All should include SPC logo, brand colors, and contact information.

---

## 14. Email & Notification Branding

### 14.1 Email Templates

The notification-service sends transactional emails. All templates need:

| Element | Value |
|---------|-------|
| **From address** | `drazbe@spc.si` or `noreply@spc.si` |
| **From name** | "SPC Aukcije" |
| **Logo** | SPC logo (hosted URL, not inline) |
| **Primary color** | `#004d71` |
| **Footer** | SPC d.o.o., Premrlova ulica 11, 1000 Ljubljana, SI49185675 |
| **Unsubscribe** | Required by EU law |

### 14.2 Notification Types

| Notification | Subject Line Template |
|-------------|----------------------|
| New bid on your lot | "Nova ponudba na vasi drazbi: {lot_name}" |
| You've been outbid | "Preklicana ponudba: {lot_name}" |
| Auction won | "Cestitamo! Zmagali ste drazbo: {lot_name}" |
| Auction ending soon | "Drazba se kmalu konca: {lot_name}" |
| Lot approved | "Vas izdelek je potrjen: {lot_name}" |
| Payment received | "Placilo prejeto: {lot_name}" |

---

## 15. Social Media & External Presence

### 15.1 Current SPC Social Presence (Assessment)

| Platform | Status | Followers | Activity |
|----------|--------|-----------|----------|
| **Facebook** | Active | ~523 likes | Low frequency |
| **Instagram** | Exists | Unknown | Low engagement |
| **LinkedIn** | No company page | N/A | None |
| **Twitter/X** | Dead link in footer | N/A | None |
| **YouTube** | None | N/A | None |
| **Bolha.com** | Listed | N/A | Marketplace listings |
| **Blog** | Dormant since mid-2022 | N/A | 4 posts total |

### 15.2 Recommendations for Platform Launch

1. **Create LinkedIn company page** for SPC -- critical for B2B credibility
2. **Update Facebook** (SPContainers) with auction platform launch announcement
3. **Remove dead Twitter/YouTube links** from footer
4. **Add Facebook + Instagram links** to platform footer
5. **Consider blog revival** -- auction results, sustainability reports, market insights

### 15.3 Social Sharing Assets

Create OG images and social media banners:

| Asset | Size | Content |
|-------|------|---------|
| **OG Image** | 1200x630 | SPC logo + "Drazbe kontejnerjev in opreme" + container photo |
| **Facebook banner** | 820x312 | SPC Aukcije branding + hero container image |
| **LinkedIn banner** | 1128x191 | SPC Aukcije + "B2B Auction Platform" |
| **Instagram post** | 1080x1080 | Launch announcement template |

---

## 16. Legal & Compliance Pages

### 16.1 Required Legal Pages (EU)

| Page | Slovenian Title | Status |
|------|----------------|--------|
| **Terms of Service** | Splosni pogoji | Must create |
| **Privacy Policy** | Varovanje osebnih podatkov | Must create |
| **Cookie Policy** | Politika uporabe piskotkov | Must create |
| **Auction Terms** | Pogoji drazbene prodaje | Must create |
| **Imprint** | Pravno obvestilo | Must create |

### 16.2 Legal Entity Information (Imprint)

```
SPC, storitveno podjetje, d.o.o.
Premrlova ulica 11
1000 Ljubljana, Slovenija

Davcna stevilka: SI 49185675
Maticna stevilka: 6128076000
Vpis: Okrozno sodisce Ljubljana

Direktor: Matej Zupan
Telefon: +386 1 77 77 666
E-posta: info@spc.si

Bancni racun: OTP banka d.d.
```

### 16.3 GDPR Compliance

The platform's compliance-service already handles GDPR technically. Legal pages must reference:
- Data controller: SPC d.o.o.
- DPO contact: (SPC to designate)
- Data retention periods
- Rights under GDPR (access, rectification, erasure, portability)
- Cookie consent requirements

---

## 17. Implementation Phases

### Phase 1: Core Visual Identity (2-3 days)

| # | Task | Files | Effort |
|---|------|-------|--------|
| 1.1 | Replace color palette in `preset.ts` | `preset.ts` | 1h |
| 1.2 | Replace color palette in `tailwind.preset.ts` | `tailwind.preset.ts` | 1h |
| 1.3 | Swap fonts (Inter -> Open Sans + Raleway) | `nuxt.config.ts`, both `index.html`, `tailwind.preset.ts` | 1h |
| 1.4 | Add heading font CSS rule | CSS entry points (3 apps) | 30m |
| 1.5 | Replace logo in Navbar | `Navbar.vue` | 30m |
| 1.6 | Replace logo in Footer | `Footer.vue` | 30m |
| 1.7 | Update button border-radius to pill | `preset.ts` | 15m |
| 1.8 | Create/add favicon | All 3 apps | 30m |
| 1.9 | Visual smoke test -- verify all 3 frontends render correctly | Manual | 1h |

**Phase 1 Deliverable:** Platform looks like SPC visually (colors, fonts, logo, buttons).

### Phase 2: Content & Brand Name (1-2 days)

| # | Task | Files | Effort |
|---|------|-------|--------|
| 2.1 | Replace "EU Auction Platform" in all page titles | `nuxt.config.ts`, `index.html` x2, `app.vue`, `error.vue`, `router/index.ts` | 1h |
| 2.2 | Update footer copyright in all 7 locale files | `en.json`, `de.json`, `fr.json`, etc. | 30m |
| 2.3 | Update footer about text | `en.json` + locale files | 30m |
| 2.4 | Update social media links in Footer | `Footer.vue` | 30m |
| 2.5 | Add SPC contact info to Footer | `Footer.vue` | 30m |
| 2.6 | Update hero section copy | Locale files | 1h |
| 2.7 | Update meta description | `nuxt.config.ts` | 15m |

**Phase 2 Deliverable:** All user-visible text says "SPC" instead of "EU Auction Platform".

### Phase 3: Internationalization (3-5 days)

| # | Task | Files | Effort |
|---|------|-------|--------|
| 3.1 | Create `sl.json` (Slovenian translation, ~330 keys) | `i18n/locales/sl.json` | 4h |
| 3.2 | Create `hr.json` (Croatian translation) | `i18n/locales/hr.json` | 3h |
| 3.3 | Create `sr.json` (Serbian translation) | `i18n/locales/sr.json` | 2h |
| 3.4 | Create `hu.json` (Hungarian translation) | `i18n/locales/hu.json` | 4h |
| 3.5 | Update `nuxt.config.ts` i18n config | `nuxt.config.ts` | 30m |
| 3.6 | Update Navbar language switcher | `Navbar.vue` | 30m |
| 3.7 | Update country selector (homepage) | Homepage component | 1h |
| 3.8 | Remove unused locale files (nl, fr, pl, ro) | Delete 4 files | 15m |
| 3.9 | Set `defaultLocale: 'sl'` | `nuxt.config.ts` | 5m |

**Phase 3 Deliverable:** Platform available in Slovenian, Croatian, German, English, Italian, Serbian, Hungarian.

### Phase 4: Product Domain Customization (1-2 days)

| # | Task | Files | Effort |
|---|------|-------|--------|
| 4.1 | Configure SPC product categories in seed data | Seed scripts / catalog-service | 2h |
| 4.2 | Create SPC demo lots (containers, equipment) | Seed script | 2h |
| 4.3 | Upload SPC product photos via media-service | Seed script | 1h |
| 4.4 | Map category icons | Frontend components | 1h |
| 4.5 | Configure SPC countries in country selector | Frontend config | 30m |

**Phase 4 Deliverable:** Platform shows SPC's actual products and market.

### Phase 5: Infrastructure & Auth (1-2 days)

| # | Task | Files | Effort |
|---|------|-------|--------|
| 5.1 | Create Keycloak login theme with SPC branding | Keycloak theme files | 3h |
| 5.2 | Configure domain names | Nginx, Helm, env files | 2h |
| 5.3 | Update SSL certificates | Infrastructure | 1h |
| 5.4 | Update security headers (CSP) | Nginx config | 1h |
| 5.5 | Update Keycloak redirect URIs | Keycloak admin | 30m |

**Phase 5 Deliverable:** Full production deployment with SPC domains.

### Phase 6: Polish & Marketing (2-3 days)

| # | Task | Files | Effort |
|---|------|-------|--------|
| 6.1 | Create OG sharing images | Static assets | 2h |
| 6.2 | Add structured data (JSON-LD) | `nuxt.config.ts` / layout | 1h |
| 6.3 | Create legal pages (Terms, Privacy, Cookies) | New Vue pages | 4h |
| 6.4 | Add EBONITETE.SI / SafeSigned trust badges | Footer / homepage | 1h |
| 6.5 | Email template branding | notification-service | 2h |
| 6.6 | Create brand guidelines summary | Documentation | 2h |

**Phase 6 Deliverable:** Production-ready branded platform.

### Total Estimated Effort

| Phase | Effort | Timeline |
|-------|--------|----------|
| Phase 1: Visual Identity | ~6h | Days 1-2 |
| Phase 2: Content & Copy | ~4h | Days 2-3 |
| Phase 3: i18n | ~15h | Days 3-7 |
| Phase 4: Product Domain | ~6.5h | Days 5-6 |
| Phase 5: Infrastructure | ~7.5h | Days 6-8 |
| Phase 6: Polish | ~12h | Days 8-10 |
| **Total** | **~51h** | **~10 working days** |

---

## 18. File-by-File Change Inventory

### Design Tokens (Highest Impact -- Changes Cascade Everywhere)

| File | Changes | Impact |
|------|---------|--------|
| `frontend/shared/design-tokens/preset.ts` | Primary color scale (blue -> SPC teal), button radius (pill), card shadow | All PrimeVue components across all 3 apps |
| `frontend/shared/design-tokens/tailwind.preset.ts` | Primary color scale, font-family (Inter -> Open Sans + Raleway heading), add `font-heading` | All Tailwind utilities across all 3 apps |
| `frontend/shared/design-tokens/pt.ts` | Review for hardcoded color classes | Global component pass-through |
| `frontend/shared/design-tokens/status-severity.ts` | No changes needed | Status colors are semantic, not brand |

### Buyer Web (Nuxt 3)

| File | Changes |
|------|---------|
| `nuxt.config.ts` | Title, meta description, font links, i18n locales, default locale |
| `app.vue` | Title template "SPC Aukcije" |
| `error.vue` | Title "SPC Aukcije" |
| `components/shared/Navbar.vue` | Logo image, brand name, language switcher flags/locales |
| `components/shared/Footer.vue` | Logo, brand name, social links, contact info |
| `pages/index.vue` (or homepage component) | Country selector, category grid, hero section |
| `assets/css/main.css` | Heading font-family rule |
| `public/favicon.svg` | SPC favicon (new file) |
| `public/og-image.png` | SPC OG image (new file) |
| `i18n/locales/en.json` | Brand name references, footer copy, hero copy |
| `i18n/locales/de.json` | Brand name references |
| `i18n/locales/it.json` | Brand name references |
| `i18n/locales/sl.json` | New file -- full Slovenian translation |
| `i18n/locales/hr.json` | New file -- full Croatian translation |
| `i18n/locales/sr.json` | New file -- full Serbian translation |
| `i18n/locales/hu.json` | New file -- full Hungarian translation |
| `i18n/locales/nl.json` | Delete (not in SPC's market) |
| `i18n/locales/fr.json` | Delete |
| `i18n/locales/pl.json` | Delete |
| `i18n/locales/ro.json` | Delete |
| `tailwind.config.ts` | No changes (imports shared preset) |

### Seller Portal (Vite SPA)

| File | Changes |
|------|---------|
| `index.html` | Title, font links, favicon |
| `src/main.ts` | Keycloak realm (if renamed) |
| `src/components/layout/SidebarNav.vue` | Brand logo/name if present in layout |
| Layout component (AppLayout or similar) | SPC logo in header/sidebar |
| `src/assets/main.css` | Heading font-family rule |

### Admin Dashboard (Vite SPA)

| File | Changes |
|------|---------|
| `index.html` | Title, font links, favicon |
| `src/main.ts` | Keycloak realm (if renamed) |
| `src/router/index.ts` | Title template "SPC" |
| Layout component | SPC logo in header/sidebar |
| `src/assets/main.css` | Heading font-family rule |

### Infrastructure

| File | Changes |
|------|---------|
| `infrastructure/config/nginx/nginx-spa-prod.conf` | Server names, CSP headers |
| `docker/compose/.env` | Domain variables |
| `docker/compose/docker-compose-infrastructure.yaml` | Keycloak realm import if applicable |
| `helm/auction-platform/values.yaml` | Ingress hosts |

### Backend (Minimal Changes)

| File | Changes |
|------|---------|
| Keycloak realm export | Display name, theme, redirect URIs |
| notification-service email templates | Sender address, branding |
| Seed data scripts | SPC-specific categories, lots, users |

---

## 19. Verification Checklist

### Visual Verification

- [ ] Buyer homepage renders with SPC teal (`#004d71`) as primary color
- [ ] All buttons are pill-shaped (9999px border-radius)
- [ ] Headings use Raleway font
- [ ] Body text uses Open Sans font
- [ ] SPC logo appears in Navbar (buyer, seller, admin)
- [ ] SPC logo appears in Footer (buyer)
- [ ] Favicon shows SPC icon in browser tab (all 3 apps)
- [ ] No blue `#2563eb` remnants visible anywhere
- [ ] Dark mode (if enabled) uses SPC teal variants correctly

### Content Verification

- [ ] "EU Auction Platform" appears NOWHERE in the UI
- [ ] Page titles say "SPC Aukcije" (all 3 apps)
- [ ] Footer copyright says "SPC d.o.o."
- [ ] Footer social links point to SPC's actual profiles
- [ ] Footer includes SPC contact info (phone, email, address)
- [ ] Meta description references SPC and containers

### i18n Verification

- [ ] Default language is Slovenian
- [ ] Language switcher shows SI, HR, DE, GB, IT, RS, HU flags
- [ ] Slovenian translation is complete (no English fallback keys visible)
- [ ] Switching to Croatian works without errors
- [ ] Switching to German works (existing translation)
- [ ] Switching to English works (existing translation)
- [ ] Country selector shows SPC's target markets

### Functional Verification

- [ ] Login via Keycloak works with SPC-branded login page
- [ ] Lot creation flow works with SPC product categories
- [ ] Search returns SPC demo lots
- [ ] Bidding flow works end-to-end
- [ ] Seller portal loads with SPC branding
- [ ] Admin dashboard loads with SPC branding
- [ ] CO2 report generates with SPC branding

### Technical Verification

- [ ] `npm run build` succeeds for all 3 frontends
- [ ] No TypeScript errors from font/color changes
- [ ] PrimeVue components render correctly (no style conflicts)
- [ ] CSS layer order maintained: `tailwind-base, primevue, tailwind-utilities`
- [ ] No console errors in browser dev tools

---

## Appendix A: SPC Color Palette Quick Reference

```css
/* SPC Brand Colors -- Production Ready */
:root {
  /* Primary Teal Scale */
  --spc-50:  #e6f2f8;
  --spc-100: #b3d9e8;
  --spc-200: #80bfd8;
  --spc-300: #4da6c8;
  --spc-400: #268db5;
  --spc-500: #0077a3;
  --spc-600: #006a92;
  --spc-700: #004d71;   /* PRIMARY */
  --spc-800: #003e5a;   /* HOVER */
  --spc-900: #002f44;
  --spc-950: #001f2e;

  /* Neutrals (keep platform defaults) */
  --spc-text: #6C6C6C;
  --spc-heading: #000000;
  --spc-light-bg: #E7E7E7;
  --spc-white: #FFFFFF;

  /* Semantic (unchanged) */
  --spc-success: #16a34a;
  --spc-warning: #d97706;
  --spc-danger: #dc2626;
}
```

## Appendix B: SPC Typography Quick Reference

```css
/* SPC Typography */
@import url('https://fonts.googleapis.com/css2?family=Open+Sans:wght@400;500;600;700&family=Raleway:wght@500;600;700&display=swap');

body {
  font-family: 'Open Sans', system-ui, -apple-system, sans-serif;
  font-size: 16px;
  line-height: 1.75;
  color: #6C6C6C;
}

h1, h2, h3, h4, h5, h6 {
  font-family: 'Raleway', 'Open Sans', system-ui, sans-serif;
  color: #000000;
}
```

## Appendix C: Research Sources

### Company Information
- [SPC Main Website](https://spc.si/)
- [SPC Online Shop](https://shop.spc.si/)
- [SPC About Page](https://shop.spc.si/o-nas/)
- [SPC Facebook](https://facebook.com/SPContainer)
- [SPC Instagram](https://instagram.com/spcontainers)

### Business Registry & Financial Data
- [Bizi.si - SPC d.o.o.](https://www.bizi.si/SPC-D-O-O/)
- [CompanyWall - SPC d.o.o.](https://www.companywall.si/podjetje/spc-doo/MMEIStER)
- [Stop Neplacniki](https://www.stop-neplacniki.si/spc-doo/)
- [MojMojster.net - SPC](https://www.mojmojster.net/spc_storitveno_podjetje)

### Branding Best Practices
- [Nicole Steffen Design - Rebrand Checklist](https://nicolesteffen.com/2026/02/03/rebrand-checklist-across-50-touchpoints/)
- [Elementor - White Labeling Guide](https://elementor.com/blog/white-labeling/)
- [B2B International - Brand Trends 2026](https://www.b2binternational.com/publications/b2b-brand-trends-2026/)
- [Huddle Creative - B2B Branding Guide 2025](https://www.huddlecreative.com/blog/b2b-branding-guide-2025)
- [PrimeVue Styled Theming](https://primevue.org/theming/styled/)
- [Tailwind CSS Theme Variables](https://tailwindcss.com/docs/theme)

### Competitor Landscape
- [Arcont d.d.](https://www.arcont.si/)
- [CONTAINEX](https://www.containex.com/)
- [Europages - Container Companies Slovenia](https://www.europages.co.uk/companies/slovenia/containers.html)

---

*Document prepared: March 2026*
*Based on extensive web research, codebase analysis, and industry best practices.*
