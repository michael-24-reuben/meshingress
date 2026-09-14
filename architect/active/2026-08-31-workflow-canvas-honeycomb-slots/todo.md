# Todo

## Foundation

- [x] Capture the initial slot, node, path, drag, and canvas condition list.
- [x] Record `acceptsInput` and `acceptsOutput` as independent node capabilities.
- [x] Establish state layering so interaction/presentation overlays do not
  compete with execution/occupancy state.
- [x] Document the model and JSON examples in `docs/workflow-canvas-state-model.md`.
- [x] Record the CSS-versus-application-state boundary for responsiveness.
- [x] Create the separate TypeScript/React inspiration prototype with a uniquely
  scoped stylesheet and query-selectable test surface.
- [x] Establish CSS custom properties as the prototype's geometry authority;
  TypeScript reads their resolved values for grid, path, and SVG placement.

## Slot-state objectives

- [x] `empty` — render a muted, focusable flat-topped slot boundary with no
  node content.
- [x] `occupied` — attach a hexagonal node presentation to a discrete slot.
- [x] Slot/node hex edge — render a passive 1px outer frame, a regular inner
  slot calculated from the frame side and slot inset, and a separate 4px node
  perimeter with a 4px node inset inside that slot.
- [ ] Frame geometry visual QA — confirm the outer frames remain visibly
  separated, the interactive slots are centered within them, and neither
  overlay reacts on behalf of the passive frame.
- [x] Hex content containment — keep slot and node visual content inside their
  respective hexagons without clipping the SVG perimeter itself.
- [ ] `reserved` — retain a visible ghost/origin while its node is dragged.
- [ ] `valid-drop-target` — clearly identify an empty compatible target slot.
- [ ] `invalid-drop-target` — clearly reject an occupied, blocked, or
  incompatible hovered slot without hiding its occupancy.
- [ ] `blocked` — render an unavailable slot that cannot receive a node.
- [ ] `out-of-bounds` — reject placement outside the editable honeycomb region.
- [x] `hovered` — give an element-local CSS hover affordance without React
  pointer-state churn.

## Node-state objectives

- [x] Base node — render as an inset hexagon with an unframed 38px image and no
  premature metadata treatment.
- [x] Node identity — show the workflow node ID at the top-right end of its
  flat edge without competing with the main icon or title.
- [ ] `acceptsInput` — expose only valid inbound attachment affordances.
- [ ] `acceptsOutput` — expose only valid outbound attachment affordances.
- [x] `idle` state transport — initialize and expose the neutral node execution
  state through the normalized scene snapshot and
  `data-node-execution-state="idle"` on the Honeycomb node.
- [ ] `idle` visual treatment — establish the neutral, ready-to-run node
  treatment.
- [ ] `running` — make current execution obvious without obscuring topology.
- [ ] `success` — retain the last successful result as a secondary state.
- [ ] `warning` — distinguish a non-fatal concern from success and failure.
- [ ] `error` / `unavailable` — give failures and unavailable tools priority over
  passive overlays.
- [ ] `disabled` — make deliberate exclusion from execution legible.
- [x] `selected` — show the current local selection without changing workflow
  data.
- [x] `hovered` / `focused` — provide CSS-only pointer and keyboard affordance.
- [ ] `bookmarked` — indicate a saved tool without competing with run state.
- [ ] `has-unsaved-changes` — distinguish edited presentation/configuration from
  persisted state.
- [x] `isolated` state transport — initialize and expose the baseline node
  connection state through the normalized scene snapshot and
  `data-node-connection-state="isolated"` on the Honeycomb node.
- [ ] `unreachable`, `missing-required-input`, `blocked-by-upstream-error`, and
  `stale-output` — surface structural and runtime diagnostics without turning
  them into a single generic error. `isolated` visual grammar is also deferred.

## Drag-state objectives

- [ ] `dragging` / `lifted-from-origin` — make the grabbed node feel detached
  while retaining a reserved origin.
- [ ] Hover an empty compatible slot — transition only the current target to
  `valid-drop-target`.
- [ ] Hover an occupied/incompatible slot — transition only the current target
  to `invalid-drop-target`.
- [ ] Successful drop — atomically empty the origin, occupy the target, and
  clear drag overlays.
- [ ] Cancel/rejected drop — restore the origin to occupied without changing
  persisted placement.

## Path-state objectives

- [x] `connected` — render the sample workflow's existing edge topology behind
  its nodes.
- [x] Initial stacked slot ports — generate six passive vertex markers per
  logical slot at nominal-frame coordinates fixed by `--hwp-frame-side`;
  preserve each three-way junction's co-located marker as a distinct port
  object while visual frame size may shrink with `--hwp-grid-gap`.
- [x] Port baseline visual QA — confirm the local preview shows six passive
  vertex markers per slot, then direct paths only when an explicit routing
  slice is authorized, with visually stable three-way stacks.
- [ ] Port visual grammar — inspect and refine marker size, contrast, layering,
  and the legibility of intentional stacking before adding port interaction or
  any new paths.
- [ ] Focused path presentation — at close-up zoom, fan co-located port
  endpoints and use curved directional path segments without changing their
  underlying stacked port coordinates or compact-view topology.
- [x] Direct prototype path baseline — render sample workflow edges as visible,
  plain solid nearest-vertex-port segments beneath slot and node content.
- [ ] `unconnected` — expose compatible unattached ports without visual noise.
- [ ] `drafting` / `reconnecting` — render a responsive preview path while a
  connection is being created or moved.
- [ ] `pending-validation` / `invalid` — show compatibility status before a
  path is committed.
- [x] `idle` state transport — initialize and expose the baseline path-flow
  state through the normalized scene snapshot and `data-path-flow-state="idle"`
  on each Honeycomb route.
- [ ] `active`, `success`, `warning`, `error`, `blocked`, and `muted` — make
  execution flow legible while preserving topology.
- [ ] `selected` / `hovered` — make a specific path inspectable independent of
  its flow status.

## Canvas-state objectives

- [x] `idle` state transport — initialize and expose the baseline canvas state
  through the normalized scene snapshot and
  `data-canvas-interaction="idle"` on the Honeycomb canvas.
- [x] `selecting` — select a prototype slot in the controllable environment.
- [ ] `panning` and `zooming` — navigate a larger honeycomb without interfering
  with node/slot interactions.
- [ ] `drawing-path` and `reconnecting-path` — keep connection gestures distinct
  from canvas navigation and node dragging.
- [ ] `layout-revealing` — reveal a selected node when it is outside the
  current viewport.
- [ ] Define prototype interaction acceptance checks, including keyboard and
  reduced-motion behavior.

## Integration boundary

- [ ] Request explicit approval before any production canvas migration.
