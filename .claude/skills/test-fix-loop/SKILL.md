---
name: test-fix-loop
description: Iteratively test and fix bugs until the happy path is clean
user-invocable: true
---

# Test-Fix Loop

Continuously alternate between `/test-happy-path` and `/fix-bugs` until a test run produces zero fixable bugs.

**Argument:** `$ARGUMENTS` — optional role filter (`buyer`, `seller`, `admin`, `broker`, `all`). Default: `all`.

---

## Overview

```
┌─────────────────────────────┐
│  /test-happy-path <role>    │
│  (generates bug report)     │
└──────────────┬──────────────┘
               ▼
       ┌───────────────┐
       │ Fixable bugs? │──── No ──► DONE (all clean)
       └───────┬───────┘
               │ Yes
               ▼
┌─────────────────────────────┐
│  /fix-bugs <role>           │
│  (fixes bugs, deletes report│
│   calls /kill-all)          │
└──────────────┬──────────────┘
               │
               ▼
          Loop back to top
```

---

## Step 1: Initialize

Set the role from `$ARGUMENTS` (default `all`). Set `MAX_ITERATIONS = 5`. Set `iteration = 0`.

---

## Step 2: Test Phase

Call `/test-happy-path` with the role argument. This will:
- Start the full stack via `/run-full-stack`
- Run all browser tests for the specified role(s)
- Write bug report(s) to `tests/`
- Tear down via `/kill-all`

---

## Step 3: Check for Fixable Bugs

After `/test-happy-path` completes, check if any bug reports exist in `tests/`:

```bash
ls -t tests/*bugs*.md tests/*report*.md 2>/dev/null
```

If no report files exist, the test run was fully clean — **go to Step 6 (Done)**.

If report files exist, read each one and count the OPEN bugs that are **fixable** (not caused by services being down). A bug is fixable if it references a source **File** and has a code-level **Root Cause**.

Unfixable bugs (skip these in the count):
- Root cause is "service X is not running" or "service X is down"
- Status is already `FIXED during test`
- No source file referenced

If **fixable bug count = 0** but report still has unfixable entries — **go to Step 6 (Done)** and report the remaining unfixable issues to the user.

If **fixable bug count > 0** — proceed to Step 4.

---

## Step 4: Fix Phase

Increment `iteration`. If `iteration > MAX_ITERATIONS`, **go to Step 5 (Max Iterations)**.

Call `/fix-bugs` with the same role argument. This will:
- Call `/kill-all`
- Read the latest bug report
- Fix all OPEN fixable bugs
- Build-verify the changes
- Delete the processed report
- Call `/kill-all`

After `/fix-bugs` completes, **loop back to Step 2** to re-test.

---

## Step 5: Max Iterations Reached

If the loop has run `MAX_ITERATIONS` times without reaching zero bugs, stop and report to the user:

- How many iterations were completed
- How many bugs were fixed across all iterations
- What bugs remain (with details from the latest report)
- Suggest the remaining bugs may need manual intervention or architectural changes

Do NOT continue looping. The user needs to review at this point.

---

## Step 6: Done — All Clean

Report the final result to the user:

- **Total iterations** completed
- **Total bugs fixed** across all iterations
- **Remaining unfixable bugs** (services down, infrastructure issues) if any
- Final status: all happy path steps PASS, or list any remaining PARTIAL/SKIP steps with reasons

If there are no fixable bugs remaining but some unfixable ones (services down), clearly distinguish these so the user knows the codebase is clean but infrastructure needs attention.

---

## Important Notes

- **Max 5 iterations.** If bugs persist after 5 test-fix cycles, something deeper is wrong — stop and report.
- **Each iteration is a full cycle:** test → check → fix → re-test. This ensures fixes don't introduce regressions.
- **The `/fix-bugs` skill deletes the report** after processing, so each `/test-happy-path` run starts fresh.
- **Progress is cumulative.** Bugs fixed in iteration 1 stay fixed in iteration 2 (unless a fix introduced a regression).
- **If a fix introduces a NEW bug** not in the previous report, it will be caught in the next test cycle and fixed in the following fix cycle.
- **Role filtering carries through.** If you start with `seller`, every test and fix cycle stays scoped to seller.
