# Architect Directory

The `architect/` directory is Task Sentinel’s structured engineering memory.

It stores planning records, investigations, design drafts, PRDs, implementation notes, blocked decisions, and resolved work records. It is broader than an `issues/` folder because not every entry is a bug. Some entries are feature plans, architecture changes, deferred ideas, refactors, research notes, or future implementation packages.

The goal is to make project work resumable by a human or agent without needing the original chat history.

---

## Purpose

Use `architect/` to track:

- bugs
- PRDs
- feature plans
- refactor plans
- design decisions
- implementation drafts
- root-cause investigations
- deferred work
- blocked tasks
- resolved engineering records
- future action-package ideas

This directory should answer:

```txt
What was being worked on?
Why did it matter?
What context was known?
What decisions were made?
What changed?
How was it verified?
What remains unresolved?
````

---

## Naming Convention

Each entry folder should use:

```txt
YYYY-MM-DD-short-kebab-title
```

Example:

```txt
2026-05-07-process-scan-log-storage-and-rotation
2026-05-04-json-window-title-escape-crash
2026-05-04-powershell-require-admin-hard-failure
```

Rules:

* Use the creation date.
* Keep the title short.
* Use lowercase kebab-case.
* Prefer specific names over generic names.
* Do not rename entries casually after creation unless the original title is misleading.

---

## Directory Layout

```txt
architect/
├─ README.md
│
├─ pending/
│  └─ 2026-05-07-process-scan-log-storage-and-rotation/
│     ├─ meta.json
│     ├─ brief.md
│     ├─ prd.md
│     ├─ todo.md
│     ├─ context.md
│     └─ blockers.md
│
├─ active/
│  └─ 2026-05-07-policy-action-package-contract/
│     ├─ meta.json
│     ├─ brief.md
│     ├─ plan.md
│     ├─ todo.md
│     ├─ context.md
│     ├─ notes.md
│     └─ blockers.md
│
├─ resolved/
│  └─ 2026-05-04-powershell-require-admin-hard-failure/
│     ├─ meta.json
│     ├─ brief.md
│     ├─ assessment.md
│     ├─ fixes.md
│     ├─ verification.md
│     └─ summary.md
│
├─ archived/
│  └─ 2026-05-01-obsolete-design-draft/
│     ├─ meta.json
│     ├─ brief.md
│     └─ summary.md
│
├─ discontinued/
│  └─ 2026-05-02-stopped-custom-plugin-loader/
│     ├─ meta.json
│     ├─ brief.md
│     └─ summary.md
│
└─ superseded/
   └─ 2026-05-03-custom-http-client-wrapper/
      ├─ meta.json
      ├─ brief.md
      └─ summary.md
