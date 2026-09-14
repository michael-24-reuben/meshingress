# Todo

## Completed ownership work

- [x] Record the scene tracker feature inventory and retirement gate.
- [x] Add parse-only scene validation and normalization to `WorkflowStudioState`
  without changing existing scene-tracker callers.
- [x] Define public, read-only attribute projection on component snapshots.
- [x] Add a path snapshot model and stable path identity map.
- [x] Add root maps for canvas data, workflow state, slots, and nodes.
- [x] Move cross-node selected-overlay exclusivity into the root aggregate.
- [x] Add required independent input/output capabilities to `WorkflowNode` and
  validate them at the node data boundary.
- [x] Project node capabilities from snapshot/state ownership into renderer
  attributes. Capability changes remain node-data updates rather than phase
  mutations, so the Scene Builder requests its existing explicit rerender.
- [x] Add aggregate subscriptions and typed mutations to the root model.
- [x] Replace the Scene Builder tracker use with the root model and retain
  Honeycomb's legacy scene fallback for compatibility.

## Next objectives

- [x] Consolidate the three exact duplications in
  `workspace-inspection-2026-09-07-113523` without deleting state methods.
- [x] Add model contract tests for all mutation categories, target payloads,
  and attribute projections.
- [x] Add `parseWorkflowStudioScene` as the preferred contract's parse-only
  normalizer, with parity coverage against the retained legacy parser and no
  caller migration.
- [x] Review the Honeycomb-backed stable-identity/lifecycle design and record
  the renderer-local snapshot conflict.
- [x] Route integrated Honeycomb node-overlay interactions through a stable
  root-model adapter while preserving the model-less standalone fallback.
- [ ] Replace the renderer-local Honeycomb snapshot hierarchy with a fully
  render-only adapter after its public standalone contract is decided.

## Legacy scene-file retirement gate

- [x] Retire `WorkflowCanvasSceneEventTracker` and its adapter boundary.
- [ ] Migrate the remaining legacy scene fallback type consumers.
- [ ] Produce feature-parity evidence for parser/creator compatibility and
  renderer fallback behavior.
- [ ] Retire `workflow-canvas-scene.ts` only after the approved migration and
  proportionate verification are complete.
