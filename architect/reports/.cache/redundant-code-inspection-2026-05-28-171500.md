# Redundant Code / Lookup Inspection Report

## Report Metadata

- Report ID: redundant-code-inspection-2026-05-28-171500
- Created At: 2026-05-28T17:15:00-04:00
- Source Branch: unknown (not checked)
- Commit / Revision: unknown (not checked)
- Inspector: automated inspection
- Registry Path: `architect/reports/reports.registry.json`

## Summary

- Total findings: 15
- Safe removals: 0
- Consolidation candidates: 2
- Lookup/cache candidates: 3
- Refactor candidates: 3
- Unknown-risk findings: 7

## Execution Map

| Phase | Classes / Files | Notes |
|---|---|---|
| startup configuration | `lib/meshingress-config/src/main/java/dev/mrk/meshingress/config/MeshingressProperties.java` | Central configuration defaults and binding. |
| tool discovery / registry scan | `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/registry/InMemoryToolRegistry.java` | Startup scan controlled by `meshingress.tools.registry.scan-on-startup`. |
| descriptor generation | `lib/meshingress-tool-framework/src/main/java/dev/mrk/meshingress/tools/framework/scanning/McpToolAnnotationScanner.java` | Reflection-based annotation scanning and schema generation. |
| MCP tools/list | `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/tools/ToolsMcpController.java` | Lists `ToolRegistry.listPublicEnabledFunctions()`. |
| MCP tools/call | `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/tools/ToolsMcpController.java` + `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/DefaultToolExecutor.java` | Validates params, resolves function/handler, enforces scopes, executes tool. |
| dispatch handler | `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/McpDispatcher.java` | JSON-RPC validation, dispatch lookup, error wrapping. |
| transport parse + response | `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/McpController.java`, `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/McpWebSocketHandler.java` | Separate parse/error handling for HTTP and WebSocket. |

## Lookup Hotspots

| Lookup | Location | Frequency | Risk | Evidence Strength | Recommendation |
|---|---|---:|---|---|---|
| Function-to-tool ownership scan | `InMemoryToolRegistry#findEnabledFunction` calls `owningTool(...)` which streams all descriptors | per tools/call | medium | HIGH | Cache a function->toolName map during registration to avoid per-call descriptor scanning. |
| Function descriptor + handler lookup | `DefaultToolExecutor#execute` uses `findEnabledFunction` then `findHandler` | per tools/call | low | MEDIUM | Consider storing handler reference in the registry or returning a combined lookup result. |
| Public function list sort/filter | `InMemoryToolRegistry#listPublicEnabledFunctions` filters+sorts on each call | per tools/list | low | MEDIUM | Memoize public list by registry version to avoid repeat sort/filter when registry is unchanged. |

## Duplicate Logic Findings

| ID | Area | Files | Problem | Evidence Strength | Recommendation | Risk |
|---|---|---|---|---|---|---|
| DL-1 | JSON parse + parse-error response | `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/McpController.java`, `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/McpWebSocketHandler.java` | Both parse JSON with `ObjectMapper.readTree` and wrap parse errors using `JsonRpcResponses`. | HIGH | Factor a shared transport helper for parse + dispatch + error handling. | low |
| DL-2 | Visibility allow/deny logic | `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/registry/InMemoryToolRegistry.java` | `toolVisibleToPublic`/`functionVisibleToPublic` and `toolAllowedForCall`/`functionAllowedForCall` repeat identical checks. | HIGH | Consolidate visibility checks to a shared helper to reduce drift. | low |

## Dead / Obsolete Code Candidates

| ID | Symbol | File | Evidence | Evidence Strength | Classification | Action |
|---|---|---|---|---|---|---|
| D-1 | `toolsRegister(...)` | `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/tools/ToolsMcpController.java` | Method only logs and returns `{}`; no internal usage beyond MCP routing. | HIGH | NEEDS_ARCHITECT_DECISION | Decide whether `tools/register` is still intended or should be removed/redirected to role-gated registration. |
| D-2 | `DelegatingToolRegistry` | `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/registry/DelegatingToolRegistry.java` | No references found outside its own file. | HIGH | UNKNOWN_USAGE | Confirm whether this is a planned extension point; remove or move to test utilities if unused. |

