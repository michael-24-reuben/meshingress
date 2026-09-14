# Notes

## 2026-08-31 — Initial state-model decisions

- Model mutually exclusive concerns in distinct fields: slot occupancy, slot
  drop state, node execution state, node connection state, path lifecycle, and
  path flow state.
- Model selection, hover, focus, bookmark, drag presentation, and unsaved
  changes as additive overlays.
- During a node drag, retain the origin as `reserved` rather than making it
  empty. On a successful drop the origin becomes empty and the target becomes
  occupied; cancel/reject restores the origin to occupied.
- An occupied slot under the dragged node is not ambiguously "occupied empty":
  it is `occupancy: occupied` plus `dropState: invalid-drop-target`.
- Use CSS for element-local, ephemeral presentation (`:hover`,
  `:focus-visible`, `:active`, transitions, and custom-property tokens). Use
  TypeScript state only for semantic conditions other elements must observe,
  especially drag origin/target and connection validation.
- During pointer movement, update semantic state only when the targeted hex or
  target validity changes; CSS should perform visual interpolation.

## 2026-08-31 — Integration seam

- The future production switch occurs at the `WorkflowCanvas` element in
  `app/meshingress-studio-web/src/features/workflow-studio/components/elements/Workbench.tsx`.
- `Workbench` already owns the canonical workflow nodes, edges, run states,
  selection, and mutation callbacks. The honeycomb canvas must consume the
  same prop contract rather than create a parallel workflow store or callback
  path.
- No change to `Workbench.tsx` has been made in this design slice. The element
  replacement is deferred until a prop-compatible prototype is ready and the
  user authorizes integration.

## 2026-08-31 — First controllable prototype

- Added `canvas/honeycomb-prototype/` under the Studio feature. The geometry is
  flat-topped: `height = 100px`, `side = 100 / sqrt(3)`, and point-to-point
  width is approximately `115.47px`.
- The prototype consumes `initialNodes` and `initialEdges` from the real sample
  workflow while a local placement bridge maps the three sample nodes to axial
  hex slots. It does not modify their production `x`/`y` values or persistence.
- Styles are isolated to the `hwp-` prefix and imported only by the prototype
  canvas. Empty slots retain a muted visible boundary; occupied nodes use a
  4px-radius card and an unframed 45px image, capped below the floored 57px
  hex-side width.
- The controllable preview is selected with `?view=honeycomb` in the Studio web
  app. `Workbench.tsx` and the production canvas were not changed.
- `npm run build` passed (`tsc -b` and `vite build`). Vite reported its existing
  advisory that the main minified bundle exceeds 500 kB.
- The local `?view=honeycomb` preview visually rendered the flat-topped lattice,
  muted slot boundaries, unframed node images, and sample flow arrangement.
  Selecting the Manual Trigger slot applied the selected state; no browser
  console errors were reported.

## 2026-08-31 — Node shape correction

- The node is now an inset flat-topped hexagon inside its slot, rather than a
  rounded rectangular card. Its unframed image is 38px (still below the 57px
  floored hex-side cap). The outer slot remains the muted 100px hex boundary.
- `npm run build` passed after the correction. Vite repeated only its existing
  advisory about the minified bundle exceeding 500 kB.

## 2026-08-31 — Hex edge weight

- Both the outer slot and the inset node now use a 4px hex perimeter. The node
  begins after a separate 4px all-round gap, rather than consuming the slot
  boundary. The refreshed local preview rendered both outlines with no browser
  console errors; `npm run build` passed.

## 2026-09-01 — SVG slot boundary

- `.hwp-slot-boundary` is now an SVG with a flat-top hex `polygon`, replacing
  the clipped CSS background/pseudo-element pair. The polygon has the same
  4px stroke and is the single boundary primitive for all 35 prototype slots.
- Hover and selected styling remain CSS-driven by targeting the SVG polygon.
  The local preview rendered all 35 SVG boundaries with no browser console
  errors, and `npm run build` passed.

## 2026-09-01 — Hexagonal content clipping

- Slot content is now contained by a hexagonal clipping layer separate from its
  SVG boundary, so a future node, indicator, or drag presentation cannot spill
  through the flat top or pointed sides.
