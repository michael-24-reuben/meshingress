# Inspect Sequence: Redundant / Unnecessary Code Review

You are reviewing this codebase for unnecessary, redundant, duplicated, obsolete, or over-complicated code.

## Goal

Perform an inspection sequence that identifies code that can be removed, simplified, consolidated, or moved into a cleaner abstraction without changing runtime behavior.

This is **not** a feature implementation task. Do not rewrite large areas immediately. First inspect, classify, and produce a structured report.

## Project Context

This project uses an `architect/` directory as structured engineering memory for planning records, investigations, design drafts, refactors, and resolved work. Any meaningful finding should be written as an architect entry so future agents can resume the work without needing the original chat context. The architect structure is intended to answer what was being worked on, why it mattered, what was known, what decisions were made, what changed, how it was verified, and what remains unresolved. :contentReference[oaicite:0]{index=0}

Meshingress tool modules expose MCP tools through Maven modules and Spring auto-configuration. Tool classes commonly use annotations such as `@McpTool`, `@McpFunction`, `@McpToolMapping`, `@McpToolScopes`, and `@McpConfigureMapping`, so inspection must account for reflection, annotation scanning, and auto-discovery before marking code as unused. :contentReference[oaicite:1]{index=1}

## Inspection Scope

Inspect the repository for:

1. **Duplicate logic**
   - Repeated builders, mappers, validators, schema generators, registry lookups, dispatch paths, or annotation parsing logic.
   - Similar code across tool runtime, dispatch, registry, availability, schema, config, and result modules.

2. **Redundant lookups**
   - Repeated registry scans.
   - Repeated annotation introspection.
   - Repeated classpath/package scanning.
   - Repeated ObjectMapper conversions.
   - Repeated config/property reads.
   - Repeated tool/function descriptor resolution.
   - Repeated policy/scope evaluation that could be cached per request, per tool, or per function.

3. **Dead or obsolete code**
   - Classes, methods, fields, constants, or TODOs no longer referenced.
   - Old experimental paths replaced by current runtime loader, registry, or dispatch flow.
   - Code that only exists for prior architecture but no longer participates in current execution.

4. **Overlapping abstractions**
   - Multiple classes doing the same conceptual job.
   - Interfaces with only one implementation and no clear extension point.
   - DTOs/records that duplicate the same fields.
   - Result or error wrappers that overlap with `DispatchExecutionResult` / `ResultContent`.

5. **Unnecessary complexity**
   - Deep call chains with no added behavior.
   - Premature extension points.
   - Reflection where direct metadata is already available.
   - Overly generic APIs that hide simple behavior.
   - Manual JSON construction that duplicates schema/result helpers.

6. **Configuration drift**
   - Properties modeled in `MeshingressProperties` but not consumed.
   - TODO properties that do not map cleanly to runtime behavior.
   - Defaults split across code, docs, and properties.
   - Security/scope/audit settings that are declared but not enforced.

7. **Annotation and schema drift**
   - Annotation fields that are not read anywhere.
   - Annotation metadata that is read but not reflected in descriptors/schemas.
   - Input schema generation that duplicates `@McpInputField`, `@McpFunctionParam`, or `@McpInputSchema` handling.
   - Tool/function ID validation duplicated in multiple places.

## Important Caution

Do **not** mark code as removable only because ordinary static search shows no references.

This project uses:
- Spring auto-configuration.
- Annotation scanning.
- Reflection.
- Tool discovery.
- MCP handler interfaces.
- Runtime-loaded tool modules.

A class may be used indirectly through annotations, Spring beans, configuration imports, reflection, or generated descriptors.

## Suggested Inspection Order

### Phase 1 — Build the execution map

Identify the main runtime paths:

```txt
startup
  -> configuration binding
  -> tool discovery / registry scan
  -> descriptor generation
  -> MCP tools/list
  -> MCP tools/call
  -> dispatch handler
  -> availability / scope / security checks
  -> invocation
  -> DispatchExecutionResult serialization
````

Record the files/classes involved in each stage.

### Phase 2 — Build a lookup map

Find all lookup/scanning/introspection points:

```txt
Class scanning
Annotation scanning
Spring bean lookup
Tool registry lookup
Function registry lookup
Descriptor lookup
Scope lookup
Availability policy lookup
Secret/config lookup
ObjectMapper conversion
JSON schema generation
```

For each lookup, answer:

```txt
Where is it called?
How often can it run?
Is it repeated for the same input?
Is it startup-only, per-request, per-tool-call, or per-function-call?
Can it be memoized safely?
Does it need invalidation for dynamic jars / runtime tool loading?
```

### Phase 3 — Detect duplication

Search for repeated patterns:

```txt
@McpTool parsing
@McpFunction parsing
@McpToolScopes merging
@McpConfigureMapping parsing
McpSecret resolution
Availability condition/policy evaluation
DispatchExecutionResult creation
ResultContent serialization
Tool ID / function ID validation
ObjectNode / ArrayNode builders
Error result construction
Timeout/audit/debug config resolution
```

Group duplicate findings by behavior, not just by syntax.

### Phase 4 — Validate true usage

Before recommending removal:

1. Check direct Java references.
2. Check annotation references.
3. Check Spring `@Bean`, `@Component`, `@AutoConfiguration`.
4. Check `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
5. Check tests and smoke tests.
6. Check docs and tool module examples.
7. Check runtime loader / dynamic jar paths.
8. Check whether the class is part of public API.

### Phase 5 — Classify findings

For each finding, classify it as one of:

```txt
SAFE_REMOVE
SAFE_INLINE
SAFE_CONSOLIDATE
NEEDS_REFACTOR
NEEDS_ARCHITECT_DECISION
KEEP_PUBLIC_API
KEEP_REFLECTION_USED
KEEP_TEST_SUPPORT
UNKNOWN_USAGE
```

Use conservative classification when reflection or runtime loading is involved.

## Report Format

Return a Markdown report with this structure:

````md
# Redundant Code / Lookup Inspection Report

## Summary

- Total findings:
- Safe removals:
- Consolidation candidates:
- Lookup/cache candidates:
- Refactor candidates:
- Unknown-risk findings:

## Execution Map

| Phase | Classes / Files | Notes |
|---|---|---|

## Lookup Hotspots

| Lookup | Location | Frequency | Risk | Recommendation |
|---|---|---:|---|---|

## Duplicate Logic Findings

| ID | Area | Files | Problem | Recommendation | Risk |
|---|---|---|---|---|---|

## Dead / Obsolete Code Candidates

| ID | Symbol | File | Evidence | Classification | Action |
|---|---|---|---|---|---|

## Overlapping Abstractions

| ID | Abstractions | Overlap | Recommendation |
|---|---|---|---|

## Configuration Drift

| Property / Group | Declared In | Consumed By | Gap |
|---|---|---|---|

## Annotation / Schema Drift

| Annotation / Field | Expected Use | Actual Use | Gap |
|---|---|---|---|

````

## Recommended Refactor Packages

### Package 1 — [short name]

**Goal:**  
**Files affected:**  
**Why:**  
**Risk:**  
**Verification:**  

## Architect Entries To Create

List proposed architect entries using:

```txt
architect/pending/YYYY-MM-DD-short-title/
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

* [ ] Run Maven tests.
* [ ] Run smoke tests.
* [ ] Verify `tools/list`.
* [ ] Verify `tools/call`.
* [ ] Verify dynamic/runtime-loaded tools still register.
* [ ] Verify annotations still produce equivalent descriptors.
* [ ] Verify config defaults still bind correctly.


## Output Requirements

Be precise. Do not say "probably unused" without evidence.

For every finding, include:

```txt
File path
Class/method/symbol name
Evidence
Why it is redundant or unnecessary
Whether it is safe to remove now
What could break
How to verify
```

## Hard Rules

* Do not delete or rewrite code during the first pass.
* Do not remove public API unless explicitly approved.
* Do not remove annotation classes just because references are indirect.
* Do not remove Spring configuration classes without checking auto-configuration imports.
* Do not remove runtime loader paths without checking dynamic tool/module behavior.
* Prefer small refactor packages over one massive cleanup.
* Any non-trivial cleanup must become an `architect/` entry before implementation.

Add this section to the prompt:

## Output File Requirement

Write the final inspection report to a Markdown file instead of only printing it in chat/output.

Create or overwrite:

```txt
architect/reports/redundant-code-inspection.md
````

The file must contain the full report, including:

* summary
* execution map
* lookup hotspots
* duplicate logic findings
* dead/obsolete code candidates
* overlapping abstractions
* configuration drift
* annotation/schema drift
* recommended refactor packages
* proposed architect entries
* verification plan

After writing the file, print only:

```txt
Inspection report written to architect/reports/redundant-code-inspection.md
```
