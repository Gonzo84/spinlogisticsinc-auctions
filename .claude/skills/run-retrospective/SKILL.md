# Run Retrospective

Analyze the current session's changes and extract new knowledge into project reference files.

## User-invocable

Trigger: `/run-retrospective`

## Instructions

You are running a retrospective on the current work session. Your goal is to extract new knowledge (gotchas, conventions, patterns, architecture decisions) from recent changes and persist them into the project's reference files so future sessions benefit.

### Step 1: Gather Session Changes

Run these commands to understand what changed:

```bash
# See recent commits (last session's work)
git log --oneline -20

# See all changes since the last session tag or significant commit
git diff HEAD~10 --stat

# See detailed diffs for understanding what was fixed/added
git diff HEAD~10 -- '*.kt' '*.ts' '*.vue' '*.yml' '*.yaml' '*.json' '*.csv'
```

Adjust the range (`HEAD~10`, specific commit hashes, etc.) based on what looks like the start of the current session. Ask the user if unclear.

### Step 2: Read Current Reference Files

Read these files to understand what's already documented:

- `CLAUDE.md` — Project instructions, critical gotchas, architecture
- `CONVENTIONS.md` — Coding conventions, patterns, what NOT to do
- `~/.claude/projects/-home-radionica-Radionica-Tradex-Tradex-eu-auction-platform/memory/MEMORY.md` — Persistent memory

### Step 3: Analyze for New Knowledge

Compare the changes against existing documentation. Look for:

1. **New Gotchas** — Bugs that were fixed, edge cases discovered, things that broke unexpectedly
2. **New Conventions** — Patterns that were established or reinforced by the changes
3. **Architecture Updates** — New endpoints, services, infrastructure changes, dependency updates
4. **Integration Patterns** — New ways services interact, API contracts, data flow patterns
5. **Bug Patterns** — Recurring issues that should be documented to prevent future occurrences
6. **Updated File Structures** — New directories, renamed files, restructured modules

For each finding, check:
- Is this already documented in CLAUDE.md, CONVENTIONS.md, or MEMORY.md?
- Is this a one-off fix or a reusable pattern?
- Is this specific enough to be actionable?

**Only extract knowledge that is NOT already documented.**

### Step 4: Present Findings to User

Before writing anything, present a summary organized by category:

```
## Retrospective Findings

### New Gotchas (for CLAUDE.md)
1. [Description of gotcha and how to avoid it]

### New Conventions (for CONVENTIONS.md)
1. [Description of convention]

### Architecture Updates (for CLAUDE.md)
1. [What changed structurally]

### Integration Patterns (for MEMORY.md)
1. [New pattern discovered]

### No Changes Needed
- [List anything already documented that was confirmed]
```

Ask the user: **"Which of these should I add to the reference files? (all / pick numbers / none)"**

### Step 5: Update Reference Files

Based on user approval, update the appropriate files:

- **CLAUDE.md** — Add new gotchas to the "Critical Gotchas" section (increment numbering). Add architecture changes to the "Architecture" section.
- **CONVENTIONS.md** — Add new conventions to relevant sections. Add new "What NOT To Do" entries if applicable.
- **memory/MEMORY.md** — Add new integration patterns, solved issues, and key learnings to appropriate sections. Keep it concise (under 200 lines).

**Rules for updates:**
- Use the Edit tool, not Write, to preserve existing content
- Append new items; never remove or rewrite existing items
- Match the existing formatting style of each file
- Number new gotchas sequentially after the last existing one
- Verify the update was applied correctly by reading the file after editing

### Step 6: Summary

After updates, print a brief summary:

```
## Retrospective Complete

- Added N gotchas to CLAUDE.md
- Added N conventions to CONVENTIONS.md
- Added N patterns to MEMORY.md
- No duplicates found
```