- The node remains an `article`, with an SVG polygon as its first child for the
  visible 4px outline. Its image and title live in an inner hexagon that clips
  overflow, keeping visual structure and content containment independent.

## 2026-09-01 — Node outline and centering correction (superseded)

- The node now occupies exactly the slot's 4px all-round inset. The prior
  additional 4px edge-width offset made the node appear top-padded and too
  small within its slot.
- The clipped node fill now occupies the same hexagonal bounds as the SVG
  outline, beneath it. This keeps overflow contained while presenting one
  visible 4px perimeter rather than an inner and outer boundary.

## 2026-09-01 — Regular concentric node geometry

- The node's live 18px face inset is now represented as geometry, not a
  uniform CSS box inset. Its height is `64px` and its width is
  `64 * 2 / sqrt(3)` (about `73.90px`), preserving the flat-top hex aspect
  ratio and centering it in the slot.
- `HONEYCOMB_GRID_GAP` is distinct from `HONEYCOMB_NODE_INSET`, preventing the
  axial layout spacing from being mistaken for internal node geometry.
- The prototype uses phrasing-only `span` markup within each slot button;
  the SVG remains the node's first child and node content is vertically
  centered within its clipped hexagon.
- Browser measurement confirmed a node aspect ratio of `1.154698`, within
  rounding of the regular-hex target `2 / sqrt(3) = 1.154701`, with an 18px
  vertical and approximately 20.78px horizontal clearance. `npm run build`
  and `npm run lint` both passed; lint retained three existing fast-refresh
  warnings outside this prototype.

## 2026-09-01 — CSS-owned honeycomb geometry

- `--hwp-slot-side` is now the sole authored slot dimension. CSS derives slot
  height from `side * sqrt(3)` and width from `side * 2`, then derives the
  inset node dimensions from those results.
- `honeycomb-grid.ts` now contains only geometry types and axial calculation
  functions. The prototype reads resolved CSS pixel values after the canvas
  mounts and supplies them to grid positioning, path centers, and SVG view
  boxes, preventing TypeScript/CSS geometry drift.
- Browser custom-property reads retain `calc(...)` tokens, so the prototype
  resolves the CSS-owned dimensions through an invisible probe's computed
  width, height, margin, and padding instead of duplicating the formula in
  TypeScript.
- Local preview verification resolved the CSS chain to a `57.735px` side,
  `99.9955px` height, `115.459px` width, and `4px` grid gap. All 35 slots
  mounted with no browser console errors.

## 2026-09-01 — Node identity placement

- The prototype renders each real workflow node ID (`r-…`) at the top-right
  end of the node's flat top edge. The label is clipped with the node's slot,
  sits above the SVG perimeter/content layers, and is intentionally secondary
  to the tool image and title.
- Local preview verification rendered `r-001`, `r-002`, and `r-004` at the
  intended placement with no browser console errors.

## 2026-09-01 — Upper intermediary prototype node

- Added an unconnected, test-page-only `r-005` placeholder titled `New Node`.
  It occupies axial slot `q:1, r:0`, the upper hexagon between `r-001` and
  `r-002`. The shared sample workflow and production canvas remain unchanged.
- Local preview verification confirmed the expected four IDs and `r-005` in
  the requested upper slot, with no browser console errors.

## 2026-09-01 — Separate slot-gridline layer (design decision)

- Wrap each interactive `button.hwp-slot` in a same-sized, non-interactive
  hexagonal visual layer. That wrapper is responsible only for the slot
  gridline, not for selecting, dropping, or otherwise reacting to input.
- The gridline is an SVG polygon with sharp joins, a muted dashed stroke, and
  transparent fill. It uses `pointer-events: none` and sits independently of
  the slot button's event and accessibility behavior.
- The button remains the semantic and interactive slot surface. Its future
  state styling should not mutate the passive gridline; state-specific visual
  overlays will be separate layers.
- This is a design-only decision in the current discussion. No prototype
  markup or CSS had changed at the time of the decision.

## 2026-09-01 — Separate slot-gridline layer (implemented)

- `hwp-slot-frame` now owns positioning for each slot. It renders the passive
  `hwp-slot-gridline` SVG sibling before the interactive `button.hwp-slot`.
- The gridline polygon has transparent fill, a muted `6px 5px` dashed 4px
  stroke, `butt` caps, mitered joins, and `pointer-events: none`.
