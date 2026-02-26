---
name: test-happy-path
description: Run happy path tests for a given role (buyer/seller/admin/broker/all) using Chrome DevTools MCP, following HAPPY_PATHS.md
user-invocable: true
disable-model-invocation: false
---

# Happy Path Testing Skill

Run browser-based happy path tests for the EU Auction Platform. Uses Chrome DevTools MCP for browser roles and Bash curl for the broker API-only path.

**Argument:** `$ARGUMENTS` — one of `buyer`, `seller`, `admin`, `broker`, or `all` (default: `all`)

---

## Step 1: Parse Arguments

Determine which role(s) to test from `$ARGUMENTS`:

- `buyer` — run only the Buyer Happy Path (Section 1 of HAPPY_PATHS.md)
- `seller` — run only the Seller Happy Path (Section 2)
- `admin` — run only the Admin Happy Path (Section 3)
- `broker` — run only the Broker Happy Path (Section 4, API-only via curl)
- `all` or empty — run all four in order: buyer → seller → admin → broker

Store the list of roles to test. For each role, you will produce a separate report file.

---

## Step 2: Pre-flight — Start the Full Stack

Use the `/run-full-stack` skill to start all infrastructure, backend services, and frontends. This ensures everything is running before testing.

Then verify the required services are reachable. Use **Bash curl** to check each URL:

| Component | URL | Required For |
|-----------|-----|--------------|
| Buyer Web | http://localhost:3000 | buyer |
| Seller Portal | http://localhost:5174 | seller |
| Admin Dashboard | http://localhost:5175 | admin |
| Gateway API | http://localhost:8080/q/health | buyer, seller, admin, broker |
| Keycloak | http://localhost:8180/realms/auction-platform | all |

```bash
# Example health check
curl -sf -o /dev/null -w "%{http_code}" http://localhost:3000
```

If a required service is **not running** after `/run-full-stack`, report it and **skip that role** (mark all steps as SKIP with reason "Service not running"). Continue with other roles.

Create the screenshots output directory:
```bash
mkdir -p tests/test-screenshots
```

---

## Step 3: Read the Canonical Test Plan

Read the file `HAPPY_PATHS.md` at the project root. This is the single source of truth for all test steps. Each section (1–4) maps to a role. Follow the numbered steps and **Verify** checkpoints exactly as written.

---

## Step 4: Execute Tests Per Role

### General Testing Protocol

For **each step** in the happy path:

1. **Execute** the action using the appropriate tool:
   - Browser roles (buyer/seller/admin): Use Chrome DevTools MCP tools (`navigate_page`, `take_snapshot`, `click`, `fill`, `fill_form`, `wait_for`)
   - Broker: Use Bash curl commands as specified in HAPPY_PATHS.md Section 4

2. **At every "Verify" checkpoint:**
   - Take a page snapshot via `take_snapshot` — check for expected elements
   - Check console for errors via `list_console_messages` (filter types: `["error", "warn"]`)
   - Check network for failed requests via `list_network_requests` — look for 4xx/5xx status codes
   - Take a screenshot via `take_screenshot` and save to: `tests/test-screenshots/<role>-<step>-<short-name>.png`
     - Example: `tests/test-screenshots/buyer-1.1-landing-page.png`

3. **Record the result** for each step: **PASS**, **FAIL**, **PARTIAL**, or **SKIP**
   - **PASS**: All verify checkpoints met, no console errors, no network failures
   - **PARTIAL**: Main functionality works but has console errors, network failures, or minor visual issues
   - **FAIL**: Verify checkpoint not met, page crashes, or critical functionality broken
   - **SKIP**: Precondition not met (service down, depends on failed prior step)

### 4a: Buyer Happy Path (Browser — http://localhost:3000)

**Test credentials:** buyer@test.com / password123

Follow HAPPY_PATHS.md Section 1, steps 1.1 through 1.7:

| Step | Name | Actions |
|------|------|---------|
| 1.1 | Landing Page | Navigate to localhost:3000. Verify hero, search bar, featured lots, navbar, footer |
| 1.2 | Authentication | Click Login → Keycloak → enter credentials → verify redirect back, navbar shows user menu |
| 1.3 | Browse & Search | Use search bar → verify results page, lot cards, filters |
| 1.4 | View Lot Detail | Click a lot card → verify detail page fields, bid panel |
| 1.5 | Place a Bid | Enter bid amount on an ACTIVE lot, click Place Bid → verify confirmation |
| 1.6 | Navigation Pages | Visit My Bids, Watchlist, Profile from user menu |
| 1.7 | Logout | Click avatar → Logout → verify redirect, Login button reappears |

### 4b: Seller Happy Path (Browser — http://localhost:5174)

**Test credentials:** seller@test.com / password123

Follow HAPPY_PATHS.md Section 2, steps 2.1 through 2.11:

| Step | Name | Actions |
|------|------|---------|
| 2.1 | Authentication | Navigate to localhost:5174 → Keycloak login → verify dashboard |
| 2.2 | Dashboard | Verify KPI cards, recent activity, quick actions, status overview |
| 2.3 | Create a New Lot | Fill lot form (Title: "Caterpillar D6 Bulldozer 2019", Brand: "Caterpillar", Starting Bid: 45000, etc.) → verify success |
| 2.4 | Submit for Review | Click Submit for Review → verify status changes to PENDING_REVIEW |
| 2.5 | My Lots Page | Verify paginated list, lot details, pagination |
| 2.6 | Edit a Lot | Edit a field, save → verify changes reflected |
| 2.7 | Settlements | Navigate to Settlements → verify page loads |
| 2.8 | Analytics | Navigate to Analytics → verify page loads |
| 2.9 | CO2 Report | Navigate to CO2 Report → verify page loads |
| 2.10 | Profile | Navigate to Profile → verify form tabs |
| 2.11 | Logout | Logout → verify session ended |

### 4c: Admin Happy Path (Browser — http://localhost:5175)

**Test credentials:** admin@test.com / password123

Follow HAPPY_PATHS.md Section 3, steps 3.1 through 3.10:

| Step | Name | Actions |
|------|------|---------|
| 3.1 | Authentication | Navigate to localhost:5175 → Keycloak login → verify dashboard |
| 3.2 | Dashboard Overview | Verify sidebar navigation links |
| 3.3 | Lot Approval | Navigate to Lot Approval → approve a pending lot → verify status change |
| 3.3b | Create Auction | Navigate to Auctions → Create Auction → select approved lot → set times → submit → verify lot becomes ACTIVE |
| 3.4 | Auction Management | Navigate to Auctions → verify table with created auction, filters, Create Auction button |
| 3.5 | User Management | Navigate to Users → verify page loads |
| 3.6 | Payment Management | Navigate to Payments → verify page loads |
| 3.7 | Fraud Detection | Navigate to Fraud Detection → verify table and filters |
| 3.8 | GDPR Compliance | Navigate to GDPR → verify table and action buttons |
| 3.9 | Analytics | Navigate to Analytics → verify KPIs and charts |
| 3.10 | System Health | Navigate to System Health → verify service health cards |

### 4d: Broker Happy Path (API-only — via curl)

**Test credentials:** broker@test.com / password123

Follow HAPPY_PATHS.md Section 4, steps 4.1 through 4.4. Use Bash curl commands:

```bash
# 4.1 Get token
TOKEN=$(curl -s -X POST "http://localhost:8180/realms/auction-platform/protocol/openid-connect/token" \
  -d "client_id=broker-app" \
  -d "grant_type=password" \
  -d "username=broker@test.com" \
  -d "password=password123" | jq -r '.access_token')

# 4.2 Dashboard
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/brokers/me/dashboard

# 4.3 Lead Management
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/brokers/leads

# 4.4 Lot Intake (if test data available)
# curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
#   http://localhost:8080/api/v1/brokers/lots/intake -d '{...}'
```

