# Plan

1. Preserve `WorkflowStudioState.ts` as the preferred state contract; do not
   delete its methods during consolidation.
2. Consolidate exact duplicate attribute naming, demo placement data, and
   factory initialization traversals without changing state behavior.
3. Expand contract coverage for each mutation category, target payload, and
   projected renderer attribute.
4. Establish parse-only state-side scene normalization with behavioral parity
   tests against the retained legacy parser. Do not migrate legacy callers.
5. Review the Honeycomb identity/lifecycle boundary. Keep
   `WorkflowModelSnapshot` as the durable phase identity; require an approved
   render-only adapter before replacing renderer-local snapshot subclasses.
6. Migrate remaining legacy scene fallback type consumers only after the model
   boundary independently owns their needed behavior.
7. Retire `workflow-canvas-scene.ts` only after explicit approval and a final
   compatibility comparison.
