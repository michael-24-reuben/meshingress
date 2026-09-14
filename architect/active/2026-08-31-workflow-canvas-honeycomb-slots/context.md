# Context

## Existing Canvas

- Production implementation: `app/meshingress-studio-web/src/features/workflow-studio/components/WorkflowCanvas.tsx`.
- The current canvas is free-positioned and grid-snapped, using rectangular
  nodes; it is not slot-owned.
- Current node execution vocabulary is `idle`, `running`, `success`, and
  `error`. It also has selection, favoriting, path reconnecting, and a
  connection-target presentation.

## Agreed Direction

- The proposed canvas uses a honeycomb/hexagon slot model.
- A slot owns placement; a node occupies a slot; a path represents compatible
  flow between nodes.
- A node may accept input, output, both, or neither. Capability must not be
  inferred from node kind or current connections.
- Each logical slot owns six dedicated `SlotPort`s, one per nominal hex vertex
  (the three-way frame junction), at coordinates based on `--hwp-frame-side`.
  They are unaffected when `--hwp-grid-gap` shrinks the rendered frame and
  opens a void around it. Neighboring slots' vertex ports may have identical
  coordinates but retain distinct `slotId` + corner identities; do not merge
  them into shared anchors.
  A port carries input/output/bidirectional direction, availability, capacity,
  and compatibility. Close-up rendering may fan the stacked endpoints and use
  curved path tangents while the logical port coordinates remain unchanged.
- The inspiration prototype now renders those six ports as passive markers and
  intentionally renders no workflow paths. It uses nominal frame measurements
  for axial placement and port coordinates; `--hwp-grid-gap` instead shrinks
  only visual frame/slot/node geometry.
- Short-lived local states such as hover should prefer native CSS
  pseudo-classes. Cross-element interaction semantics (for example the
  reserved drag origin and target-slot validity) need application state and
  should be exposed through minimal data attributes/classes.

## Reference Artifact

- `docs/workflow-canvas-state-model.md` defines the current proposed state
  vocabulary, valid JSON examples, drag transitions, and visual conflict
  priority. It is explicitly a design reference, not a persisted/API contract.

## Worktree Boundary

The repository contains extensive unrelated active storage-provider work.
Preserve it. This entry is deliberately a separate active design record and
does not replace the storage-provider objective in `architect/ASSIGNMENT.md`.
