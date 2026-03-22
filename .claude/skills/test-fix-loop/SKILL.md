---
name: test-fix-loop
description: Iteratively test and fix bugs until the happy path is clean
user-invocable: true
---

# Test-Fix Loop

Continuously alternate between testing and fixing bugs until a test run produces zero fixable bugs.

**Argument:** `$ARGUMENTS` — optional mode (`demo`, `buyer`, `seller`, `admin`, `broker`, `all`). Default: `demo`.

---

## CRITICAL: Context Management

**This skill MUST use Task tool subagents for the test and fix phases.** Running `/test-happy-path` or `/fix-bugs` directly via the Skill tool will flood the main conversation context with browser snapshots, screenshots, and network data, causing autocompact and losing loop state.

**Rules:**
1. **NEVER call `/test-happy-path` or `/fix-bugs` via the Skill tool** from within this loop
2. **ALWAYS use the Task tool** with `subagent_type: "general-purpose"` for each phase
3. **Persist loop state to a file** after each phase so it survives autocompact
4. **Keep the main conversation lean** — only read summaries and bug report files, never full browser output

---

## Overview

```
┌─────────────────────────────────────────┐
│  Task subagent: test phase              │
│  (runs /test-happy-path, writes report) │
└──────────────┬──────────────────────────┘
               ▼
       ┌───────────────┐
       │ Fixable bugs? │──── No ──► DONE (all clean)
       └───────┬───────┘
               │ Yes
               ▼
┌─────────────────────────────────────────┐
│  Task subagent: fix phase               │
│  (runs /fix-bugs, deletes report)       │
└──────────────┬──────────────────────────┘
               │
               ▼
          Loop back to top
```

---

## Step 1: Initialize

Set the mode from `$ARGUMENTS` (default `demo`). Set `MAX_ITERATIONS = 10`. Set `iteration = 0`.

Write initial state to the state file:
```bash
cat > tests/.test-fix-state.json << 'EOF'
{
  "mode": "<mode>",
  "iteration": 0,
  "maxIterations": 10,
  "status": "running",
  "history": []
}
EOF
```

Initialize the fix summary file (this file persists across the entire loop and is never deleted):
```bash
cat > tests/.fix-summary.md << 'EOF'
# Test-Fix Loop Summary

**Mode:** <mode>
**Started:** <date>
**Status:** Running

## Iterations

(updated after each iteration)

## Bugs Fixed

| Iter | Bug ID | Severity | Component | Description | Files Changed |
|------|--------|----------|-----------|-------------|---------------|

## Unfixable / Skipped

| Bug ID | Severity | Reason |
|--------|----------|--------|
EOF
```

---

## Step 1a: Update Fix Summary

**After EVERY test or fix phase**, update `tests/.fix-summary.md` with the latest results. This file is the **persistent record** of the entire test-fix loop. It must:

- Be updated (not recreated) after each phase — append new iteration results, update the status
- **Never be deleted** by any phase (test, fix, or cleanup)
- Survive across iterations so the user can review cumulative progress at any time
- On completion (Step 5 or Step 6), update the status to `Complete` or `Max Iterations Reached` and include the final summary

---

## Step 1b: Browser Cleanup Between Phases

**Before EVERY test or fix subagent launch**, clean up the Chrome DevTools MCP browser from the main conversation context:

1. Call `mcp__chrome-devtools__list_pages` to check if a browser is connected
2. If pages exist: close all except the last via `mcp__chrome-devtools__close_page`, then navigate the last to `about:blank`
3. If `list_pages` fails (no browser connected), that's fine — skip cleanup

This prevents stale browser windows from prior subagents interfering with the next phase.

---

## Step 2: Test Phase (via Task Subagent)

**Use the Task tool** with these parameters:
- `subagent_type`: `"general-purpose"`
- `description`: `"Test happy path iteration N"`
- `prompt`: Include the full instructions — tell the subagent to:
  1. Run the `/test-happy-path` skill with the mode argument (e.g., `demo`)
  2. After completion, return a brief summary: how many steps passed/failed, how many bugs found, and the report filename

Example Task prompt:
```
Run the /test-happy-path skill with argument "<mode>".
The working directory is /home/radionica/Radionica/Tradex/Tradex/spin-logistics.
After testing completes, report back:
- Number of steps: PASS / FAIL / PARTIAL / SKIP
- Number of bugs found (by severity)
- Report filename(s) written to tests/
```

**Wait for the Task to complete** before proceeding.

---

## Step 3: Check for Fixable Bugs

After the test subagent completes, check if any bug reports exist in `tests/`:

```bash
ls -t tests/*bugs*.md tests/*report*.md 2>/dev/null
```

If no report files exist, the test run was fully clean — **go to Step 6 (Done)**.

