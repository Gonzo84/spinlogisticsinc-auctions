# UX Improvements - Per Page Recommendations

## buyer-web (Public Marketplace)

### Homepage (`/`)

**Current Issues:**
- Hero title clips on mobile ("Buy Industrial Equipment at Auct...")
- Country browser uses ugly horizontal scrollbar on mobile
- Featured Auctions and Newly Listed sections render empty with no data
- No clear value proposition or trust signals
- No visible CTA for registration
- Font (Inter) not loaded via Google Fonts or @font-face

**Recommendations:**
1. **Hero title**: Use responsive text sizing (`text-3xl sm:text-4xl lg:text-5xl`) with word-break handling
2. **Country browser**: Replace horizontal scrollbar with PrimeVue `<Carousel>` or `<ScrollPanel>` with smooth navigation arrows
3. **Empty states**: Add illustrated empty states ("No featured auctions right now. Browse categories below.") with CTA buttons
4. **Trust signals**: Add "X+ machines sold", "Y countries", "Z verified sellers" stats bar below hero
5. **Registration CTA**: Prominent "Start Bidding - Register Free" button in hero
6. **Font loading**: Add `<link>` to Google Fonts in `nuxt.config.ts` head configuration
7. **CO2 Counter**: Add skeleton while loading, add tooltip explaining methodology
8. **Categories**: Show lot count per category, add "View all categories" link

### Search Page (`/search`)

**Current Issues:**
- Skeleton loaders are good (keep them)
- No clear indication of search result count
- Filter sidebar has no "Apply" / "Clear All" visual hierarchy
- No map view option (industrial equipment is location-sensitive)
- Grid/list toggle exists but may not be discoverable

**Recommendations:**
1. **Result count**: Add "Showing X of Y lots" header with sort control
2. **Active filters**: Show applied filters as chips above results with "X" to remove
3. **Filter UX**: Add "Clear all filters" button at top of sidebar; use PrimeVue `<Accordion>` for collapsible filter sections
4. **Sort**: Use PrimeVue `<Select>` for sort dropdown with options: "Closing Soonest", "Newly Listed", "Price: Low to High", "Price: High to Low", "Most Bids"
5. **Grid/List toggle**: Make more prominent with icon buttons using `<SelectButton>` component
6. **Empty search**: Show "No lots match your filters. Try adjusting your search." with filter suggestions
7. **Infinite scroll or pagination**: Add PrimeVue `<Paginator>` at bottom
8. **Save search**: "Save this search" button for registered users

### Lot Detail (`/lots/[id]`)