- Hover, keyboard focus, and selected feedback now use the button's clipped
  pseudo-element surface. The passive gridline therefore never changes in
  response to input.
- Local preview verification found 35 gridlines, computed transparent fills,
  dashed mitered strokes, and pointer-inert SVGs. Clicking Manual Trigger
  still applied `hwp-slot--selected`. `npm run build` passed.

## 2026-09-01 — Frame inset and perimeter correction

- The axial honeycomb is now logically gapless (`--hwp-grid-gap: 0px`). The
  former visual gap is represented as a `4px` inset of each slot frame rather
  than separation between the cells.
- The slot frame uses a dedicated `1px` dashed perimeter. Its regular-hex
  dimensions are derived from its inset height so it remains concentric with
  the outer logical slot.
- Nodes preserve a distinct `4px` perimeter and sit a further `4px` inward
  from the frame (`8px` from the logical slot), preventing node and frame
  outlines from coinciding.
- Local preview measured a `91.99px` frame height with a `1px` stroke and an
  `84px` node height with a `4px` stroke; slot selection and pointer-inert
  gridlines were also verified. `npm run build` passed.

## 2026-09-01 — Outer-frame experiment reverted

- The outer `hwp-slot-frame` / `hwp-slot-gridline` experiment, including its
  gapless axial geometry, 4px frame inset, and 1px perimeter, was reverted at
  the user's direction.
- The current prototype baseline again uses the original interactive
  `button.hwp-slot` with its contained `hwp-slot-boundary` SVG and the prior
  4px slot/node perimeter treatment.
- Preserve the earlier frame notes as rejected exploration only. Do not reuse
  their geometry or markup in the next frame design without a new explicit
  design decision. `npm run build` passed after the restoration.

## 2026-09-02 — Separated-frame hierarchy

- The outer `hwp-slot-frame` retains the standard frame geometry. Because
  axial placement still receives the existing `--hwp-grid-gap`, frames retain
  the visual separation of the original slot lattice rather than touching.
- The interactive inner slot is inset by `--hwp-slot-inset`; its side is
  calculated from the frame side by subtracting `2 / sqrt(3)` times that
  perpendicular inset. Its height and width then derive from its own side,
  preserving flat-top regular-hex geometry.
- The node dimensions now derive from the inner slot, preserving its separate
  4px inset and 4px perimeter. The outer frame is a transparent, passive,
  muted solid SVG with a 1px mitered perimeter.
- `npm run build` passed. A desktop preview connection timed out before a
  screenshot was returned, so visual QA remains explicitly pending.

## 2026-09-02 — Solid frame perimeter

- The passive `hwp-slot-frame-boundary` no longer defines a dash pattern. Its
  1px muted mitered SVG perimeter is now solid; all frame geometry and
  interaction boundaries remain unchanged.

## 2026-09-02 — Frame-root CSS geometry

- `--hwp-frame-side` is now the sole authored standard side length for the
  parent frame. CSS derives `--hwp-frame-width` and `--hwp-frame-height` from
  it; the geometry probe reads those dimensions for axial layout and SVGs.
- `--hwp-slot-side` is now calculated rather than authored, using the frame
  side and `--hwp-slot-inset`. Slot and node measurements continue to derive
  from that calculated child geometry. `npm run build` passed.

## 2026-09-02 — Session handoff

- The active entry now contains `handoff.md`, a self-contained continuation
  note for this honeycomb objective. It deliberately leaves the root Architect
  assignment and handoff files untouched because they are owned by the
  separate storage-provider objective.

## 2026-09-02 — Proposed connection-port vocabulary

- Superseded by the fixed lattice-anchor decision below. It incorrectly
  coupled the first proposal to the visible node perimeter and considered only
  side midpoints.
- Call the small visible edge points **connection ports** (or `ConnectionPort`
  in code). A port is the semantic contract for an attachable edge; its
  geometric coordinate is an **anchor**. This avoids overloading "anchor" with
  input/output, compatibility, and capacity semantics.
- Begin with one port at the midpoint of each of the six flat-topped hex sides,
  not at its vertices. Side-centre ports provide an unambiguous outward normal
  for a path's first and final segment and keep the six-way topology legible.
