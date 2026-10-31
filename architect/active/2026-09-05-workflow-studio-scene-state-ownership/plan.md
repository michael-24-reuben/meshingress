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
8. Keep the production Workbench intact while exposing an opt-in `Studio
   Workbench Tmp` route that uses Honeycomb to inspect the live Studio runtime.
   Preserve the exported optional `scene` fallback; this is not the final
   render-only migration.
9. Add attribute-only Honeycomb presentation for every already-declared state.
   Derive only execution lifecycle and topology connection states that are
   unambiguous from the temporary Workbench's existing runtime inputs.
10. Wrap the Scene Builder's Honeycomb canvas in a pan-only viewport. Suppress
    wheel/native scrolling, preserve node-control clicks, and treat the
    viewport translation as ephemeral UI geometry rather than workflow state.
11. Make the Scene Builder viewport a bounded full-height layout: remove its
    redundant header/catalog panels, use a right-aligned footer for state
    counts, move the controls to a resizable right sidebar, and let the canvas
    consume the remaining center area.
12. Project path lifecycle and flow in the temporary Workbench from existing
    topology, endpoint capabilities, and live execution. Check dangling ends,
    self-links/cycles, and upstream failures; reset projection on a new run.
    Carry path attributes onto adjacent-route controls as well as SVG routes.
    Keep drafting/pending-validation and warning/muted deferred until their
    editing/runtime triggers exist. Verify transitions and branch isolation.
# 2026-10-31: Scene Builder editing and live runtime integration

The owner requested a Canvas Scene Builder that supports workflow editing and
live execution. Preserve the existing state-authoring surface, the production
Workbench, public state methods, and the standalone renderer fallback.

1. Inventory the Studio editing, validation, execution, and workflow storage
   contracts and establish how a Scene Builder session shares their state.
2. Design and implement durable Honeycomb placement and topology editing,
   including insertion, movement, deletion, connections, validation, and
   persistence or an explicitly defined workflow serialization boundary.
3. Connect the live Studio run and validation paths, selected-node inspector,
   and execution projections to that same edited workflow.
4. Verify editing, persistence, runtime transitions, and compatibility with
   focused tests, the frontend build, and browser checks. Do not claim runtime
   signals that the backend does not expose.

The existing temporary Workbench is a useful integration reference, but its
35-slot derived placements are not a durable placement model. The existing
Scene Builder currently owns static fixture data rather than Studio's live
workflow state. Both boundaries must be addressed before calling this complete.
