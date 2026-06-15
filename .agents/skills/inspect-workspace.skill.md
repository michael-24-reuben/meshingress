# Inspect Sequence: Workspace Redundancy / Unnecessary Code Review

You are reviewing the current project workspace for unnecessary, redundant, duplicated, obsolete, over-complicated, or drift-prone code and project assets.

## Goal

Perform an inspection sequence that identifies code, configuration, scripts, documentation, generated artifacts, and workspace structure that can be removed, simplified, consolidated, cached, renamed, or moved into a cleaner abstraction without changing intended behavior.

This is **not** a feature implementation task. Do not rewrite large areas immediately. First inspect, classify, write a structured report under `architect/reports/`, update the report registry, and propose architect entries for follow-up implementation.

## Workspace Context

This skill is intended for a **project workspace**, not one fixed product or repository identity.

Treat the workspace as the source of truth. Before making assumptions, inspect the repository layout, package manifests, build files, source roots, scripts, tests, documentation, generated outputs, and `architect/` records.

The workspace may contain one or more of these areas:

```txt
apps/
app/
packages/
libs/
lib/
modules/
toolspace/
scripts/
examples/
docs/
architect/
.github/
.vscode/
.idea/
src/
test/
tests/
build/
dist/
target/
out/
```

Do not assume the project is only a backend, frontend, CLI, library, agent skill, Maven project, Node workspace, Spring app, or tool runtime. Determine the actual project shape first.

This project uses an `architect/` directory as structured engineering memory for planning records, investigations, design drafts, refactors, reports, and resolved work. Any meaningful finding should be traceable from a report into a pending or active architect entry so future agents can resume the work without needing the original chat context.

## Key Workspace Features To Identify First

Before inspecting for redundancy, identify these features where present:

1. **Workspace topology**
   - Root-level apps, libraries, packages, modules, tools, examples, docs, and generated folders.
   - Package/module boundaries and dependency direction.
   - Whether the workspace is single-project, monorepo, multi-module, or mixed-language.

2. **Build and package system**
   - Maven/Gradle files, `package.json`, workspace manifests, lockfiles, bundler configs, compiler configs, Dockerfiles, CI workflows, and release scripts.
   - Build outputs that should not be inspected as source unless intentionally checked in.

3. **Runtime entrypoints**
   - Application mains, server bootstrap files, CLI entrypoints, route/controller registration, plugin/module registration, worker/job startup, frontend mount points, and test harnesses.

4. **Public API and integration boundaries**
   - Exported modules/packages, CLI commands, HTTP routes, WebSocket endpoints, SDK surfaces, annotations/decorators, plugin interfaces, generated schemas, and documented extension points.

5. **Configuration surface**
   - Environment files, typed properties, config classes, config schemas, feature flags, default values, validation, secrets references, and documented setup steps.

6. **Data and schema contracts**
   - DTOs, records, interfaces, JSON schemas, OpenAPI specs, GraphQL schemas, database migrations, validation rules, serialized result formats, and event payloads.

7. **UI/frontend interaction paths**
   - Page entrypoints, routing, state stores, API clients, form generators, renderers, error handlers, and duplicated UI utilities.

8. **Tooling and automation**
   - Scripts, dev commands, generators, codegen, smoke tests, install helpers, local environment scripts, and agent/skill prompts.

9. **Tests and verification assets**
   - Unit tests, integration tests, smoke tests, fixtures, snapshots, mock data, example projects, and manual verification docs.

10. **Architect/report lifecycle state**
   - Existing `architect/pending/`, `architect/active/`, `architect/resolved/`, `architect/archived/`, `architect/reports/`, and `architect/reports/reports.registry.json` state.

## Inspection Scope

Inspect the workspace for:

1. **Duplicate logic**
   - Repeated builders, mappers, validators, adapters, serializers, parsers, schema generators, registry lookups, routing paths, dispatch paths, error wrappers, API clients, or file utilities.
   - Similar code across apps, libraries, packages, modules, tools, scripts, frontend, backend, and tests.

2. **Redundant lookups and repeated computation**
   - Repeated registry scans.
   - Repeated filesystem walks.
   - Repeated package/module discovery.
   - Repeated annotation/decorator/reflection introspection.
   - Repeated config/property reads.
   - Repeated schema generation or validation setup.
   - Repeated route/command/handler lookup.
   - Repeated JSON serialization/deserialization where typed or cached data already exists.
   - Repeated policy/security/permission evaluation that could be cached per request, per process, per module, or per lifecycle phase.