- Each port should eventually own a stable ID, `direction` (`input`, `output`,
  or `bidirectional`), enabled/visible state, capacity/occupancy, and a
  connection compatibility result. The node-level `acceptsInput` and
  `acceptsOutput` flags remain coarse capabilities; port state refines where a
  particular edge can attach.
- For a draft or automatic connection, choose the lowest-cost pair among
  compatible source and target ports. Start with Euclidean anchor distance;
  later add direction, obstacle, crowding, and crossing penalties rather than
  treating physical closeness as the only criterion.
- Ports can also drive drag-path previews, reconnection handles, endpoint
  snapping, fan-out/capacity feedback, disabled or blocked affordances,
  directional layout cues, accessibility hit targets, and stable edge
  attachment when a node's visual contents change.

## 2026-09-02 — Dedicated stacked slot ports

- Superseded for placement: ports belong at vertices rather than side midpoints.
- Each logical frame/slot owns exactly six **slot ports** (`SlotPort`), one at
  each side midpoint. Ports are frame-dedicated; there are no corner ports.
- Their positions are calculated from the nominal `--hwp-frame-side` geometry.
  A frame's rendered hexagon may shrink through `--hwp-grid-gap`, leaving a
  visual void, without moving its six port coordinates.
- When frames form the honeycomb, corresponding neighboring ports may occupy
  the same coordinate. This is deliberate **stacking**, not a shared port:
  each still has its own `slotId`, side, direction, state, and future edge
  history. Never de-duplicate them by coordinate.
- A focused/close-up view can reveal path data and visually separate stacked
  endpoints with small presentation offsets. It may use a curved departure or
  arrival tangent to signal the path's next direction; that curve is a zoomed
  presentation of the same logical endpoint, not a different route identity.

## 2026-09-02 — Initial stacked-port marker implementation

- The inspiration canvas no longer receives or renders sample workflow edges.
  This isolates the port visual grammar before routing is reintroduced.
- `flatTopHexPortSides` and `flatTopHexSidePortPoint` generate the six named
  side-midpoint positions for every logical slot. Each rendered marker has a
  slot-qualified `data-slot-port` identity, so equal coordinates do not imply a
  shared or de-duplicated port.
- Axial placement now uses nominal `--hwp-frame-side` geometry only. CSS derives
  a centered visual frame from that nominal size and `--hwp-grid-gap`; the inner
  slot and node derive from the visual frame. Consequently, the port ring stays
  fixed while the displayed hexagon can expose surrounding void.
- `npm run build` passed (`tsc -b` and Vite). The desktop browser binding timed
  out while opening the local preview, so marker appearance and stacking remain
  pending visual QA.

## 2026-09-02 — Initial stacked-port visual QA

- A fresh local `?view=honeycomb` preview confirmed that all 35 slots render
  six passive side-port markers and the former workflow path layer is absent.
- Adjacent slots' matching port markers visually co-locate without interaction
  conflict. The markers remain non-interactive while the slot buttons retain
  their existing accessibility and selection behavior.
- The current live `--hwp-grid-gap` value is `0.2px`, making the exposed void
  between the fixed port ring and shrinkable visual frame intentionally subtle.
  Marker size, contrast, and that gap remain refinement decisions; no paths or
  port interaction should be added before those are approved.

## 2026-09-02 — Vertex-port placement correction

- Each slot's six `SlotPort`s now occupy the nominal hex vertices: `top-left`,
  `top-right`, `right`, `bottom-right`, `bottom-left`, and `left`. These are the
  three-frame honeycomb junctions requested by the user, not side midpoints.
- The port geometry helper and data attributes use `corner` terminology so the
  source cannot confuse this model with the rejected side-midpoint placement.
- `npm run build` passed, and a fresh local preview visibly confirmed markers
  at the three-way corners with no workflow path layer.

## 2026-09-02 — Prototype port identities

- The isolated prototype now uses generated slot IDs such as `slot/q1-r0`,
  shared junction IDs such as `junction/u4-v1`, and concrete slot-port IDs
  such as `port/junction/u4-v1/slot/q1-r0`.
- Each passive port marker exposes its concrete port key through
  `data-slot-port` and its shared topology location through
  `data-junction-id`. This preserves stacked-port ownership while providing
  the child routing record a stable identity seam.
