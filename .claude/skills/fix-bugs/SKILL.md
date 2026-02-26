---
name: fix-bugs
description: Fix all bugs from every happy path bug report in tests/, oldest first
user-invocable: true
---

# Fix Bugs — Process All Reports

Iterate through all bug reports in `tests/`, oldest first. For each report: fix every OPEN bug, delete the report, then move to the next one. Repeat until the folder has no reports left.

**Argument:** `$ARGUMENTS` — optional role filter (`buyer`, `seller`, `admin`, `broker`). If empty, processes all reports.

---

## Step 1: Clean Slate — Kill Everything

Call `/kill-all` to stop all services, frontends, and browser. This ensures a fresh, unpolluted state before making code changes.

---

## Step 2: Start the Full Stack

Call `/run-full-stack` to start all infrastructure, backend services, and frontends. Having the stack running allows you to verify fixes against the live application and inspect API responses during debugging.

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

For each report in the queue (oldest first), execute Steps 4a–4h, then move to the next report. Continue until the queue is empty.

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

### 4c: Implement the Fix
- Apply the minimal fix described in the report
- If the report doesn't specify an exact fix, analyze the root cause and implement the simplest correct solution
- Follow existing code patterns and conventions in the file

### 4d: Skip Criteria
Skip a bug (do not attempt to fix) if:
- It is caused by a **service being down** (infrastructure issue, not a code bug) — e.g., "service X is not running"
- It was already marked as `FIXED during test` in the report
- It requires **infrastructure changes** that can't be done via code (e.g., Keycloak admin API calls at runtime)

### 4e: What Counts as a Fixable Bug
Focus on bugs that involve:
- **Backend code fixes** — SQL queries, repository methods, service logic, API endpoints
- **Frontend code fixes** — Vue components, composables, templates, API calls
- **Configuration fixes** — application.yml, realm JSON, Flyway migrations
- **Form/UI fixes** — validation, data binding, display logic

### 4f: Build Verification for This Report

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

### 4g: Delete the Processed Report

Once all fixable bugs in this report have been addressed and the build passes, delete the report file:

```bash
rm tests/<report-filename>.md
```

Also clean up any test screenshots referenced by that report if they exist in `tests/test-screenshots/`:
```bash
rm -f tests/test-screenshots/<role>-*.png
```

### 4h: Check for Remaining Reports

```bash
ls -tr tests/*bugs*.md tests/*report*.md 2>/dev/null
```

If more reports remain, **loop back to Step 4a** with the next (oldest) report. If none remain, proceed to Step 5.

---

## Step 5: Final Cleanup

Call `/kill-all` to ensure a completely clean state with the new code.

Verify the `tests/` folder has no remaining report files:
```bash
ls tests/*bugs*.md tests/*report*.md 2>/dev/null | wc -l
```

Report the total number of reports processed and bugs fixed.

---

## Important Notes

- **Do not introduce new bugs.** Keep fixes minimal and targeted.
- **Do not refactor surrounding code.** Only change what's needed to fix the reported bug.
- **Preserve existing patterns.** Match the style of the file you're editing.
- **If a fix requires a database migration**, create a new Flyway migration file with the next version number in sequence.
- **If a fix is unclear**, read more context from the codebase before implementing. Check related files, tests, and API contracts.
- **Track progress** using the task list — mark each bug as in_progress when starting and completed when done.
