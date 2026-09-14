# Workspace Redundancy / Unnecessary Code Inspection Report

## Report Metadata

- Report ID: `workspace-inspection-2026-09-07-113523`
- Created At: `2026-09-07T11:35:23-04:00`
- Source Branch: `development`
- Inspector: Codex
- Scope: `app/meshingress-studio-web/src/features/workflow-studio` state-model and Honeycomb renderer boundary only.
- Registry Path: `architect/reports/reports.registry.json`

## Summary

- Total findings: 4
- Safe removals: 0
- Consolidation candidates: 3
- Lookup/cache candidates: 0
- Refactor candidates: 0
- Architect-record drift findings: 1

This is not an unused-code sweep. The legacy scene parser, scene creator, scene fallback props, and every `WorkflowStudioState` method are intentionally outside removal scope.

## Workspace Map

| Area | Paths | Purpose | Notes |
|---|---|---|---|
| State contract | `states/WorkflowStudioState.ts` | Snapshot phases, root model identity, subscriptions, mutations, renderer attributes | Preferred and preserved state boundary. |
| State factory | `states/factories/WorkflowModelSnapshotFactory.ts` | Creates stable renderer-neutral state identities from fixture data | Does not own Honeycomb JSX. |
| Scene Builder | `WorkflowCanvasSceneBuilderPage.tsx` | Hosts the factory, subscribes for rerendering, and presents mutation controls | Direct model consumer. |
| Honeycomb renderer | `canvas/honeycomb-prototype/**` | Renders model attributes and retains scene fallback compatibility | Renderer remains responsible for geometry and JSX. |
| Legacy compatibility | `states/workflow-canvas-scene.ts` | Scene parsing, creation, and fallback scene types | Kept intact; not classified as unused or removable. |

## Execution / Integration Maps

| Flow | Entry Point | Main Files | Notes |
|---|---|---|---|
| Scene Builder state update | `WorkflowCanvasSceneBuilderPage` | Page -> `WorkflowModelSnapshot` -> snapshot listeners -> React reducer -> Honeycomb | Phase changes are model-driven. Capability changes retain an explicit rerender because they are data changes, not phase changes. |
| Renderer attribute projection | Honeycomb snapshot components | `WorkflowModelSnapshot#get*Model()` -> `getDataAttributes()` | Legacy `scene` attributes remain a compatibility fallback. |

## Duplicate Logic Findings

| ID | Area | Files | Problem | Evidence Strength | Recommendation | Risk |
|---|---|---|---|---|---|---|
| DUP-01 | Attribute names | `WorkflowStudioState.ts`, `WorkflowCanvasSceneBuilderPage.tsx` | Both independently convert camel-case state categories to `data-*` names. | HIGH | Export one state-bound helper and have the page consume it. | Low; pure string derivation. |
| DUP-02 | Demo placements | `WorkflowCanvasSceneBuilderPage.tsx`, `HoneycombWorkflowCanvasInspiration.tsx` | The same four prototype placements are declared in both the integrated page and standalone Honeycomb default. | HIGH | Export the Honeycomb default placements and consume that single list from the page. | Low; values remain unchanged. |
| DUP-03 | Factory initialization | `WorkflowModelSnapshotFactory.ts` | Placements are traversed once to construct slots, then again solely to set every slot occupied. | HIGH | Set occupancy while constructing each slot model. | Low; no listeners exist while the factory initializes models. |

## Contract / Architect Drift

| ID | Contract / Record | Expected Use | Actual Use | Evidence Strength |
|---|---|---|---|---|
| DRIFT-01 | `2026-09-05-workflow-studio-scene-state-ownership` | The record states that `parseWorkflowStudioScene(input)` exists in `WorkflowStudioState.ts`. | No such symbol exists in the live source search. The legacy `parseWorkflowCanvasScene(input)` remains. | HIGH |

This is recorded as a parity objective, not repaired in this cleanup slice. Reintroducing or relocating parse behavior needs an explicit design decision because it changes the preferred state-file contract.

## Do Not Change Without Design Decision

- Do not delete, rename, or remove methods from `WorkflowStudioState.ts`.
- Do not delete `parseWorkflowCanvasScene` or `createWorkflowCanvasScene`.
- Do not remove Honeycomb's optional legacy `scene` compatibility input.
- Do not alter phase batching, notification semantics, overlay exclusivity, or public mutation contracts while consolidating presentation-only duplication.
- Do not turn node-capability data changes into phase events in this slice; that requires a deliberate event-contract extension.

## Recommended Refactor Package

### Package 1 — State-model presentation consolidation

**Goal:** Remove exact duplicate derivations and fixture data without changing behavior or deleting API surface.

**Files affected:**

- `states/WorkflowStudioState.ts`
- `states/factories/WorkflowModelSnapshotFactory.ts`
- `WorkflowCanvasSceneBuilderPage.tsx`
- `canvas/honeycomb-prototype/HoneycombWorkflowCanvasInspiration.tsx`

**Risk:** Low.

**Evidence strength:** HIGH.

**Lifecycle action:** `LINK_TO_EXISTING_ARCHITECT`

**Verification:** Run the focused Vitest contract suite and the Studio build.

## Execution Outcome

Package 1 completed on `2026-09-07`.

- `workflowStudioStateAttributeName` is now the shared state-bound attribute-name derivation used by the snapshot base and Scene Builder catalog.
- `defaultHoneycombPrototypePlacements` is now the one prototype placement list consumed by both the standalone Honeycomb default and Scene Builder.
- Slot occupancy now initializes during the factory's existing placement-to-slot construction pass.

No `WorkflowStudioState` method, legacy parser/creator, or scene fallback was deleted or changed.

Verification completed:

- `npm test` — 1 file and 2 tests passed.
- `npm run build` — passed. Vite emitted the existing advisory for a JavaScript chunk above 500 kB.

## Follow-up Objectives

1. Add model-level contract coverage for all mutation categories, emitted target payloads, and projected attributes.
2. Decide whether state-side parsing is required; if so, define parity with the retained legacy parser before implementation.
3. Replace state-only factory subclasses with stable Honeycomb-backed model identities only after their ownership/lifecycle contract is approved.
4. Migrate all legacy scene fallback type consumers before considering removal of `workflow-canvas-scene.ts`.