3. **Dead or obsolete code**
   - Classes, methods, functions, fields, constants, scripts, config files, test fixtures, examples, TODOs, and docs no longer referenced.
   - Old experimental paths replaced by newer runtime, build, CLI, UI, API, or module flows.
   - Generated/build outputs accidentally treated as source.
   - Deprecated migration paths with no active compatibility requirement.

4. **Overlapping abstractions**
   - Multiple classes/functions doing the same conceptual job.
   - Interfaces with only one implementation and no clear extension point.
   - DTOs/records/types that duplicate the same fields.
   - Result/error/response wrappers that overlap.
   - Multiple config loaders, route registries, API clients, schema builders, or logging adapters with incompatible conventions.

5. **Unnecessary complexity**
   - Deep call chains with no added behavior.
   - Premature extension points.
   - Reflection/dynamic loading where direct metadata already exists.
   - Overly generic APIs that hide simple behavior.
   - Manual object construction that duplicates existing helpers.
   - Scripts that reimplement package manager, build tool, or framework behavior without clear reason.

6. **Configuration drift**
   - Config properties declared but not consumed.
   - Docs mentioning settings that do not exist in code.
   - Defaults split across code, docs, environment examples, CI, and tests.
   - Feature flags that are declared but not enforced.
   - Security/permission/audit/secret settings declared but not applied.
   - Mechanical wiring gaps versus policy-design gaps.

7. **Contract/schema drift**
   - Types, schemas, annotations, decorators, route metadata, CLI metadata, or docs that disagree.
   - Fields that are declared but not read anywhere.
   - Metadata that is read but not reflected in public descriptors, generated docs, schemas, API responses, or UI forms.
   - ID/name/path validation duplicated in multiple places.

8. **Frontend/backend integration drift**
   - Frontend API calls that do not match backend routes.
   - Backend response shapes that do not match renderers or generated forms.
   - Duplicated client functions for the same endpoint.
   - Error handling split across multiple incompatible helpers.
   - Mock data or fixture payloads that no longer match runtime data.

9. **Package/module boundary drift**
   - Cross-module imports that bypass intended public exports.
   - Circular dependencies.
   - Package manifests exporting paths that no longer exist.
   - Internal code imported by external modules.
   - Library code depending on app-specific code.

10. **Documentation and generated artifact drift**
   - README instructions that no longer match commands or file layout.
   - Generated docs checked in but stale.
   - Duplicate docs describing the same setup differently.
   - Examples that no longer compile or run.

11. **Report-to-architect lifecycle gaps**
   - Findings that should become `architect/pending/` entries but are not linked.
   - Active implementation work that lacks a source report.
   - Resolved entries whose source report was not copied into the resolved entry folder.
   - Missing or stale report registry events.

## Important Caution

Do **not** mark code or files as removable only because ordinary static search shows no references.

A symbol or file may be used indirectly through:

- Dependency injection.
- Framework conventions.
- Route discovery.
- CLI command discovery.
- Plugin/module loading.
- Reflection.
- Annotations or decorators.
- Code generation.
- Build tool configuration.
- Package exports.
- Test fixtures or snapshots.
- Documentation examples.
- CI workflows.
- Shell scripts.
- Dynamic imports.
- Runtime-loaded modules.
- Public API / SPI contracts.
- External consumers not visible in the workspace.

Use conservative classification when indirect usage, generated usage, public API, or workspace lifecycle history is involved.

## Suggested Inspection Order

### Phase 1 — Build the workspace map

Identify the repository shape:

```txt
workspace root
  -> apps / packages / modules / libs / scripts / docs
  -> package/build manifests
  -> source roots
  -> test roots
  -> generated/build outputs
  -> architect lifecycle directories
```

Record the files/classes/modules involved in each area.

### Phase 2 — Build execution and integration maps

Identify the main runtime and development paths that exist in this workspace.

Examples:

```txt
application startup
  -> config binding/loading
  -> dependency/module discovery
  -> route/command/handler registration
  -> request/command execution
  -> validation/security/policy checks
  -> result serialization/rendering
```

```txt
frontend interaction
  -> page entrypoint
  -> state/init logic
  -> API client call
  -> backend route/controller
  -> response renderer
  -> error handler
```

```txt
CLI/tool execution
  -> command registry
  -> argument parsing
  -> handler dispatch
  -> side-effect boundary
  -> output formatting
```

```txt
build/test workflow
  -> dependency install
  -> code generation
  -> compile/bundle/package
  -> unit/integration/smoke tests
  -> artifact output
```