If report files exist, **read each one** and count the OPEN bugs that are **fixable** (not caused by services being down). A bug is fixable if it references a source **File** and has a code-level **Root Cause**.

Unfixable bugs (skip these in the count):
- Root cause is "service X is not running" or "service X is down"
- Status is already `FIXED during test`
- No source file referenced

If **fixable bug count = 0** but report still has unfixable entries — **go to Step 6 (Done)** and report the remaining unfixable issues to the user.

If **fixable bug count > 0** — proceed to Step 4.

Update the state file with results from this iteration.

---

## Step 4: Fix Phase (via Task Subagent)

Increment `iteration`. If `iteration > MAX_ITERATIONS`, **go to Step 5 (Max Iterations)**.

**Use the Task tool** with these parameters:
- `subagent_type`: `"general-purpose"`
- `description`: `"Fix bugs iteration N"`
- `prompt`: Include the full instructions — tell the subagent to:
  1. Run the `/fix-bugs` skill (no role filter needed — it processes all reports in `tests/`)
  2. After completion, return a brief summary: how many bugs were fixed, which files were changed, any build errors

Example Task prompt:
```
Run the /fix-bugs skill.
The working directory is /home/radionica/Radionica/Tradex/Tradex/spin-logistics.
After fixing completes, report back:
- Number of bugs fixed
- Number of bugs skipped (unfixable)
- Files changed
- Build verification result (pass/fail)
```

**Wait for the Task to complete** before proceeding.

Update the state file:
```bash
# Update state file with fix results
cat > tests/.test-fix-state.json << 'EOF'
{
  "mode": "<mode>",
  "iteration": <N>,
  "maxIterations": 10,
  "status": "running",
  "history": [
    {"iteration": 1, "bugsFound": <n>, "bugsFixed": <n>},
    ...
  ]
}
EOF
```

After the fix subagent completes, **run Step 1b (Browser Cleanup)** then **loop back to Step 2** to re-test.

---

## Step 5: Max Iterations Reached

If the loop has run `MAX_ITERATIONS` times without reaching zero bugs, stop and report to the user:

- How many iterations were completed
- How many bugs were fixed across all iterations
- What bugs remain (with details from the latest report)
- Suggest the remaining bugs may need manual intervention or architectural changes

Clean up the state file:
```bash
rm -f tests/.test-fix-state.json
```

Do NOT continue looping. The user needs to review at this point.

---

## Step 6: Done — All Clean

Report the final result to the user:

- **Total iterations** completed
- **Total bugs fixed** across all iterations
- **Remaining unfixable bugs** (services down, infrastructure issues) if any
- Final status: all happy path steps PASS, or list any remaining PARTIAL/SKIP steps with reasons

Clean up the state file and test reports, but **keep `.fix-summary.md`**:
```bash
rm -f tests/.test-fix-state.json
rm -f tests/*-happy-path-report-*.md tests/*bugs*.md tests/*report*.md
```

The `tests/` directory should be left with only `tests/.fix-summary.md`.

If there are no fixable bugs remaining but some unfixable ones (services down), clearly distinguish these so the user knows the codebase is clean but infrastructure needs attention.

---

## Recovery: If Autocompact Occurs

If you lose context due to autocompact, check the state file and fix summary to recover:
```bash
cat tests/.test-fix-state.json 2>/dev/null
cat tests/.fix-summary.md 2>/dev/null
```

Also check for existing bug reports:
```bash
ls -t tests/*bugs*.md tests/*report*.md 2>/dev/null
```

Use the state file for loop position and `.fix-summary.md` for cumulative progress to determine where in the loop you are and resume from the correct step.

---

## Important Notes

- **ALWAYS use Task subagents** for test and fix phases. Never call these skills directly in the main conversation.
- **Max 10 iterations.** If bugs persist after 10 test-fix cycles, something deeper is wrong — stop and report.
- **Each iteration is a full cycle:** test → check → fix → re-test. This ensures fixes don't introduce regressions.
- **The `/fix-bugs` skill deletes the report** after processing, so each `/test-happy-path` run starts fresh.
- **`tests/.fix-summary.md` is NEVER deleted.** It is the persistent record updated after every phase. On completion, the tests/ folder should contain only `.fix-summary.md`commit a.
- **Progress is cumulative.** Bugs fixed in iteration 1 stay fixed in iteration 2 (unless a fix introduced a regression).
- **If a fix introduces a NEW bug** not in the previous report, it will be caught in the next test cycle and fixed in the following fix cycle.
- **Mode carries through.** If you start with `demo`, every test cycle uses the demo flow. For role-specific modes (`seller`, `buyer`, etc.), the fix cycle processes all reports in `tests/`.
- **The state file is your safety net.** Always update it after each phase completes.