## Overlapping Abstractions

| ID | Abstractions | Overlap | Evidence Strength | Recommendation |
|---|---|---|---|---|
| OA-1 | HTTP MCP controller vs WebSocket handler | Both perform parse + error + dispatch around the same `McpDispatcher` usage. | HIGH | Introduce a shared transport helper to reduce duplication and drift. |

## Configuration Drift

### Mechanical Wiring Gaps

| Property / Group | Declared In | Consumed By | Gap | Evidence Strength |
|---|---|---|---|---|
| `meshingress.tools.default-timeout`, `meshingress.tools.default-audit`, `meshingress.tools.default-debug-trace` | `lib/meshingress-config/src/main/java/dev/mrk/meshingress/config/MeshingressProperties.java` | Tests only (`ToolRegistrationServiceTests`) | No runtime usage. | HIGH |
| `meshingress.tools.registration.require-approval-for-dynamic-phases` | `lib/meshingress-config/src/main/java/dev/mrk/meshingress/config/MeshingressProperties.java` | Tests only (`ToolRegistrationServiceTests`) | No runtime enforcement in registration flows. | HIGH |
| `meshingress.security.require-tool-approval`, `require-approval-for-privileged`, `require-approval-for-critical`, `default-deny` | `lib/meshingress-config/src/main/java/dev/mrk/meshingress/config/MeshingressProperties.java` | none found | Flags are declared but not consulted in runtime code. | HIGH |
| `meshingress.scopes.audit-required-for-high-risk`, `explicit-approval-for-critical` | `lib/meshingress-config/src/main/java/dev/mrk/meshingress/config/MeshingressProperties.java` | none found | Scope policy flags declared but unused. | HIGH |
| `meshingress.secrets.allow-env`, `allow-file`, `allow-inline`, `fail-on-missing` | `lib/meshingress-config/src/main/java/dev/mrk/meshingress/config/MeshingressProperties.java` | none found | Secret handling flags declared but unused; only `redactionPlaceholder` is used. | HIGH |

### Policy Design Gaps

| Property / Group | Declared In | Expected Policy | Gap | Evidence Strength |
|---|---|---|---|---|
| Security approval flags | `MeshingressProperties.Security` | Approval gating for privileged/critical tools | No enforcement in executor/registry/security services. | MEDIUM |
| Scope audit flags | `MeshingressProperties.Scopes` | Require audit/explicit approval for high-risk scopes | No enforcement in executor or scope policy services. | MEDIUM |

## Annotation / Schema Drift

| Annotation / Field | Expected Use | Actual Use | Gap | Evidence Strength |
|---|---|---|---|---|
| `@McpInputSchema.description` (method level) | Method-level schema description should flow into generated input schema. | `inputSchemaFor(...)` only checks `provider` and ignores `description` when using default provider. | Method-level description is unused unless a custom provider is supplied. | HIGH |

## Report Lifecycle Findings

| ID | Report / Entry | Problem | Recommendation | Registry Event Needed |
|---|---|---|---|---|
| RL-1 | `architect/reports/redundant-code-inspection-2026-28-05-160820.md` | No registry entry exists yet because `reports.registry.json` is missing. | Backfill registry entry for the existing report or mark it as superseded. | REPORT_UPDATED / REGISTRY_AUDITED |

## Do Not Change Without Design Decision

- `lib/meshingress-tool-api` public SPI (`McpToolHandler`, `McpToolDescriptor`, annotations) because tool modules depend on it.
- Runtime tool registration paths (`ToolRegistry.registerRuntimeHandler`, `unregisterRuntimeOwner`) because dynamic tools depend on these APIs.
- Dispatcher JSON-RPC error behavior (`JsonRpcResponses`, `McpDispatcher`) because clients rely on envelope stability.
- Annotation scanning flow (`McpToolAnnotationScanner`) because reflection and schema generation are part of public behavior.

