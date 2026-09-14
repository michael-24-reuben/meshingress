# Notes

## 2026-09-05 — Initial assessment

The scene tracker cannot yet be retired. The current renderer receives its
state attributes from the scene snapshot, and local snapshot phase mutations
are not an independent renderer/state pipeline.

The first safe increment is deliberately parse-only. `parseWorkflowStudioScene`
adds input normalization and validation to the state module without creating a
second live store or changing any tracker call site.

`npm run build` completed successfully in
`app/meshingress-studio-web`. Vite emitted its existing bundle-size advisory.

## 2026-09-05 — Workflow node capability contract

`WorkflowNodeCapabilities` now requires `acceptsInput` and `acceptsOutput`.
The initial manual trigger is output-only; the current tool fixtures and
tool-node creator allow both directions. This is a data-contract step only:
the scene tracker still owns capability attribute projection, and the values
are not yet derived from tool metadata or port compatibility.

`npm run build` completed successfully after the addition. Vite again emitted
only its existing bundle-size advisory.

## 2026-09-07 — Tracker retirement and focused consolidation

The tracker and bridge adapter are retired from the live Studio flow. The
Scene Builder now has a stable `WorkflowModelSnapshot` reference, uses its
root subscription for phase changes, and sends the model directly to Honeycomb.
Honeycomb still accepts an optional legacy scene for standalone compatibility.

`workspace-inspection-2026-09-07-113523` is linked to this entry. It scopes
the cleanup to three exact behavior-preserving duplications: attribute-name
derivation, demo placements, and duplicate factory placement traversal. The
inspection deliberately excludes unused-code removal and preserves every
`WorkflowStudioState` method.

The record's older parse-only claim is stale: a source search finds no
`parseWorkflowStudioScene` export in `WorkflowStudioState.ts`. Keep parser
work as a decision-gated parity objective rather than silently recreating it.

## 2026-09-07 — Consolidation outcome

The three report findings are complete. Attribute-name derivation now has one
state-owned implementation; the Scene Builder and standalone Honeycomb view
share one prototype placement list; and the model factory sets slot occupancy
during its original placement construction pass. No `WorkflowStudioState`
method was deleted.

`npm test` passed with 2 tests. `npm run build` passed; Vite retained only its
existing JavaScript chunk-size advisory.

## 2026-09-07 — Model contract coverage

The state-model test now exercises all nine phase mutation categories. For
each mutation it verifies the emitted active event's target payload and the
corresponding projected `data-*` renderer attribute. The fixture-default
test also verifies node capability attributes.

Single-value snapshots currently emit both the requested active phase event
and a base/previous-phase deactivation event. The test deliberately locates
the requested active event rather than assuming event ordering; no state
behavior was changed.

`npm test` passed with 3 tests. `npm run build` also passed; Vite retained
only its existing JavaScript chunk-size advisory. The next architect stage
remains the explicit parser-contract decision; this test work does not create
or restore `parseWorkflowStudioScene`.

## 2026-09-07 — Parse-only state contract

`parseWorkflowStudioScene(input)` is now the preferred state's independent,
parse-only normalizer. It validates the same scene shape, target/category
assignments, and enum values as the retained legacy parser; it also clones the
input and recomputes normalized state attributes, including composable node
overlays. It does not create scene defaults, dispatch mutations, or migrate a
legacy caller.

The parser parity tests compare normalized valid output and confirm both
parsers reject a category assigned to the wrong target. `npm test` passed with
5 tests, and `npm run build` passed with only Vite's existing chunk-size
advisory.

## 2026-09-07 — Honeycomb identity/lifecycle review

The Scene Builder has the correct durable state owner: one
`WorkflowModelSnapshot` is retained in `useRef`, and its root subscription
requests a React refresh. The Honeycomb renderer is not yet an identity-safe
consumer. Calling `HoneycombWorkflowCanvasInspiration(...)` during each page
render constructs a new canvas snapshot and, through its constructors, new slot
and node snapshot subclasses. Their interaction handlers call local
`setOverlay*` methods, so the mutation does not enter the root model's
subscription stream or its cross-node selection rule.

No renderer code was changed. The approved direction required before a
replacement is a render-only Honeycomb adapter: the root model remains the sole
phase owner; renderer elements resolve root snapshots by ID and send mutations
back through the root public API; geometry and tooltip/hover positioning remain
React-local. The older tracker-based `WorkflowCanvasComponent` lifecycle sketch
cannot be implemented unchanged because its constructor and update contract
depend on the retired tracker.

## 2026-09-07 — Honeycomb root-interaction adapter

Added `HoneycombWorkflowModelAdapter` under the Honeycomb adapter namespace.
When the integrated renderer has a `WorkflowModelSnapshot`, node click, hover,
focus, and blur now send overlay mutations through that root model. The existing
model-less renderer path retains its local overlay behavior.

This is an interaction-routing increment, not a claim that Honeycomb is fully
render-only. `HoneycombCanvasModelSnapshot`, slot snapshots, and node snapshots
are still rebuilt by the current renderer path. The static internal usage search
finds no caller that passes its optional legacy `scene`, but removing that
exported standalone fallback is a compatibility decision and was not made.

`npm test` passed with 6 tests, including root-model adapter identity coverage.
`npm run build` passed with only Vite's existing chunk-size advisory.