```

---

# Status Folders

## `pending/`

Use `pending/` for work that is known but not currently being implemented.

Good candidates:

* future todos
* PRDs
* deferred ideas
* known design gaps
* unresolved bugs
* planned refactors
* user decisions needed later
* implementation ideas not yet started

A pending entry should usually include:

```txt
meta.json
brief.md
todo.md
context.md
```

Optional:

```txt
prd.md
plan.md
blockers.md
```

---

## `active/`

Use `active/` for work currently being investigated, designed, or implemented.

Good candidates:

* current debugging sessions
* active refactors
* open architecture changes
* partially implemented plans
* tasks with evolving notes
* entries requiring repeated agent/human updates

An active entry should usually include:

```txt
meta.json
brief.md
plan.md
todo.md
context.md
notes.md
```

Optional:

```txt
blockers.md
prd.md
```

---

## `resolved/`

Use `resolved/` for completed work.

A resolved entry should explain:

* what was reported
* what caused it
* what changed
* how it was verified
* what risks remain

A resolved entry should usually include:

```txt
meta.json
brief.md
assessment.md
fixes.md
verification.md
summary.md
```

---

## `archived/`

Use `archived/` for stale, obsolete, or preserved records that do not fit a more specific terminal state.

Do not move completed work to `archived/` by default. Completed work belongs in `resolved/`.

Do not use `archived/` when the stronger reason is known. Use `discontinued/` for intentionally stopped work and `superseded/` for work replaced by a better implementation, later decision, or existing library.

---

## `discontinued/`

Use `discontinued/` for work that was intentionally stopped and is not expected to continue.

Good candidates:

* prototypes that are no longer maintained
* implementation tracks cancelled by project direction
* features deliberately dropped from scope
* work stopped because its cost, risk, or priority changed

Discontinued entries must preserve the reason in `meta.json` so future readers know why the work stopped.

A discontinued entry should usually include:

```txt
meta.json
brief.md
summary.md
```

Optional:

```txt
context.md
assessment.md
```

---

## `superseded/`

Use `superseded/` for work that was replaced by a better solution, later design, existing library, platform capability, or another architect entry.

Good candidates:

* implementations reversed after a better approach appeared
* custom code replaced by an existing library
* design drafts invalidated by a later architecture decision
* entries replaced by a more accurate or more complete entry

Superseded entries must preserve the reason in `meta.json`. When there is a replacement entry, library, module, or decision, record it in both `meta.json.related` and the supersession metadata.

A superseded entry should usually include:

```txt
meta.json
brief.md
summary.md
```

Optional:

```txt
context.md
assessment.md
```

---

# File Roles

## `meta.json`

Machine-readable lifecycle and indexing metadata.

Use this file for:

* status
* timestamps
* tags
* related entries
* lifecycle events
* origin information
* grouping/search support

Do not put full investigation notes in `meta.json`.

Terminal states that abandon or replace work must include a compact reason in `meta.json.disposition.reason`. Use Markdown files for longer explanation.

Use `disposition` only when the entry is moved to a terminal non-resolution space such as `discontinued/` or `superseded/`. Keep it `null` for ordinary `pending`, `active`, `blocked`, `resolved`, and generic `archived` entries unless the archive reason needs to be indexed.

Example:

```json
{
  "id": "2026-05-07-process-scan-log-storage-and-rotation",
  "title": "Process Scan Log Storage and Rotation",
  "status": "pending",
  "createdAt": "2026-05-07T02:10:00-04:00",
  "activatedAt": null,
  "resolvedAt": null,
  "archivedAt": null,
  "discontinuedAt": null,
  "supersededAt": null,
  "updatedAt": "2026-05-07T02:10:00-04:00",
  "tags": [
    "logging",
    "audit",
    "jsonl",
    "policy-evaluation",
    "storage"
  ],
  "related": [
    "2026-05-07-policy-action-package-contract"
  ],
  "origin": {
    "source": "chat",
    "summary": "Discussion about storing raw PolicyEvaluationResult output over time for audit, debugging, and action-package use."
  },
  "disposition": null,
  "events": [
    {
      "at": "2026-05-07T02:10:00-04:00",
      "type": "CREATED",
      "note": "Created from discussion about process scan log storage."
    }
  ]
}
```

Disposition example for `discontinued/`:

```json
{
  "status": "discontinued",
  "discontinuedAt": "2026-05-08T14:30:00-04:00",
  "disposition": {
    "type": "discontinued",
    "reason": "Stopped because the feature no longer fits the MVP scope.",
    "decidedAt": "2026-05-08T14:30:00-04:00",
    "decidedBy": "maintainer",
    "replacement": null
  }
}
```

Disposition example for `superseded/`:

```json
{
  "status": "superseded",
  "supersededAt": "2026-05-08T16:45:00-04:00",
  "related": [
    "2026-05-08-adopt-standard-http-client"
  ],
  "disposition": {
    "type": "superseded",
    "reason": "Replaced by the standard HTTP client because it already provides retries, timeouts, and metrics hooks.",
    "decidedAt": "2026-05-08T16:45:00-04:00",
    "decidedBy": "maintainer",
    "replacement": "2026-05-08-adopt-standard-http-client"
  }
}
```

Allowed `status` values:

```txt
pending
active
blocked
resolved
archived
discontinued
superseded
```

Recommended event types:

```txt
CREATED
MOVED_TO_ACTIVE
UPDATED
BLOCKED
UNBLOCKED
RESOLVED
REOPENED
ARCHIVED
DISCONTINUED
SUPERSEDED
RENAMED
LINKED
```

---

## `brief.md`

The core human-readable statement.

Use for:

* original request
* problem statement
* design goal
* expected behavior
* actual behavior
* affected files
* relevant logs
* reproduction steps
* scope boundaries

This replaces narrow names like `issue.md`, because not every architect entry is an issue.

Keep `brief.md` mostly stable after creation. Put evolving thoughts in `notes.md`.

---

## `prd.md`

Product or implementation requirements.

Use for:

* goals
* problem statement
* requirements
* non-goals
* acceptance criteria
* future enhancements
* implementation constraints

Use `prd.md` when an entry describes future functionality, not just a bug fix.

---

## `todo.md`

Active checklist.

Use for:

* investigation tasks
* implementation tasks
* verification tasks
* user decisions still needed
* follow-up work

Example:

```md
# Todo