Only include maps that match the actual workspace.

### Phase 3 — Build a lookup map

Find all lookup/scanning/introspection points:

```txt
Filesystem scanning
Package/module discovery
Class/function discovery
Annotation/decorator scanning
Dependency injection lookup
Route lookup
Command lookup
Handler lookup
Registry lookup
Schema lookup/generation
Config/property lookup
Feature flag lookup
Permission/policy lookup
Secret reference lookup
Serializer/deserializer lookup
Frontend API-client lookup
Report registry lookup
Architect entry lookup
```

For each lookup, answer:

```txt
Where is it called?
How often can it run?
Is it repeated for the same input?
Is it startup-only, build-time, per-request, per-command, per-render, per-test, or report-lifecycle-only?
Can it be memoized safely?
Does it need invalidation for file changes, dynamic modules, generated code, hot reload, or runtime-loaded plugins?
Does it need invalidation when a report is promoted to pending/active/resolved?
```

### Phase 4 — Detect duplication

Search for repeated patterns:

```txt
Configuration loading/defaulting
Environment variable parsing
Route/controller registration
CLI command registration
Handler/dispatcher lookup
Permission/scope/policy evaluation
Schema/type/DTO generation
Request/response/result construction
Error result construction
Object/JSON builders
Frontend API clients
Frontend form/rendering helpers
Validation rules
Path/ID/name validation
Filesystem helpers
Logging/audit event writing
Build/test script logic
Report registry event writing
Architect lifecycle event writing
```

Group duplicate findings by behavior, not just by syntax.

### Phase 5 — Validate true usage

Before recommending removal:

1. Check direct source references.
2. Check imports/exports and package/module manifests.
3. Check build files, bundler config, compiler config, and workspace config.
4. Check route/CLI/plugin/framework registration mechanisms.
5. Check dependency injection, annotations, decorators, reflection, and generated code.
6. Check tests, fixtures, snapshots, examples, and smoke tests.
7. Check docs, READMEs, scripts, and CI workflows.
8. Check whether the symbol/file is part of public API, SPI, CLI, route, package export, or documented extension point.
9. Check `architect/reports/reports.registry.json` for linked report or architect state.
10. Check related architect entries before treating a path as obsolete.

### Phase 6 — Classify findings

For each finding, classify it as one of:

```txt
SAFE_REMOVE
SAFE_INLINE
SAFE_CONSOLIDATE
NEEDS_REFACTOR
NEEDS_ARCHITECT_DECISION
KEEP_PUBLIC_API
KEEP_DYNAMIC_USAGE
KEEP_FRAMEWORK_CONVENTION
KEEP_TEST_SUPPORT
KEEP_DOC_EXAMPLE
KEEP_REPORT_HISTORY
UNKNOWN_USAGE
```

Use conservative classification when framework conventions, dynamic loading, public API, generated code, or report history is involved.

### Phase 7 — Assign evidence strength

Every finding must include an evidence strength:

```txt
HIGH
MEDIUM
LOW
```

Use:

- `HIGH` when direct code evidence and usage checks support the finding.
- `MEDIUM` when evidence is strong but indirect usage or future design intent may exist.
- `LOW` when the finding is plausible but needs manual confirmation.

### Phase 8 — Decide report lifecycle actions

For every recommended refactor package, decide whether it should:

```txt
STAY_REPORT_ONLY
PROMOTE_TO_PENDING_ARCHITECT
PROMOTE_TO_ACTIVE_ARCHITECT
LINK_TO_EXISTING_ARCHITECT
BLOCKED_PENDING_DECISION
```

Do not create implementation code in this inspection pass. Create or recommend architect entries only.

## Report Storage Rules

Reports live in:

```txt
architect/reports/
```

Create a timestamped report file. Do not overwrite prior reports.

Recommended format:

```txt
architect/reports/workspace-inspection-YYYY-MM-DD-HHmmss.md
```

Example:

```txt
architect/reports/workspace-inspection-2026-05-28-160820.md
```

The report keeps its own date-time because multiple reports may target the same issue or produce different findings over time.

Optionally update a convenience pointer if the project uses one:

```txt
architect/reports/workspace-inspection.latest.md
```

## Report Registry Requirement

After writing the report, create or update:

```txt
architect/reports/reports.registry.json
```

The registry is machine-readable state for report tracking. It records reports, promotion state, related architect entries, and audit events.

### Registry Shape

Use this shape unless the repository already defines a stricter one:

```json
{
  "schemaVersion": 1,
  "updatedAt": "2026-05-28T16:08:20-04:00",
  "reports": [
    {
      "id": "workspace-inspection-2026-05-28-160820",
      "kind": "workspace-inspection",
      "title": "Workspace Redundancy / Unnecessary Code Inspection Report",
      "path": "architect/reports/workspace-inspection-2026-05-28-160820.md",
      "status": "reported",
      "createdAt": "2026-05-28T16:08:20-04:00",
      "updatedAt": "2026-05-28T16:08:20-04:00",
      "summary": {
        "totalFindings": 0,
        "safeRemovals": 0,
        "consolidationCandidates": 0,
        "lookupCacheCandidates": 0,
        "refactorCandidates": 0,
        "unknownRiskFindings": 0,
        "documentationDriftFindings": 0,
        "integrationDriftFindings": 0
      },
      "relatedArchitectEntries": [],
      "supersedes": [],
      "supersededBy": null,
      "events": [
        {
          "at": "2026-05-28T16:08:20-04:00",
          "type": "REPORT_CREATED",
          "note": "Initial workspace inspection report created."
        }
      ]
    }
  ]
}
```

### Report Status Values

Use these status values:

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

### Report Event Types

Use these event types:

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
REPORT_MOVED_TO_RESOLUTION
REGISTRY_AUDITED
```

## Report-to-Architect Transformation

Reports are inspection artifacts. Architect entries are execution artifacts.

When a report finding becomes planned work:

1. Create an entry under `architect/pending/YYYY-MM-DD-short-title/`, unless implementation begins immediately.
2. If implementation begins immediately, create it under `architect/active/YYYY-MM-DD-short-title/`.
3. Add normal architect files:

```txt
meta.json
brief.md
todo.md
context.md
```

4. Add `plan.md` if staged implementation is required.
5. In the entry `meta.json`, link back to the source report:

```json
{
  "sourceReports": [
    "workspace-inspection-2026-05-28-160820"
  ]
}
```

6. Update `architect/reports/reports.registry.json`:
   - Change the report status to `promoted_pending`, `promoted_active`, or `linked_existing`.
   - Add the architect entry ID to `relatedArchitectEntries`.
   - Append events such as `REPORT_PROMOTED_TO_PENDING`, `ARCHITECT_CREATED_PENDING`, `REPORT_PROMOTED_TO_ACTIVE`, or `ARCHITECT_CREATED_ACTIVE`.

## Resolution Report Placement

When an architect entry is resolved, preserve the source report inside the resolved entry folder.

Use this layout:

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

Example:

```txt
architect/resolved/2026-05-28-workspace-config-consolidation/report-2026-05-28-160820.md
```

The architect folder uses the entry creation date. The report file keeps the report timestamp because multiple reports may feed the same architect entry.

After copying or moving the report into the resolved folder:

1. Update `reports.registry.json` with status `resolved`.
2. Add `ARCHITECT_RESOLVED`.
3. Add `REPORT_COPIED_TO_RESOLUTION` if the original remains in `architect/reports/`.
4. Add `REPORT_MOVED_TO_RESOLUTION` only if the original report was actually moved out of `architect/reports/`.

Prefer copying over moving so `architect/reports/` remains a stable report history.

## Report Format

Return a Markdown report with this structure:

```md
# Workspace Redundancy / Unnecessary Code Inspection Report

## Report Metadata

- Report ID:
- Created At:
- Source Branch:
- Commit / Revision:
- Inspector:
- Registry Path: `architect/reports/reports.registry.json`

## Summary

- Total findings:
- Safe removals:
- Consolidation candidates:
- Lookup/cache candidates:
- Refactor candidates:
- Documentation drift findings:
- Integration drift findings:
- Unknown-risk findings:

## Workspace Map

| Area | Paths | Purpose | Notes |
|---|---|---|---|

## Execution / Integration Maps

| Flow | Entry Point | Main Files | Notes |
|---|---|---|---|

## Lookup Hotspots

| Lookup | Location | Frequency | Risk | Evidence Strength | Recommendation |
|---|---|---:|---|---|---|

## Duplicate Logic Findings

| ID | Area | Files | Problem | Evidence Strength | Recommendation | Risk |
|---|---|---|---|---|---|---|

## Dead / Obsolete Code Candidates

| ID | Symbol / File | Location | Evidence | Evidence Strength | Classification | Action |
|---|---|---|---|---|---|---|

## Overlapping Abstractions

| ID | Abstractions | Overlap | Evidence Strength | Recommendation |
|---|---|---|---|---|

