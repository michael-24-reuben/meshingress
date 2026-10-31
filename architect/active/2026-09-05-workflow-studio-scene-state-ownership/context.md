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

## 2026-09-26 temporary Studio Workbench

`?view=workbench-tmp` is an intentionally isolated experiment. Its dedicated
page reuses `WorkflowStudioPage` for the live nodes, edges, run transport,
validation, panels, and selected node, but replaces only the center
`Workbench` surface with `StudioWorkbenchTmp`. The original `Studio Workbench`
route continues to render the rectangular editor unchanged.

The temporary surface creates a stable `WorkflowModelSnapshot` for the current
topology, applies each live `NodeRunState` as a Honeycomb node-execution phase,
and subscribes to model changes for renderer refresh. Node selection also flows
back to the Studio inspector. Topology edits, tool insertion, edge manipulation,
and layout changes deliberately remain in the production Workbench.

The Honeycomb optional `scene` prop is still exported and supported. This slice
adds a React wrapper while retaining the snapshot-producing compatibility entry
point; it does not remove or reinterpret the legacy standalone fallback.

## 2026-09-26 Honeycomb state presentation

The generic Honeycomb renderer now carries a presentation-only stylesheet with
selectors for every declared canvas-interaction, workflow, slot, node, and path
state. It consumes the existing `data-*` attributes from the root model or the
legacy scene fallback and does not mutate state, change input affordances, or
claim that an unimplemented state can now occur.

The temporary Workbench additionally derives only three workflow lifecycle
possibilities from existing inputs (`executing`, terminal `completed`, and
terminal `failed`, otherwise `incomplete`) and node `connected`/`isolated`
from its live edges. Path flow and diagnostic connection states are not
inferred, because the current runtime does not provide an unambiguous source.

## 2026-10-02 Scene Builder pan viewport

The Canvas Scene Builder now owns a Scene-Builder-specific pan viewport around
the Honeycomb canvas. Pointer capture begins only from non-control canvas
space, activates after a four-pixel movement threshold, and translates the
rendered content locally. The viewport suppresses wheel/native scrolling;
node buttons and form controls remain outside the pan gesture so their existing
model interactions continue unchanged.

The root canvas model reports `StateCategory.ofPanning` only while a drag is
active and restores the prior canvas-interaction phase afterward. The offset is
ephemeral React UI geometry, not a scene, topology, placement, or fallback
contract change. This does not authorize the later path-state work or any
standalone `scene` fallback removal.

## 2026-10-02 Scene Builder bounded layout

The Scene Builder's redundant local header and multi-card state catalog were
removed. The app-level navigation remains unchanged. A footer now keeps the
state-group and declared-state totals at the right edge without competing with
the canvas.

The canvas is the first grid column and fills all remaining width and height of
the app shell. Controls are in a right-side panel with a focusable vertical
separator: pointer dragging and Left/Right keyboard keys resize it within a
248px-to-560px range. The page, grid, canvas, and pan viewport all use
min-height: 0 and hidden overflow, while the controls themselves scroll when
needed. This prevents the Honeycomb canvas from forcing a fixed 610px minimum
outside its available bounds.

## 2026-10-02 path projection in Studio Workbench Tmp

`honeycombPathStatesForRun` derives path state from the temporary Workbench's
existing nodes, edges, capabilities, runStates, and running flag.
`syncHoneycombPathStates` applies both path categories through the root model
after each runtime update. Studio edges compile to default-output routes;
the runtime's node-start/completion evidence supports the conservative flow
projection below. This is not port-level telemetry or a validation transport.

| Category | State | Current automatic trigger / remaining work |
|---|---|---|
| Lifecycle | unconnected | An edge references a missing source or target node. |
| Lifecycle | connected | Both endpoints exist, permit the direction, and the edge is acyclic and unblocked. |
| Lifecycle | invalid | Output/input capability disallows the edge, or it is a self-link/cycle member. |
| Lifecycle | blocked | Source failed, or an ancestor failed while this source has not executed. |
| Lifecycle | drafting | Deferred: the temporary canvas has no path-creation gesture. |
| Lifecycle | pending-validation | Deferred: there is no asynchronous path-validation event. |
| Flow | idle | No demonstrated delivery; includes rerun reset, source running, and structurally invalid/dangling paths. |
| Flow | active | Workflow running, source succeeded, and target running. |
| Flow | success | Both source and target succeeded. |
| Flow | error | Source or target reports error on a structurally valid edge. |
| Flow | warning | Deferred: NodeRunState has no warning outcome. |
| Flow | muted | Deferred: no edge-mute or skipped-route signal exists. |

Structural validity takes precedence over execution. Ancestor errors affect
only waiting sources and do not overwrite a running/successful source reached
through another branch. A blocked descendant retains idle flow unless one of
its own endpoints has an error. New-run idle states clear previous blocked,
success, active, and error projections.

Route SVG paths and immediate-route seam markers share path IDs, attributes,
and presentation. Missing endpoints have no drawable geometry in the current
Honeycomb renderer: their unconnected state exists in the root model, but no
free-ended preview is fabricated. Geometry routing failure is also not treated
as a runtime path failure. Scene Builder still permits manual state inspection;
it does not automatically overwrite user-chosen path states. The production
Workbench and exported standalone scene fallback remain unchanged.
