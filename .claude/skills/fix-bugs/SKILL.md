---
name: fix-bugs
description: Fix all bugs from every happy path bug report in tests/, oldest first
user-invocable: true
---

# Fix Bugs — Process All Reports

Iterate through all bug reports in `tests/`, oldest first. For each report: fix every OPEN bug, delete the report, then move to the next one. Repeat until the folder has no reports left.

**Argument:** `$ARGUMENTS` — optional role filter (`buyer`, `seller`, `admin`, `broker`). If empty, processes all reports.

---

## Step 1: Clean Slate — Kill Frontend and Browser Only

Kill any running frontend dev servers and close browser pages. Do NOT tear down Docker backend services — they should stay running for API verification during fixes.

```bash
# Kill frontend dev servers by port
for port in 3000 3001 3002 5174 5175 5176 5177; do
  pid=$(lsof -ti :$port 2>/dev/null)
  if [ -n "$pid" ]; then kill -9 $pid 2>/dev/null && echo "Killed PID $pid on port $port"; fi
done
echo "Frontend dev servers killed"
```

Close any open Chrome DevTools MCP browser pages:
1. Call `mcp__chrome-devtools__list_pages` — if connected, close all pages except the last, then navigate the last to `about:blank`
2. If not connected, skip (no browser to clean up)

**Do NOT call `/kill-all`** — this would tear down Docker backend services that may be needed for API verification. Do NOT call `/run-full-stack` — the fix skill does not need frontends or browser.

---

## Step 2: Ensure Docker Backend Services Are Running

Check if Docker backend services are running. If not, start them:
```bash
running=$(docker ps --filter "name=auction-platform" --format "{{.Names}}" 2>/dev/null | wc -l)
if [ "$running" -lt 10 ]; then
  echo "Backend services not running, starting..."
  cd /home/radionica/Radionica/Tradex/Tradex/eu-auction-platform && \
  docker compose -f docker/compose/docker-compose-infrastructure.yaml --env-file docker/compose/.env up -d && \
  docker compose -f docker/compose/docker-compose-full.yaml --env-file docker/compose/.env up -d --build
else
  echo "Backend services already running ($running containers)"
fi
```

Do NOT start frontend dev servers — the fix skill only reads/edits code and verifies builds.

---

## Step 3: Build the Report Queue

List all bug/report markdown files in `tests/`, sorted **oldest first** (by modification time):

```bash
ls -tr tests/*bugs*.md tests/*report*.md 2>/dev/null
```

If `$ARGUMENTS` specifies a role (e.g., `seller`), filter to only that role's reports:
```bash
ls -tr tests/*${ROLE}*bugs*.md tests/*${ROLE}*report*.md 2>/dev/null
```

If no reports are found, inform the user and stop — there is nothing to fix.

Store the full ordered list of report file paths. This is the **report queue**.

---

## Step 4: Process Reports in a Loop

For each report in the queue (oldest first), execute Steps 4a–4i, then move to the next report. Continue until the queue is empty.

### 4a: Read and Parse the Report

Read the full report file. Parse out every bug entry (sections matching `### BUG-*`) with status `OPEN`. Create a task list from these bugs ordered by severity:
1. **Critical** — these block the entire flow
2. **Major** — these break significant functionality
3. **Minor** — cosmetic or non-blocking issues
4. **Info** — accessibility and spec mismatches

### 4b: Understand Each Bug
- Read the bug's **Description**, **Root Cause**, **File**, and **Fix Required** fields from the report
- Read the referenced source file(s) to understand the current code
- If the report specifies exact line numbers, verify they still match (code may have shifted)

### 4c: Read CONVENTIONS.md

Before implementing any fix, read `CONVENTIONS.md` at the project root. This is the mandatory code style and architectural guide. All fixes MUST follow its rules, including:
- **Kotlin style** (section 4): naming, CDI patterns, logging, configuration
- **Vue/TypeScript** (section 3): SFC ordering, composables, PrimeVue patterns, API layer
- **REST API** (section 5): response wrapping, pagination, status codes
- **Database** (section 7): naming, Flyway migrations, column types
- **Critical gotchas** (section 16): known pitfalls that caused real bugs