## Configuration Drift

Split findings into:

### Mechanical Wiring Gaps

| Config / Setting | Declared In | Consumed By | Gap | Evidence Strength |
|---|---|---|---|---|

### Policy Design Gaps

| Config / Setting | Declared In | Expected Policy | Gap | Evidence Strength |
|---|---|---|---|---|

## Contract / Schema Drift

| Contract / Field | Expected Use | Actual Use | Gap | Evidence Strength |
|---|---|---|---|---|

## Frontend / Backend Integration Drift

| ID | Frontend Location | Backend Location | Mismatch | Evidence Strength | Recommendation |
|---|---|---|---|---|---|

## Package / Module Boundary Drift

| ID | Boundary | Problem | Evidence Strength | Recommendation |
|---|---|---|---|---|

## Documentation / Generated Artifact Drift

| ID | File | Problem | Evidence Strength | Recommendation |
|---|---|---|---|---|

## Report Lifecycle Findings

| ID | Report / Entry | Problem | Recommendation | Registry Event Needed |
|---|---|---|---|---|

## Do Not Change Without Design Decision

List symbols, APIs, files, package exports, routes, commands, configs, or flows that must not be deleted or changed until a design decision is captured.

## Recommended Refactor Packages

### Package 1 — [short name]

**Goal:**  
**Files affected:**  
**Why:**  
**Risk:**  
**Evidence strength:**  
**Lifecycle action:** `STAY_REPORT_ONLY | PROMOTE_TO_PENDING_ARCHITECT | PROMOTE_TO_ACTIVE_ARCHITECT | LINK_TO_EXISTING_ARCHITECT | BLOCKED_PENDING_DECISION`  
**Verification:**  

## Architect Entries To Create

List proposed architect entries using:

```txt
architect/pending/YYYY-MM-DD-short-title/
architect/active/YYYY-MM-DD-short-title/
```

Each entry should include:

```txt
meta.json
brief.md
todo.md
context.md
```

Add `plan.md` if the finding requires staged implementation.

## Verification Plan

Include only checks that apply to this workspace:

- [ ] Run the project test suite.
- [ ] Run relevant smoke tests.
- [ ] Run the project build/package command.
- [ ] Run lint/typecheck/static analysis if available.
- [ ] Verify runtime entrypoints still start.
- [ ] Verify CLI commands still route correctly if applicable.
- [ ] Verify frontend pages still call the expected backend functions if applicable.
- [ ] Verify backend routes/controllers still return expected contracts if applicable.
- [ ] Verify package/module exports still resolve.
- [ ] Verify dynamic/plugin/framework-discovered code still registers if applicable.
- [ ] Verify generated schemas/docs are current if applicable.
- [ ] Verify config defaults still bind/load correctly.
- [ ] Verify `architect/reports/reports.registry.json` is valid JSON.
- [ ] Verify every promoted report has a related architect entry.
- [ ] Verify resolved architect entries include source report copies when applicable.
```

## Output Requirements

Be precise. Do not say "probably unused" without evidence.

For every finding, include:

```txt
File path
Class/function/method/symbol/file name
Evidence
Evidence strength
Why it is redundant or unnecessary
Whether it is safe to remove now
What could break
How to verify
Lifecycle action
```

## Hard Rules

- Do not delete or rewrite code during the first pass.
- Do not remove public API unless explicitly approved.
- Do not remove package exports, CLI commands, routes, annotations, decorators, framework registration files, or config files without validating indirect usage.
- Do not remove generated files unless the workspace clearly treats them as disposable build outputs.
- Do not remove tests, fixtures, or examples without checking whether they document behavior or support compatibility.
- Do not remove report history unless the registry records the move/archive event.
- Prefer small refactor packages over one massive cleanup.
- Any non-trivial cleanup must become an `architect/` entry before implementation.
- Any report promoted to pending/active architect work must be linked in both the report registry and the architect entry metadata.
- When resolved, copy the source report into the resolved architect entry folder as `report-YYYY-MM-DD-HHmmss.md`.

## Output File Requirement

Write the final inspection report to a Markdown file instead of only printing it in chat/output.

Create a new timestamped report:

```txt
architect/reports/workspace-inspection-YYYY-MM-DD-HHmmss.md
```

Then update:

```txt
architect/reports/reports.registry.json
```

After writing the file and updating the registry, print only:

```txt
Inspection report written to architect/reports/workspace-inspection-YYYY-MM-DD-HHmmss.md
Report registry updated at architect/reports/reports.registry.json
```