Verify HTTP status codes and response shapes match HAPPY_PATHS.md expectations.

---

## Step 5: Compile Bug Report

For **each failure or partial pass**, create a bug entry with the following format (matching existing reports in `tests/`):

### Bug Entry Template

```markdown
### BUG-<ROLE><NUMBER>: <Short title>

- **Step:** <step reference, e.g., "2.3 Create a New Lot">
- **Severity:** <Critical | Major | Minor | Info>
- **Status:** OPEN
- **Description:** <What happened vs what was expected>
- **Root Cause:** <Analysis of why it failed — check source code if possible>
- **File:** <Affected source file(s) with path>
- **Console Error:** <Exact console error text, or "None">
- **Network:** <Failed request details: method, URL, status code, response body snippet>
```

**Severity definitions:**
- **Critical**: Blocks the happy path flow entirely (can't proceed to next step)
- **Major**: Incorrect behavior, data wrong, or significant functionality broken (flow continues)
- **Minor**: Cosmetic, UX, or non-blocking issue
- **Info**: Accessibility warnings, suggestions, or spec mismatches

---

## Step 6: Write the Report

Write the report to: `tests/<role>-happy-path-report-<YYYY-MM-DD>.md`

Use today's date. For `all` mode, write one report per role.

### Report Template

```markdown
# <Role> Happy Path — Test Report

**Date:** <YYYY-MM-DD>
**Tester:** Claude Code (automated via Chrome DevTools MCP)
**Application:** <App name> (<URL>)
**Environment:** <List running services and ports>

---

## Summary

| Severity | Count |
|----------|-------|
| Critical | <n> |
| Major | <n> |
| Minor | <n> |
| Info | <n> |
| **Total** | **<n>** |

**Overall Result:** <X of Y happy path steps pass. Brief summary.>

---

## Happy Path Step Results

| Step | Test | Result | Notes |
|------|------|--------|-------|
| <ref> | <name> | <PASS/FAIL/PARTIAL/SKIP> | <brief note> |
| ... | ... | ... | ... |

---

## Critical Bugs

<bug entries with severity=Critical>

## Major Bugs

<bug entries with severity=Major>

## Minor Bugs

<bug entries with severity=Minor>

## Info / Accessibility Issues

<bug entries with severity=Info>

---

## Network Request Failures Summary

| Endpoint | Status | Page(s) Affected |
|----------|--------|------------------|
| ... | ... | ... |

---

## Console Errors Summary

| Error | Page(s) | Count |
|-------|---------|-------|
| ... | ... | ... |

---

## Screenshots

All test screenshots saved to: `tests/test-screenshots/`

| File | Description |
|------|-------------|
| `<role>-<step>-<name>.png` | <description> |
| ... | ... |
```

---

## Step 7: Teardown

Use the `/kill-all` skill to stop and remove all services, kill frontend dev servers, and close any Chrome DevTools browser. Do **not** pass the `volumes` argument — volumes should persist for future test runs.

---

## Important Notes

- **Do NOT fix bugs during the test run.** Only observe and report. Fixing is a separate task.
- **Take screenshots liberally.** At minimum: one per happy path step, plus one per bug found.
- **Check console and network after every navigation.** Many bugs only manifest as silent API failures.
- **If a step depends on a prior step that failed**, mark it as SKIP with the reason.
- **For the broker path**, save curl output as evidence instead of screenshots.
- **Cross-reference with known issues** in HAPPY_PATHS.md "Known Issues" section — note if a bug is pre-existing vs new.
- **Keycloak login flow**: After navigating to the app, wait for Keycloak redirect. Use `fill_form` to enter credentials, then `click` the Sign In button. Use `wait_for` to confirm redirect back to the app.