- [ ] Define scan log record shape.
- [ ] Add JSONL writer.
- [ ] Add size-based rotation.
- [ ] Add date-folder layout.
- [ ] Add gzip compression for rotated files.
- [ ] Add verification notes.
```

---

## `context.md`

Supporting background for humans and agents.

Use for:

* related files
* related entries
* architecture notes
* constraints
* assumptions
* previous decisions
* relevant command output
* chat-derived context
* known risks

This file should make future handoff easier.

---

## `plan.md`

Implementation or design plan.

Use when the entry needs staged execution.

Include:

* proposed approach
* affected modules
* data flow
* risks
* rejected alternatives
* migration notes
* ordering of work

---

## `notes.md`

Active investigation scratchpad.

Use for:

* temporary findings
* observations
* debugging notes
* partial theories
* open questions
* working thoughts

When resolving the entry, move important conclusions into:

```txt
assessment.md
fixes.md
verification.md
summary.md
```

---

## `blockers.md`

Optional pause-state file.

Use only when progress is blocked by:

* missing information
* missing permissions
* unclear expected behavior
* unavailable reproduction data
* user decision required
* dependency not implemented yet

Example:

```md
# Blockers

## BLOCKED: User decision needed

The log writer cannot be finalized until a retention policy is chosen.

Options:

1. Keep logs for 7 days.
2. Keep logs for 30 days.
3. Keep logs until manually deleted.
```

---

## `assessment.md`

Root-cause or system assessment.

Use after investigation.

Include:

* root cause
* why it happened
* affected behavior
* affected files
* risk level
* alternatives considered
* final diagnosis

---

## `fixes.md`

Implementation record.

Use for:

* files changed
* functions changed
* classes changed
* behavior changes
* migration notes
* refactor notes
* compatibility notes

Example:

```md
# Fixes

## Files Changed

- `packages/collector/src/adapters/windows/WindowsProcessAdapter.ts`
- `packages/shared/src/types/policy.ts`

## Behavioral Changes

- The adapter no longer hard-fails when admin rights are unavailable.
- Missing privileged fields are treated as unavailable data instead of fatal errors.
```

---

## `verification.md`

Proof that the work was completed correctly.

Use for:

* tests run
* manual checks
* reproduction retry results
* edge cases checked
* remaining risks
* known limitations

Example:

```md
# Verification

## Manual Checks

- Re-ran observer CLI without admin rights.
- Confirmed process collection continues.
- Confirmed privileged fields degrade gracefully.

## Remaining Risks