## Recommended Refactor Packages

### Package 1 — Transport parsing consolidation

**Goal:** Reduce duplicate JSON parse + error handling between HTTP and WebSocket MCP transports.  
**Files affected:** `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/McpController.java`, `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/McpWebSocketHandler.java`, new shared helper under `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/`.  
**Why:** Same parse/dispatch/error pattern repeated in two transport handlers.  
**Risk:** low.  
**Evidence strength:** HIGH.  
**Lifecycle action:** PROMOTE_TO_PENDING_ARCHITECT  
**Verification:** POST `/mcp` still handles parse errors and valid dispatch; WS path still handles payload size + parse errors.

### Package 2 — Registry visibility filter cleanup

**Goal:** Consolidate repeated visibility/allowlist logic in `InMemoryToolRegistry`.  
**Files affected:** `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/registry/InMemoryToolRegistry.java`.  
**Why:** Duplicate logic across tool/function visibility and call eligibility increases drift risk.  
**Risk:** low.  
**Evidence strength:** HIGH.  
**Lifecycle action:** PROMOTE_TO_PENDING_ARCHITECT  
**Verification:** `tools/list` output unchanged; `tools/call` allow/deny behavior unchanged.

### Package 3 — Config drift audit

**Goal:** Decide whether to implement or remove unused properties.  
**Files affected:** `lib/meshingress-config/src/main/java/dev/mrk/meshingress/config/MeshingressProperties.java`, related services.  
**Why:** Multiple properties are declared but not consumed.  
**Risk:** medium.  
**Evidence strength:** HIGH.  
**Lifecycle action:** PROMOTE_TO_PENDING_ARCHITECT  
**Verification:** Configuration binding still works; no missing property references in runtime logs.

## Architect Entries To Create

- `architect/pending/2026-05-28-transport-parsing-consolidation/`
- `architect/pending/2026-05-28-registry-visibility-cleanup/`
- `architect/pending/2026-05-28-config-drift-audit/`

Each entry should include:

```
meta.json
brief.md
todo.md
context.md
```

Add `plan.md` if staged implementation is required.

## Verification Plan

- [ ] Run Maven tests.
- [ ] Run smoke tests.
- [ ] Verify `tools/list`.
- [ ] Verify `tools/call`.
- [ ] Verify dynamic/runtime-loaded tools still register.
- [ ] Verify annotations still produce equivalent descriptors.
- [ ] Verify config defaults still bind correctly.
- [ ] Verify `architect/reports/reports.registry.json` is valid JSON.
- [ ] Verify every promoted report has a related architect entry.
- [ ] Verify resolved architect entries include source report copies when applicable.

[//]: # (
{
  "id": "redundant-code-inspection-2026-05-28-171500",
  "kind": "redundant-code-inspection",
  "title": "Redundant Code / Lookup Inspection Report",
  "path": "architect/reports/redundant-code-inspection-2026-05-28-171500.md",
  "status": "promoted_active",
  "createdAt": "2026-05-28T17:15:00-04:00",
  "updatedAt": "2026-05-28T18:26:00-04:00",
  "relatedArchitectEntries": [
    "2026-05-28-transport-parsing-consolidation",
    "2026-05-28-registry-visibility-cleanup",
    "2026-05-28-config-drift-audit"
  ],
  "supersedes": [
    "redundant-code-inspection-2026-28-05-160820"
  ],
  "supersededBy": null,
  "events": [
    {
      "at": "2026-05-28T18:26:00-04:00",
      "type": "ARCHITECT_RESOLVED",
      "note": "Resolved transport parsing consolidation and registry visibility cleanup."
    },
    {
      "at": "2026-05-28T18:26:00-04:00",
      "type": "REPORT_COPIED_TO_RESOLUTION",
      "note": "Copied source report into resolved transport and registry entries."
    }
  ]
}
)