- No path rendering or routing interaction was restored. `npm run build`
  passed after the identity implementation.
- A generated 35-slot check found 210 unique concrete port IDs and confirmed a
  single canonical junction maps to three distinct slot owners. `npm run lint`
  passed with only the Studio's three existing fast-refresh warnings outside
  this prototype.

## 2026-09-02 — Direct visible prototype routes

- The user authorized route visibility. The prototype again receives the sample
  workflow edges and renders each as a stage-level SVG path below the slot and
  node layers.
- Every direct route evaluates all source/target vertex-port pairs and selects
  the shortest pair. The rendered route retains the concrete source and target
  port IDs as data attributes, then uses a clean solid cyan stroke that remains
  distinct from the muted gridline without adding decoration.
- This is deliberately not the future topology planner: no intermediate-slot
  traversal, port closure, compatibility, capacity, route interaction, or
  rerouting behavior has been introduced. Build, lint, and fresh local visual
  QA passed; lint retained only three existing warnings outside the prototype.
- The initial dashed-arrow styling was explicitly superseded. Routes now use
  only a 3px solid cyan line: high-contrast enough to separate flow from the
  muted grid, with no arrow, dash, or glow decoration.

## Superseded — Fixed lattice anchors and port bindings

- The requested fixed points are **lattice anchors** (`LatticeAnchor`), not
  ports tied to the rendered hexagon. They belong to the logical honeycomb cell
  and retain their coordinates even when a frame, slot, or node shrinks and a
  visual void opens between cells.
- Each logical cell exposes twelve anchors: six `edge` anchors at the original
  cell-edge midpoints and six `corner` anchors at the original cell vertices.
  They use stable keys such as `north-edge` and `north-east-corner`.
- A **connection port** (`ConnectionPort`) is the node-level semantic binding
  to a lattice anchor. It is where `input`/`output`/`bidirectional`, enabled,
  capacity, occupancy, and compatibility rules live. The marker can be
  rendered at the anchor, but the anchor persists for an empty or visually
  resized cell.
- This intentionally introduces a future geometry split: `lattice` dimensions
  define axial pitch and anchor positions; `frame`, `slot`, and `node`
  dimensions define only visual and interaction bounds. The current
  frame-root CSS is still the live prototype implementation and is not changed
  by this discussion.
- Corner anchors may coincide logically among up to three neighboring cells and
  edge anchors among two. Give them cell-qualified IDs initially; later decide
  whether compatible shared positions should use a canonical lattice-coordinate
  identity for routing and de-duplication.

## 2026-09-02 — r-004 placement relocation

- The isolated placement bridge now maps `r-004` to `slot/q4-r2` (`q:4`,
  `r:2`) at the user's direction. No shared workflow data or production canvas
  behavior changed.
- Because the visible direct-route baseline resolves its endpoints from current
  slot coordinates, the `r-002` to `r-004` segment automatically chooses a new
  nearest vertex-port pair. The preview and Studio build were rechecked.

## 2026-09-02 — Port-topology traversal

- The visible route is no longer a direct endpoint segment. It follows adjacent
  port pairs around frame perimeters and moves between slots only through
  co-located, independently owned ports at a shared junction.
- The SVG route carries the complete concrete port and junction sequences for
  inspection. The isolated prototype currently treats all slots as corridors;
  input/output compatibility, capacity, closure, and replanning are deferred.

## 2026-09-05 — Snapshot-state review

- Reviewed only the Honeycomb snapshot renderers. They apply normalized scene
  attributes at the corresponding rendering boundaries: canvas attributes on
  `HoneycombCanvasModelSnapshot`, path attributes on each route, slot
  attributes on `HoneycombSlotFrameSnapshot`, and node attributes on
  `HoneycombNodeSnapshot`.
- Marked the completed baseline transports for canvas `idle`, node execution
  `idle`, node connection `isolated`, and path flow `idle`. These are real
  renderer-consumed states, not merely catalog entries.
- Kept every state-specific visual or interaction objective open. In
  particular, the snapshots do not yet implement an idle visual treatment,
  diagnostics, drag/drop behavior, capability affordances, path-flow visuals,
  panning, zooming, or production-canvas adoption.