- Some owner/user fields may remain unavailable without elevation.
```

---

## `summary.md`

Final compact handoff.

Use for:

* one-paragraph problem summary
* cause
* fix
* result
* future follow-up

A future reader should be able to read only `summary.md` and understand the outcome.

---

# Lifecycle

Typical flow:

```txt
pending -> active -> resolved
```

Alternative flow:

```txt
pending -> archived
pending -> discontinued
active -> discontinued
pending -> superseded
active -> superseded
resolved -> superseded
active -> blocked -> active -> resolved
resolved -> active
resolved -> archived
```

## Creating an Entry

When creating a new entry:

1. Create the folder under `pending/` unless work begins immediately.
2. Add `meta.json`.
3. Add `brief.md`.
4. Add `todo.md`.
5. Add `context.md` if there is meaningful background.
6. Add `prd.md` or `plan.md` when needed.

## Activating an Entry

When work begins:

1. Move the folder from `pending/` to `active/`.
2. Update `meta.json`:

    * `status`
    * `activatedAt`
    * `updatedAt`
    * append `MOVED_TO_ACTIVE` event
3. Add or update:

    * `plan.md`
    * `todo.md`
    * `notes.md`

## Blocking an Entry

When work cannot continue:

1. Keep the folder in `active/` unless it is deferred.
2. Add or update `blockers.md`.
3. Update `meta.json`:

    * `status: "blocked"`
    * `updatedAt`
    * append `BLOCKED` event

## Resolving an Entry

When work is complete:

1. Move the folder to `resolved/`.
2. Update `meta.json`:

    * `status: "resolved"`
    * `resolvedAt`
    * `updatedAt`
    * append `RESOLVED` event
3. Add or finalize:

    * `assessment.md`
    * `fixes.md`
    * `verification.md`
    * `summary.md`

## Discontinuing an Entry

When work is intentionally stopped and is not expected to continue:

1. Move the folder to `discontinued/`.
2. Update `meta.json`:

    * `status: "discontinued"`
    * `discontinuedAt`
    * `updatedAt`
    * `disposition.type: "discontinued"`
    * `disposition.reason`
    * append `DISCONTINUED` event
3. Add or finalize `summary.md` with the human-readable explanation.
4. Preserve useful context, but do not treat the entry as completed work.

## Superseding an Entry

When work is replaced by a better implementation, another entry, an existing library, or a later decision:

1. Move the folder to `superseded/`.
2. Update `meta.json`:

    * `status: "superseded"`
    * `supersededAt`
    * `updatedAt`
    * `disposition.type: "superseded"`
    * `disposition.reason`
    * `disposition.replacement` when known
    * `related` with the replacement entry when applicable
    * append `SUPERSEDED` event
3. Add or finalize `summary.md` with what replaced the entry and why.
4. If the replacement is an existing library or external capability rather than another architect entry, name it in `disposition.replacement` and explain it in `summary.md`.

## Reopening an Entry

When resolved, discontinued, or superseded work needs more changes:

1. Move the folder back to `active/`.
2. Update `meta.json`:

    * `status: "active"`
    * `resolvedAt: null` when reopening from `resolved/`
    * `discontinuedAt: null` when reopening from `discontinued/`
    * `supersededAt: null` when reopening from `superseded/`
    * `disposition: null` unless the prior disposition remains important as indexed context
    * `updatedAt`
    * append `REOPENED` event
3. Add new notes to `notes.md`.
4. Preserve old resolution, discontinuation, or supersession files unless they are misleading.

---

# Tagging Guidelines

Use tags to make future search, grouping, and agent reasoning easier.

Recommended tag groups:

## Area Tags

```txt
collector
classifier
policy-engine
action-engine
ledger
security
cli
logging
ui
shared-types
```

## Type Tags

```txt
bug
prd
refactor
design
investigation
todo
decision
verification
performance
security
storage
```

## Data/Behavior Tags

```txt
jsonl
policy-evaluation
active-process-snapshot
user-decision-rule
scan-level
process-telemetry
trust-score
rotation
compression
windows
powershell
```

## Status/Workflow Tags

```txt
blocked
needs-user-decision
future
mvp
post-mvp
manual-check-needed
discontinued
superseded
```

---

# Relationship Tracking

Use `meta.json.related` to connect entries.

Example:

```json
{
  "related": [
    "2026-05-07-policy-action-package-contract",
    "2026-05-07-process-scan-log-storage-and-rotation"
  ]
}
```

Use relationships for:

* parent/child work
* follow-up tasks
* similar bugs
* related architecture decisions
* PRDs that depend on another PRD
* fixes that came from the same root cause
* superseded entries and their replacements

---

# Entry Templates

## Pending PRD Template

```txt
architect/pending/YYYY-MM-DD-short-title/
├─ meta.json
├─ brief.md
├─ prd.md
├─ todo.md
└─ context.md
```

## Active Debugging Template

```txt
architect/active/YYYY-MM-DD-short-title/
├─ meta.json
├─ brief.md
├─ todo.md
├─ context.md
├─ notes.md
└─ blockers.md
```

## Active Refactor Template

```txt
architect/active/YYYY-MM-DD-short-title/
├─ meta.json
├─ brief.md
├─ plan.md
├─ todo.md
├─ context.md
└─ notes.md
```

## Resolved Bug Template

```txt
architect/resolved/YYYY-MM-DD-short-title/
├─ meta.json
├─ brief.md
├─ assessment.md
├─ fixes.md
├─ verification.md
└─ summary.md
```

## Archived Entry Template

```txt
architect/archived/YYYY-MM-DD-short-title/
├─ meta.json
├─ brief.md
└─ summary.md
```

## Discontinued Entry Template

```txt
architect/discontinued/YYYY-MM-DD-short-title/
├─ meta.json
├─ brief.md
└─ summary.md
```

## Superseded Entry Template

```txt
architect/superseded/YYYY-MM-DD-short-title/
├─ meta.json
├─ brief.md
└─ summary.md
```

---

# Example Entry: Process Scan Log Storage

```txt
architect/pending/2026-05-07-process-scan-log-storage-and-rotation/
├─ meta.json
├─ brief.md
├─ prd.md
├─ todo.md
└─ context.md
```

## Example `meta.json`

```json
{
  "id": "2026-05-07-process-scan-log-storage-and-rotation",
  "title": "Process Scan Log Storage and Rotation",
  "status": "pending",
  "createdAt": "2026-05-07T02:10:00-04:00",
  "activatedAt": null,
  "resolvedAt": null,
  "archivedAt": null,
  "discontinuedAt": null,
  "supersededAt": null,
  "updatedAt": "2026-05-07T02:10:00-04:00",
  "tags": [
    "logging",
    "audit",
    "jsonl",
    "policy-evaluation",
    "storage",
    "future"
  ],
  "related": [
    "2026-05-07-policy-action-package-contract"
  ],
  "origin": {
    "source": "chat",
    "summary": "Discussion about storing raw process evaluation logs over time for debugging, audit, bot review, and future action-package execution."
  },
  "disposition": null,
  "events": [
    {
      "at": "2026-05-07T02:10:00-04:00",
      "type": "CREATED",
      "note": "Created from discussion about process scan log storage."
    }
  ]
}
```

---

# Rules

## Keep Entries Focused

Prefer one focused entry per concern.

Good:

```txt
2026-05-07-process-scan-log-storage-and-rotation
2026-05-07-policy-action-package-contract
```

Avoid:

```txt
2026-05-07-fix-logging-and-actions-and-ledger-and-cli
```

## Keep `brief.md` Stable

`brief.md` should preserve the original problem or goal.

Do not rewrite it every time the plan changes.

## Put Evolving Work in the Right File

Use:

```txt
notes.md      -> temporary thoughts
todo.md       -> checklist
plan.md       -> structured implementation path
assessment.md -> final diagnosis
fixes.md      -> final implementation record
summary.md    -> final handoff
```

## Do Not Hide Decisions in Chat

If a decision affects future implementation, write it into:

```txt
context.md
plan.md
assessment.md
summary.md
```

## Keep `meta.json` Parseable

`meta.json` must remain valid JSON.

Do not add comments, trailing commas, Markdown, or large notes.

## Prefer Markdown for Human Context

Use Markdown files for explanations, plans, and summaries.

Use JSON only for metadata that future tooling should parse.

---

# Future Tooling Ideas

The `architect/` structure is designed to support later automation.

Possible future commands:

```txt
task-sentinel architect list --status pending
task-sentinel architect list --status superseded
task-sentinel architect list --tag logging
task-sentinel architect open 2026-05-07-process-scan-log-storage-and-rotation
task-sentinel architect related 2026-05-07-policy-action-package-contract
task-sentinel architect move --to active 2026-05-07-process-scan-log-storage-and-rotation
task-sentinel architect discontinue 2026-05-07-process-scan-log-storage-and-rotation --reason "No longer fits MVP scope"
task-sentinel architect supersede 2026-05-07-custom-http-client --replacement 2026-05-08-adopt-standard-http-client --reason "Existing client covers retries and metrics"
task-sentinel architect summarize --status resolved
```

Potential future uses:

* generate changelogs
* find related bugs
* group similar failures
* track unresolved design decisions
* produce agent handoff summaries
* build a local project knowledge graph
* calculate time from creation to resolution
* identify recurring subsystem problems
* audit why work was discontinued or superseded

---

# Recommended Minimum Entry

For most new entries, start with:

```txt
meta.json
brief.md
todo.md
context.md
```

Add other files only when needed.

For very small tasks, this is enough:

```txt
meta.json
brief.md
todo.md
```

For resolved records, always aim to end with:

```txt
meta.json
brief.md
assessment.md
fixes.md
verification.md
summary.md
```
