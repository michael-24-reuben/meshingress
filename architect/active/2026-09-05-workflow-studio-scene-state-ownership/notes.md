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

## 2026-09-26 — Studio Workbench Tmp

Added an opt-in `?view=workbench-tmp` page alongside the production Studio
Workbench and Canvas Scene Builder. It shares the `WorkflowStudioPage` runtime
but conditionally replaces only the central rectangular Workbench with a
read-only Honeycomb surface. Run and Validate call the existing runtime
handlers; live idle/running/success/error values are mapped into the root
Honeycomb model's `nodeExecutionState` phase, and node clicks update the shared
Studio selection.

The temporary surface has its own deterministic placement bridge and scoped
execution-state visual treatment. The original Workbench, topology editing,
and its `scene` compatibility fallback remain intact. `HoneycombWorkflowCanvas`
is now a React rendering wrapper, while `HoneycombWorkflowCanvasInspiration`
remains the snapshot-producing compatibility entry point for existing callers.

`npm test` passed with 9 tests. `npm run build` passed after sandbox elevation
was required for Vite's helper process; it retained only the existing
chunk-size advisory. `npm run lint` completed with existing Fast Refresh and
state-class warnings only; no lint errors or whitespace errors were reported.

## 2026-09-26 — Honeycomb state presentation

Added `honeycomb-state-presentation.css`, imported by the shared Honeycomb
renderer. It has an attribute selector for every declared state in all nine
state categories, so the Scene Builder and temporary Workbench visibly expose
the state already carried by their model attributes. This is presentation only:
it does not add new event paths, change selection semantics, or manufacture
runtime diagnostics.

The temporary Workbench now additionally projects workflow `incomplete`,
`executing`, `completed`, or `failed` from its existing run status and projects
node `connected` or `isolated` from its existing edges. It intentionally does
not derive path flow, missing-input, blocked-upstream, stale-output, or other
diagnostic states without authoritative runtime data.

Source inspection confirms the stylesheet selectors cover every declared state.
The earlier raw-stylesheet test claim was corrected: the app TypeScript setup
does not include Node types and Vitest stubs the stylesheet import, so no
dedicated selector-source test is retained. The existing model and temporary
Workbench tests remain the behavioral coverage for state projection.

## 2026-10-02 — Scene Builder pan viewport

The Scene Builder's Honeycomb canvas is wrapped by a pan-only viewport. A
primary-pointer drag from non-control canvas space captures the pointer, begins
after a four-pixel threshold, and translates the content by the pointer delta.
The viewport prevents wheel scrolling and overrides the embedded Honeycomb
scroll container only within Scene Builder. Slot-frame buttons and form
controls are deliberately excluded from the pan start so existing selection and
control behavior is retained.

The root canvas phase changes to `StateCategory.ofPanning` while the gesture is
active and returns to the prior interaction phase on pointer release or
cancellation; the translation itself remains React-local. `SceneBuilderPan.test.ts`
covers the threshold and delta helper. `npm test` passed with 11 tests, `npm run
build` passed with only the existing Vite chunk-size advisory, and `npm run lint`
completed with existing warnings only. A local browser check confirmed a drag
pans the Scene Builder canvas and a wheel action over it does not scroll it.

## 2026-10-02 — Scene Builder bounded layout

The local Scene Builder header and state catalog were replaced by a compact
right-aligned footer that reports 9 state groups and 59 declared states. The
canvas now occupies the complete bounded center column, while the controls
panel is on the right.

The separator is exposed as a focusable ARIA separator. Dragging it expanded
the controls from 320px to 402px in the local browser; ArrowRight then reduced
it to 386px, demonstrating keyboard resize behavior. The canvas remains
contained above the footer after either resize. The resize helper's bounds are
covered by the focused test. npm test passed with 12 tests; npm run build
passed after sandbox elevation was required for Vite's Windows helper process,
with only the existing chunk-size advisory. npm run lint completed with its
existing warnings only.

## 2026-10-02 — Path lifecycle and flow projection

Implemented the authorized safe path slice in Studio Workbench Tmp. Structural
checks cover missing endpoints, input/output capabilities, self-links, and
cycle membership. Flow uses endpoint execution evidence; running a source
alone does not mark its output path active. Ancestor-error traversal blocks
waiting descendants while preserving unrelated branches and already-executed
sources. Both path categories are refreshed through root mutations on each
runtime update, including a new-run reset.

Adjacent route controls previously lacked model path attributes. They now
carry optional path identity and read root/legacy attributes alongside SVG
paths. State styling reaches their polygon and arrow stroke through the existing
route-control color variable. The optional identity preserves manual seam
overrides and the standalone fallback contract.

Verification: npm test passed with 16 tests; npm run build passed after the
known sandbox spawn EPERM required elevation, with only the existing chunk-size
advisory; npm run lint completed with existing warnings only. Tests cover
endpoint/capability/cycle checks, conservative delivery, transitive blocking,
branch isolation, root subscriptions, and success/failure/rerun transitions.
Browser inspection of the Scene Builder confirmed success on both the SVG path
and its adjacent marker (rgb(98, 216, 164)), and blocked/idle on both with
rgb(255, 159, 115). Studio Workbench Tmp initialized both sample paths to
connected/idle. No real workflow was launched during UI verification.

Eight of twelve path values now have automatic triggers for the available
temporary-Workbench data. Drafting, pending-validation, warning, and muted
remain presentation/manual-inspection states pending editing/runtime signals.
Missing-endpoint paths have root state but no drawable geometry. The overall
scene-ownership entry remains active; no commit was created.
