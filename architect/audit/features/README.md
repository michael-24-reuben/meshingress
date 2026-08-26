---
name: feature-audits
description: Create and maintain self-contained feature evidence packages under architect/audit/features without treating snapshots as the current source of truth.
---

# Feature Audits

Use `architect/audit/features/` for durable, owner-facing memos about a discrete project or module capability. A feature audit is an evidence snapshot of a usable feature; it is not an implementation plan, a live report workflow, or a replacement for source code and tests.

## Workspace Contract

Create one container per feature:

```txt
architect/audit/features/
├─ SKILL.md
├─ audit.schema.json
└─ YYYY-MM-DD-feature-short-title/
   ├─ audit.json
   └─ audit.md
```

The dated directory preserves the first audit date and keeps each feature independent. Its `audit.json` is the compact machine-readable memo. Its `audit.md` is the human-readable explanation. Do not create a workspace-level feature registry: discover feature containers by directory, or add a separate index only when a project explicitly requires one.

Do not create `architect/audit/features/README.md` unless a project establishes a separate workspace prompt. The root `architect/README.md` and this skill are the default guidance.

## `audit.json`

Validate each audit against [`audit.schema.json`](audit.schema.json). The required fields are:

- `id`: stable feature identifier; use a descriptive kebab-case ID and do not reuse it for another feature.
- `recordedAt`: the time this snapshot was written, in ISO 8601 date-time form.
- `status`, `scope`, and `summary`: concise feature identity and scope.
- `origin`: how the memo was formed. `origin.authority` must state that the memo is not the source of truth and identify live source/tests as authoritative.
- `evidence`: code, verification, design-record, or documentation references. Each reference needs its kind, path, line range, and relevance note.
- `markdownRef`: an array of Markdown documents related to this feature. The primary companion is normally `{ "path": "audit.md", "role": "feature memo" }`.
- `tags`: searchable short labels.

The schema intentionally permits future fields. Keep additions compact and evidence-oriented; do not store raw logs, credentials, complete source files, or lengthy prose in JSON.

## `audit.md`

The companion Markdown file explains the feature in enough detail for a maintainer to understand its boundary, evidence, and known currency limits. Include a feature title, feature ID, recorded time, status, a feature memo, evidence summary, and origin/currency statement.

Put short, multiline illustrative code excerpts in `audit.md` when they improve understanding. Do not duplicate excerpts in `audit.json`. The excerpt is explanatory only; readers must follow the `evidence` paths to the live implementation.

The primary `audit.md` needs no anchor because it is coupled to one feature container. When referencing a section in another Markdown document, use that document's stable anchor or identifier.

## Creation and Maintenance Rules

1. Inspect current source and tests before recording a feature. Do not promote an intention, PRD, or unverified discovery as implemented behavior.
2. Create `YYYY-MM-DD-feature-short-title/` with `audit.json` and `audit.md` together.
3. Add at least one code or verification evidence item with a repository-relative path and line range.
4. Keep origin data time-bounded. If code changes meaningfully, add a later audit container or explicitly amend the existing snapshot; do not silently imply that old evidence is current.
5. Keep feature audits separate from `architect/reports/`: reports are live inspection workflow evidence, while feature audits are stable capability memos.
6. Never place secrets, private tokens, raw request dumps, or user-managed runtime data in a feature audit.

## Suggested Schema

The installed [`audit.schema.json`](audit.schema.json) is the machine-usable Draft 7 schema. It is intentionally based on the following shape, with date-time, non-empty, uniqueness, and minimum-evidence constraints added to make audits dependable:

```json
{
  "id": "feature-example-v1",
  "recordedAt": "2026-08-07T14:52:37Z",
  "status": "implemented",
  "scope": {
    "project": "example-project",
    "modules": ["app/example"]
  },
  "summary": "One concise capability statement.",
  "origin": {
    "kind": "implementation-audit",
    "recordedFrom": ["source inspection on 2026-08-07"],
    "authority": "Snapshot only; referenced live source and tests are authoritative."
  },
  "evidence": [
    {
      "kind": "code",
      "path": "app/example/Feature.java",
      "lines": "12-34",
      "note": "Implements the capability boundary."
    }
  ],
  "markdownRef": [
    {
      "path": "audit.md",
      "role": "feature memo"
    }
  ],
  "tags": ["example", "feature"]
}
```