### 4d: Implement the Fix
- Apply the minimal fix described in the report
- If the report doesn't specify an exact fix, analyze the root cause and implement the simplest correct solution
- Follow `CONVENTIONS.md` rules and existing code patterns in the file

### 4e: Skip Criteria
Skip a bug (do not attempt to fix) if:
- It is caused by a **service being down** (infrastructure issue, not a code bug) — e.g., "service X is not running"
- It was already marked as `FIXED during test` in the report
- It requires **infrastructure changes** that can't be done via code (e.g., Keycloak admin API calls at runtime)

### 4f: What Counts as a Fixable Bug
Focus on bugs that involve:
- **Backend code fixes** — SQL queries, repository methods, service logic, API endpoints
- **Frontend code fixes** — Vue components, composables, templates, API calls
- **Configuration fixes** — application.yml, realm JSON, Flyway migrations
- **Form/UI fixes** — validation, data binding, display logic

### 4g: Build Verification for This Report

After all bugs in the current report are fixed, do a quick build check to catch compile errors:

**Backend (if backend files were changed):**
```bash
cd /home/radionica/Radionica/Tradex/Tradex/eu-auction-platform && ./gradlew build -x test 2>&1 | tail -20
```

**Frontend (if frontend files were changed):**
For each affected frontend app:
```bash
cd /home/radionica/Radionica/Tradex/Tradex/eu-auction-platform/frontend/<app> && npx vue-tsc --noEmit 2>&1 | tail -20
```

If build fails, fix the compilation error before proceeding.

### 4h: Delete the Processed Report

Once all fixable bugs in this report have been addressed and the build passes, delete the report file:

```bash
rm tests/<report-filename>.md
```

Also clean up any test screenshots referenced by that report if they exist in `tests/test-screenshots/`:
```bash
rm -f tests/test-screenshots/<role>-*.png
```

### 4i: Check for Remaining Reports

```bash
ls -tr tests/*bugs*.md tests/*report*.md 2>/dev/null
```

If more reports remain, **loop back to Step 4a** with the next (oldest) report. If none remain, proceed to Step 5.

---

## Step 5: Final Cleanup

Do NOT call `/kill-all` — leave Docker backend services running for the next test phase. Only ensure no stale frontend processes or browser pages remain.

Verify the `tests/` folder has no remaining report files:
```bash
ls tests/*bugs*.md tests/*report*.md 2>/dev/null | wc -l
```

Report the total number of reports processed and bugs fixed.

---

## Important Notes

- **Do not introduce new bugs.** Keep fixes minimal and targeted.
- **Do not refactor surrounding code.** Only change what's needed to fix the reported bug.
- **Follow CONVENTIONS.md.** All fixes must adhere to the project's code conventions. Read it in Step 4c before implementing any fix.
- **If a fix requires a database migration**, create a new Flyway migration file with the next version number in sequence. Follow section 7 of CONVENTIONS.md (snake_case, `app` schema, `IF NOT EXISTS` guards).
- **If a fix is unclear**, read more context from the codebase before implementing. Check related files, tests, and API contracts.
- **If implementing a new feature** as part of a fix (e.g., adding a missing endpoint), follow the architectural patterns in CONVENTIONS.md: hexagonal architecture (section 4.1), API conventions (section 5), PrimeVue patterns (section 3.11).
- **Track progress** using the task list — mark each bug as in_progress when starting and completed when done.

### Context Management
- When running as a subagent (called from test-fix-loop via Task tool), write a summary of all fixes to `tests/.fix-summary.md` before completing. This allows the parent loop to read the summary without needing the full conversation context.
- Keep file reads targeted — only read the specific sections of files that need fixing, not entire large files unless necessary.
