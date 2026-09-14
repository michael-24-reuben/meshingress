# Context

## Historical baseline before tracker retirement

`states/workflow-canvas-scene.ts` currently owns more than renderer attributes:

- aggregate identity and data records for canvas, workflow, slots, nodes, and
  paths;
- default scene creation from nodes, edges, and placements;
- runtime shape, category, and enum validation;
- normalized `data-*` attribute projection, including multi-value node
  overlays;
- typed trigger dispatch, cloned snapshots, and subscriptions;
- cross-element selection exclusivity and node capability attributes.

`WorkflowStudioState.ts` currently provides useful per-component phase APIs
for canvas, workflow, slots, and nodes. It does not yet provide a root identity
map, path model, aggregate notification stream, or scene-to-renderer contract.

`WorkflowNode` owns required `capabilities.acceptsInput` and
`capabilities.acceptsOutput` booleans. The node snapshot projects them as
renderer attributes. They remain node-data updates rather than phase-state
mutations, and they are not yet derived from an authoritative tool/port
contract.

## Initial parse-only bridge

`parseWorkflowStudioScene(input)` independently validates and normalizes a
scene-shaped input in `WorkflowStudioState.ts`. It mirrors the legacy parser's
target/category/enum rules, cloned-result behavior, and `data-*` projection for
valid inputs. It intentionally does not create defaults, dispatch mutations,
subscribe listeners, or replace callers. The original
`parseWorkflowCanvasScene(input)` remains active for all existing callers.

## Architectural boundary

Do not use direct/private-field access between snapshot classes. It would
create an unstable second contract around mutable phase maps and cached JSX.
Prefer explicit public, read-only identity and attribute-projection APIs.

## 2026-09-07 current state

`WorkflowCanvasSceneEventTracker` and its adapter boundary have been retired.
The Scene Builder creates one stable `WorkflowModelSnapshot` through
`WorkflowModelSnapshotFactory`, subscribes to it for phase-triggered React
rerenders, and passes the model to Honeycomb for attribute projection.

`workflow-canvas-scene.ts` remains intentionally present. Its parser, creator,
and scene-shaped fallback types still support the Honeycomb compatibility path.
Do not treat absence of direct Scene Builder use as authorization to delete it.

The older record briefly referred to `parseWorkflowStudioScene(input)` before
the export existed in live source. The parse-only implementation and its parity
tests now resolve that record drift without changing legacy callers.

## Honeycomb identity/lifecycle review before interaction routing

`WorkflowCanvasSceneBuilderPage` correctly retains one
`WorkflowModelSnapshot` in a React `useRef` and subscribes to its root phase
changes. Honeycomb does not yet preserve that same identity for interaction:
each page render calls `HoneycombWorkflowCanvasInspiration(...)`, which creates
a new `HoneycombCanvasModelSnapshot`; its constructor creates new slot and node
snapshot subclasses. Those subclasses own separate phase sets and their pointer,
focus, and click handlers mutate the renderer-local node snapshot instead of
the root model.

The required next boundary is a render-only Honeycomb adapter. It must look up
the root model's node/slot/path snapshots by stable ID, render their attributes,
and route user interactions through `WorkflowModelSnapshot.applyMutation(...)`
or its public convenience methods. Geometry, tooltip position, and other
ephemeral UI values may remain React-local state. Do not make the old
`WorkflowCanvasComponent` tracker-based lifecycle sketch authoritative: the
tracker is retired, and choosing a replacement lifecycle contract needs explicit
approval.

`HoneycombWorkflowModelAdapter` now covers the narrow interaction boundary for
integrated Honeycomb rendering: click, hover, and focus overlay changes route
to the root model. It preserves the local overlay fallback when no root model
is supplied. The renderer-local snapshot hierarchy still exists as a view
builder and is recreated per render, so it is not yet the final render-only
adapter.

An internal usage search finds the Scene Builder as the only current caller of
`HoneycombWorkflowCanvasInspiration`, and it supplies a root model rather than
a legacy scene. However, the exported Honeycomb props still publicly accept a
legacy `scene` for standalone callers. Removing that fallback would alter the
standalone contract and requires an explicit decision.

On September 14, the user requested reconciliation of the root assignment and handoff. They now point to this active objective; the previous storage-provider handoff is preserved as historical context. This documentation update does not authorize the remaining public fallback removal or change implementation status.