**Current Issues:**
- No breadcrumb navigation (can't go back to search results with filters)
- No image gallery (single image)
- Bid panel needs visual hierarchy improvement
- No "similar lots" section
- No sharing functionality

**Recommendations:**
1. **Breadcrumb**: Add `<Breadcrumb>` with: Home > Category > Lot Title
2. **Image gallery**: Use PrimeVue `<Galleria>` with thumbnails, fullscreen zoom
3. **Bid panel redesign**:
   - Current bid: Large, prominent, animated on change
   - Timer: Color-coded with clear "Extended!" indicator
   - Bid input: Pre-filled with minimum, increment buttons (-, +)
   - Auto-bid toggle: Collapsible section below manual bid
   - Bid history: Expandable table or `<Timeline>` component
4. **Lot information tabs**: Use PrimeVue `<Tabs>` for: Description, Specifications, Location, Bid History, Shipping
5. **Similar lots**: "You might also like" carousel at bottom
6. **Social sharing**: Share button with link copy
7. **Watchlist**: Heart icon with toast confirmation
8. **Seller info**: Mini card with seller name, rating, verified badge

### My Bids / Watchlist / Purchases (`/my/*`)

**Recommendations:**
1. Replace custom tables with PrimeVue `<DataTable>` with:
   - Status column using `<Tag>` component
   - Quick actions column (view lot, increase bid)
   - Sort by closing time, bid amount, status
2. Add `<Tabs>` navigation between Active Bids / Won / Lost / Watchlist
3. Empty state illustrations for each section
4. Quick-bid action from bid list (inline `<InputNumber>` + button)

### Registration (`/auth/register`)

**Recommendations:**
1. Multi-step wizard using PrimeVue `<Stepper>`:
   - Step 1: Account type (Buyer / Seller)
   - Step 2: Company info (name, VAT, country)
   - Step 3: Contact details
   - Step 4: Verification
2. Inline validation with Zod schema
3. Country select with flag icons using `<Select>` with templates
4. VAT number validation with real-time EU VIES check
5. Password strength indicator

---

## seller-portal (Seller Dashboard)

### Dashboard (`/`)

**Current Issues:**
- KPI cards are functional but lack visual distinctiveness
- Revenue chart shows placeholder data styling
- Activity feed could be more interactive
- No quick actions

**Recommendations:**
1. **KPI cards**: Add trend arrows (up/down) with `<Tag>` severity colors; add sparkline mini-charts
2. **Quick actions bar**: "Create New Lot", "View Pending Approvals", "Download Report" buttons
3. **Activity feed**: Use PrimeVue `<Timeline>` with lot thumbnails, clickable items
4. **Chart improvements**: Add period selector (7d, 30d, 90d, 1y) using `<SelectButton>`
5. **Status overview**: Donut chart showing lot status distribution (draft/pending/active/sold)

### Lot Management (`/lots`)

**Current Issues:**
- Tab-based filtering is ok but limited
- Table pagination could be more robust
- Bulk actions need better UX
- No column customization

**Recommendations:**
1. Replace with PrimeVue `<DataTable>` with:
   - `lazy` server-side pagination
   - `filterDisplay="row"` inline filters
   - Sortable columns
   - Row selection checkboxes for bulk actions
   - Column toggle menu (hide/show columns)
2. **Status filter**: Use `<SelectButton>` or `<Tabs>` above table
3. **Bulk actions toolbar**: `<Toolbar>` that appears when rows selected: "Submit for Review", "Delete Draft", "Export"
4. **Quick edit**: Inline editing for title, starting price (double-click or edit icon)
5. **Lot thumbnails**: Show small image in first column

### Lot Creation/Edit (`/lots/create`, `/lots/:id/edit`)

**Current Issues:**
- Long form without sections
- Manual validation with basic error messages
- Image upload works but could be smoother

**Recommendations:**
1. **Multi-step form**: Use PrimeVue `<Stepper>`:
   - Step 1: Basic Info (title, description, category)
   - Step 2: Specifications (key-value pairs with `<InputGroup>`)
   - Step 3: Pricing (starting bid, reserve, buy-now)
   - Step 4: Location (country, city, address)
   - Step 5: Images (upload with `<FileUpload>`)
   - Step 6: Review & Submit
2. **Validation**: Zod schema with real-time field validation via `@primevue/forms`
3. **Rich text**: PrimeVue `<Editor>` for lot description
4. **Category select**: `<TreeSelect>` for hierarchical categories
5. **Image upload**: PrimeVue `<FileUpload>` with preview, reorder (drag-drop), crop
6. **Save draft**: Auto-save with "Draft saved" toast notification
7. **Preview**: "Preview as buyer" button showing lot card rendering

### Settlements (`/settlements`)

**Recommendations:**
1. PrimeVue `<DataTable>` with lazy loading and status filters
2. **Summary cards**: Total earned, pending, processing, paid (with time period filter)
3. **Invoice download**: `<Button>` with `pi pi-download` icon in action column
4. **Export**: CSV/PDF export from DataTable
5. **Date range filter**: PrimeVue `<DatePicker>` with range mode

### Analytics (`/analytics`)

**Recommendations:**
1. **Period selector**: `<SelectButton>` for time range (7d, 30d, 90d, 1y, custom)
2. **KPI cards**: With trend comparison (vs previous period)
3. **Chart variety**: Keep bar + doughnut, add line chart for trends
4. **Category breakdown**: PrimeVue `<DataTable>` with percentage bars
5. **Export**: "Download Report" button generating PDF summary

### Profile (`/profile`)

**Recommendations:**
1. Use PrimeVue `<Tabs>` for section navigation
2. **Company details**: Proper form with validation, country `<Select>` with flags
3. **Bank settings**: Masked IBAN display, secure edit flow with confirmation
4. **Notification preferences**: `<ToggleSwitch>` for each notification type
5. **Avatar upload**: `<FileUpload>` with `<Avatar>` preview

---

## admin-dashboard (Admin Control Panel)

### Dashboard (`/`)

**Current Issues:**
- KPI cards are basic
- Live bid chart functional but could show more context
- No quick action shortcuts

**Recommendations:**
1. **System status bar**: Green/amber/red indicators for each microservice health
2. **KPI cards**: Active auctions, pending approvals, today's bids, flagged items
3. **Live activity feed**: PrimeVue `<Timeline>` showing real-time platform events
4. **Alert panel**: Unresolved fraud alerts, pending GDPR requests (with severity badges)
5. **Quick actions**: "Approve Lots", "Review Fraud", "Create Auction" shortcut buttons

### Auction Management (`/auctions`)

**Recommendations:**
1. PrimeVue `<DataTable>` with full lazy mode:
   - Status `<Tag>`, bid count `<Badge>`, countdown timer
   - Row expansion for lot list preview
   - Multi-select for bulk operations (close, cancel)
   - Column filters (status, date range, category)
2. **Create auction**: PrimeVue `<Dialog>` or `<Stepper>` wizard
3. **Quick status change**: Inline action buttons (approve, close, cancel) with `<ConfirmDialog>`

### Lot Approval (`/lots/approval`)

**Current Issues:**
- Expandable cards are ok but slow for bulk review
- No side-by-side comparison

**Recommendations:**
1. **Split view**: List on left, detail preview on right (using CSS grid `lg:grid-cols-3`)
2. **Quick actions**: Approve/reject buttons always visible
3. **Batch approve**: Select multiple lots and approve in one action
4. **Image preview**: `<Galleria>` for lot images in detail panel
5. **Seller context**: Link to seller profile, previous lot history

### User Management (`/users`)

**Recommendations:**
1. PrimeVue `<DataTable>` with:
   - Avatar + name column
   - Role badges using `<Tag>`
   - Status indicator (active/blocked/pending KYC)
   - Action menu (`<Menu>` popup): View, Block, Trigger GDPR Export
2. **User detail**: `<Tabs>` for Profile, KYC History, Bid Activity, Payments
3. **Search**: Global text search + role filter + status filter

### Fraud Detection (`/fraud`)

**Recommendations:**
1. **Severity-sorted list**: PrimeVue `<DataTable>` with severity column (critical/high/medium/low)
2. **Alert detail drawer**: `<Drawer>` opening from right with full alert context
3. **Actions**: Investigate, Resolve, Dismiss, Escalate buttons
4. **Timeline**: Event timeline for each alert showing investigation steps
5. **Charts**: Fraud trend line chart, category breakdown pie chart

### GDPR Requests (`/gdpr`)

**Recommendations:**
1. **Request table**: PrimeVue `<DataTable>` with request type, user, status, SLA countdown
2. **SLA timer**: Color-coded countdown (green > 5 days, amber 2-5 days, red < 2 days)
3. **Actions**: Approve/reject with mandatory comment using `<Dialog>` + `<Textarea>`
4. **Audit log**: Expandable row showing all actions taken on request

### System Health (`/system`)

**Recommendations:**
1. **Service grid**: Cards for each microservice showing status, latency, error rate
2. **Uptime chart**: Line chart showing 24h uptime per service
3. **Recent errors**: `<DataTable>` with error logs, severity, timestamp
4. **Resource meters**: `<ProgressBar>` for CPU, memory, disk usage
