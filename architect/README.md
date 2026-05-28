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
└─ archived/
   └─ 2026-05-01-obsolete-design-draft/
      ├─ meta.json
      ├─ brief.md
      └─ summary.md
```

---

# Status Folders

## `reports/`

Use `reports/` for inspection outputs, audits, scans, and structured findings that have not necessarily become implementation work yet. Reports are evidence records. Pending, active, and resolved architect entries are execution records.

Good candidates:

* redundant-code inspections
* security audits
* configuration drift reports
* dependency scans
* architecture consistency checks
* generated findings that may later become pending or active work

A report file should be timestamped so multiple reports can describe the same area at different times:

```txt
architect/reports/redundant-code-inspection-YYYY-MM-DD-HHmmss.md
```

Example:

```txt
architect/reports/redundant-code-inspection-2026-05-28-160820.md
```

The report should keep its own date-time even when it later feeds a broader architect entry. Multiple reports may lead to the same pending or active entry, and one report may split into multiple entries.

Root report files are live evidence while any related architect entry is still `pending/`, `active/`, or blocked. After every related execution entry has reached a terminal lifecycle state such as `resolved/`, `discontinued/`, or `archived/`, preserve the report inside each terminal entry and then move the root report file to the report cache:

```txt
architect/reports/.cache/redundant-code-inspection-YYYY-MM-DD-HHmmss.md
```

The cache copy is an archival fallback only. Do not keep finalized root report files in `architect/reports/`, and do not keep finalized report nodes in `reports.registry.json`.

The `reports/` directory should include a machine-readable registry:

```txt
architect/reports/reports.registry.json
```

The registry tracks:

* report IDs
* report paths
* report statuses
* summary counts
* related pending/active/resolved architect entries
* supersession relationships
* lifecycle events

Allowed report status values:

```txt
reported
triaged
promoted_pending
promoted_active
linked_existing
blocked
resolved
archived
superseded
```

Recommended report event types:

```txt
REPORT_CREATED
REPORT_UPDATED
REPORT_TRIAGED
REPORT_SUPERSEDED
REPORT_ARCHIVED
REPORT_PROMOTED_TO_PENDING
REPORT_PROMOTED_TO_ACTIVE
REPORT_LINKED_TO_EXISTING_ARCHITECT
ARCHITECT_CREATED_PENDING
ARCHITECT_CREATED_ACTIVE
ARCHITECT_MOVED_TO_ACTIVE
ARCHITECT_BLOCKED
ARCHITECT_RESOLVED
REPORT_COPIED_TO_RESOLUTION
REGISTRY_AUDITED
```

Example `reports.registry.json`:

```json
{
  "schemaVersion": 1,
  "updatedAt": "2026-05-28T16:08:20-04:00",
  "reports": [
    {
      "id": "redundant-code-inspection-2026-05-28-160820",
      "kind": "redundant-code-inspection",
      "title": "Redundant Code / Lookup Inspection Report",
      "path": "architect/reports/redundant-code-inspection-2026-05-28-160820.md",
      "status": "reported",
      "createdAt": "2026-05-28T16:08:20-04:00",
      "updatedAt": "2026-05-28T16:08:20-04:00",
      "summary": {
        "totalFindings": 11,
        "safeRemovals": 0,
        "consolidationCandidates": 2,
        "lookupCacheCandidates": 3,
        "refactorCandidates": 3,
        "unknownRiskFindings": 6
      },
      "relatedArchitectEntries": [],
      "supersedes": [],
      "supersededBy": null,
      "events": [
        {
          "at": "2026-05-28T16:08:20-04:00",
          "type": "REPORT_CREATED",
          "note": "Initial redundant code inspection report created."
        }
      ]
    }
  ]
}
```

Keep the registry valid JSON. Do not add comments or Markdown inside `reports.registry.json`.

The registry tracks live report workflow only. When a report is finalized into terminal architect entries and moved to `architect/reports/.cache/`, remove that report object from `reports.registry.json` and refresh `updatedAt`.

---

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

Use `archived/` for stale, rejected, obsolete, or intentionally abandoned records.

Do not move completed work to `archived/` by default. Completed work belongs in `resolved/`.

Use `archived/` only when the entry is no longer relevant but should still be preserved.

---

# File Roles

## `reports/*.md`

A report Markdown file is an evidence snapshot. It should capture findings, counts, affected files, risk, evidence strength, recommendations, and proposed architect entries.

Reports should answer:

```txt
What was inspected?
When was it inspected?
What was found?
What evidence supports each finding?
What should become pending or active architect work?
What should not be changed without a design decision?
How should follow-up work be verified?
```

Reports should not directly replace `brief.md`, `assessment.md`, or `plan.md`. Instead, they feed those files when an architect entry is created.

## `reports.registry.json`

Machine-readable report lifecycle registry.

Use this file for:

* report IDs and paths
* report status
* timestamps
* summary counters
* links from reports to architect entries
* report supersession
* report-to-architect promotion events
* resolution-copy events

Do not put full report bodies in `reports.registry.json`. Store live report bodies in `architect/reports/*.md`, finalized fallback copies in `architect/reports/.cache/*.md`, and terminal-entry report copies in the terminal entry folder.

---

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
  "sourceReports": [],
  "origin": {
    "source": "chat",
    "summary": "Discussion about storing raw PolicyEvaluationResult output over time for audit, debugging, and action-package use."
  },
  "events": [
    {
      "at": "2026-05-07T02:10:00-04:00",
      "type": "CREATED",
      "note": "Created from discussion about process scan log storage."
    }
  ]
}
```

Allowed `status` values:

```txt
pending
active
blocked
resolved
archived
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
active -> blocked -> active -> resolved
resolved -> active
resolved -> archived
```

Report-backed flow:

```txt
reports/report.md
  -> reports.registry.json status: reported
  -> pending entry created, or active entry created
  -> reports.registry.json status: promoted_pending or promoted_active
  -> architect entry implemented
  -> terminal entry created under resolved/, discontinued/, archived/, or another final folder
  -> source report copied into the terminal entry as report-YYYY-MM-DD-HHmmss.md
  -> when all related entries are terminal, move root report to reports/.cache/
  -> remove the finalized report node from reports.registry.json
```

## Creating a Report

When an inspection, audit, or scan produces findings:

1. Create a timestamped Markdown report under `architect/reports/`.
2. Create or update `architect/reports/reports.registry.json`.
3. Add a `REPORT_CREATED` or `REPORT_UPDATED` event.
4. Keep the report sanitized according to the repository safety rules.
5. Do not create implementation changes from the report in the same pass unless explicitly requested.

## Promoting a Report to an Architect Entry

When a report finding becomes planned work:

1. Create the folder under `pending/` unless work begins immediately.
2. Create the folder under `active/` if implementation begins immediately.
3. Add normal architect files such as `meta.json`, `brief.md`, `todo.md`, and `context.md`.
4. Add `plan.md` when staged implementation is needed.
5. Add the report ID to the architect entry `meta.json` under `sourceReports`.
6. Add the architect entry ID to the report registry item under `relatedArchitectEntries`.
7. Append registry events such as `REPORT_PROMOTED_TO_PENDING`, `ARCHITECT_CREATED_PENDING`, `REPORT_PROMOTED_TO_ACTIVE`, or `ARCHITECT_CREATED_ACTIVE`.

Example architect `meta.json` report link:

```json
{
  "sourceReports": [
    "redundant-code-inspection-2026-05-28-160820"
  ]
}
```

## Resolving Report-Backed Work

When work from a report is resolved:

1. Move the architect entry to `resolved/` or another documented terminal folder.
2. Finalize `assessment.md`, `fixes.md`, `verification.md`, and `summary.md`.
3. Copy the source report into the terminal entry folder.
4. Name the copied report using the report timestamp:

```txt
architect/resolved/YYYY-MM-DD-short-title/report-YYYY-MM-DD-HHmmss.md
```

Example:

```txt
architect/resolved/2026-05-28-transport-parsing-consolidation/report-2026-05-28-160820.md
```

5. Append `ARCHITECT_RESOLVED` and `REPORT_COPIED_TO_RESOLUTION` while the report remains live in `architect/reports/`.
6. If any related architect entry is still `pending/`, `active/`, or blocked, keep the root report file and registry node in place.
7. If every related architect entry is terminal, finalize the source report:
   * verify the report has been copied into each terminal entry that needs the evidence
   * move `architect/reports/<report-id>.md` to `architect/reports/.cache/<report-id>.md`
   * remove that report object from `architect/reports/reports.registry.json`
   * refresh the registry `updatedAt` timestamp and keep the JSON valid
8. Do not leave duplicate finalized report files in `architect/reports/`.

The terminal entry copy is the primary historical record. The `.cache/` file is only a fallback for the original root report after the live workflow is complete.

## Creating an Entry

When creating a new entry:

1. Create the folder under `pending/` unless work begins immediately.
2. Add `meta.json`.
3. If created from a report, include `sourceReports` in `meta.json`.
4. Add `brief.md`.
5. Add `todo.md`.
6. Add `context.md` if there is meaningful background.
7. Add `prd.md` or `plan.md` when needed.

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
2. If the entry came from one or more reports, copy each source report into the resolved folder as `report-YYYY-MM-DD-HHmmss.md`.
3. Update `meta.json`:

    * `status: "resolved"`
    * `resolvedAt`
    * `updatedAt`
    * append `RESOLVED` event
4. Add or finalize:

    * `assessment.md`
    * `fixes.md`
    * `verification.md`
    * `summary.md`

## Reopening an Entry

When resolved work needs more changes:

1. Move the folder back to `active/`.
2. Update `meta.json`:

    * `status: "active"`
    * `resolvedAt: null`
    * `updatedAt`
    * append `REOPENED` event
3. Add new notes to `notes.md`.
4. Preserve old resolution files unless they are misleading.

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

---

# Entry Templates

## Report Template

```txt
architect/reports/
├─ reports.registry.json
└─ redundant-code-inspection-YYYY-MM-DD-HHmmss.md
```

A report may later be copied into one or more terminal entries:

```txt
architect/resolved/YYYY-MM-DD-short-title/
└─ report-YYYY-MM-DD-HHmmss.md
```

After all work sourced from the report reaches a terminal lifecycle state, move the root report file into the cache and remove its registry node:

```txt
architect/reports/.cache/
└─ redundant-code-inspection-YYYY-MM-DD-HHmmss.md
```

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
├─ summary.md
└─ report-YYYY-MM-DD-HHmmss.md
```

## Archived Entry Template

```txt
architect/archived/YYYY-MM-DD-short-title/
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
  "sourceReports": [],
  "origin": {
    "source": "chat",
    "summary": "Discussion about storing raw process evaluation logs over time for debugging, audit, bot review, and future action-package execution."
  },
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

## Keep Reports Separate From Execution Entries

Reports are evidence snapshots. Pending and active architect entries are execution plans. Do not treat a report as implementation approval by itself. Promote the relevant finding into `pending/` or `active/`, link it in `reports.registry.json`, and keep the report available for audit.

## Keep the Report Registry Auditable

Whenever a report is created, triaged, promoted, linked, superseded, archived, copied to resolution, or resolved, append an event to `architect/reports/reports.registry.json`. Use the event enum values documented in the `reports/` section.

When a report's related execution entries are all terminal, finish the registry audit before deletion: confirm the terminal-entry report copies exist, move the root report to `architect/reports/.cache/`, remove the report node, and refresh `updatedAt`. Do not keep a finalized report node solely to preserve event history.

## Use ISO Date Order

Use `YYYY-MM-DD` for folder and report dates. Avoid `YYYY-DD-MM`; for example, use `2026-05-28`, not `2026-28-05`.

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
task-sentinel architect list --tag logging
task-sentinel architect open 2026-05-07-process-scan-log-storage-and-rotation
task-sentinel architect related 2026-05-07-policy-action-package-contract
task-sentinel architect move --to active 2026-05-07-process-scan-log-storage-and-rotation
task-sentinel architect summarize --status resolved
task-sentinel architect reports list
task-sentinel architect reports audit-registry
task-sentinel architect reports promote redundant-code-inspection-2026-05-28-160820 --to pending
task-sentinel architect reports copy-to-resolution redundant-code-inspection-2026-05-28-160820 2026-05-28-transport-parsing-consolidation
task-sentinel architect reports finalize redundant-code-inspection-2026-05-28-160820
```

Potential future uses:

* track report-to-architect promotion
* audit unresolved reports
* preserve source reports inside terminal entries
* move finalized source reports into `reports/.cache/`
* generate changelogs
* find related bugs
* group similar failures
* track unresolved design decisions
* produce agent handoff summaries
* build a local project knowledge graph
* calculate time from creation to resolution
* identify recurring subsystem problems

---

# Recommended Minimum Entry

For most new reports, start with:

```txt
architect/reports/reports.registry.json
architect/reports/<kind>-YYYY-MM-DD-HHmmss.md
```

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
