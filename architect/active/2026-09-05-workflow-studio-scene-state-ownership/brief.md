# Workflow Studio Scene State Ownership

Prove that `WorkflowStudioState` can independently own workflow-canvas state
before retiring `states/workflow-canvas-scene.ts`. Until that proof exists, the
scene tracker remains in use and must retain its current behavior.

The intended end state is not a hidden migration. Snapshot classes own their
local renderer attributes, while a clearly named root Studio state aggregate
owns scene identity, cross-element rules, validation, and subscriptions. The
legacy scene tracker may then remain as an optional adapter or supplier rather
than the mandatory authoritative store.

## Non-goals

- Deleting or renaming `workflow-canvas-scene.ts` in this entry's first slice.
- Replacing current scene-tracker callers before feature parity is demonstrated.
- Accessing snapshot private fields as an alternate state contract.
- Changing production WorkflowCanvas persistence or integration boundaries.
