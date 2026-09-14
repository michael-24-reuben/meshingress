# Notes

## 2026-09-02 — Initial identity and routing position

- A shared ID should describe the physical/topological vertex, not collapse the
  three distinct slot ports located there.
- `p-<junction-id>:<slot-id>` keeps the user's layered identifier idea while
  preserving a concrete owner for compatibility, capacity, and history.
- Node ownership is intentionally rejected for the durable identifier: drag or
  reassignment would otherwise change the path endpoint identity even though
  the physical slot port did not move.
- A straight line between endpoint slots is an effective first-pass visual
  intention. It is insufficient as a routing rule because it cannot account
  for closed ports, direction, capacity, or whether an intermediate node may
  be crossed. Use it as a cost bias inside a constrained graph search.
- No source or prototype code changed while creating this record.

## 2026-09-02 — Identifier grammar revision

- The initial `j:4:1` / `p-j:4:1:q:1,r:0` proposal is rejected. It exposes
  the correct identity layers but is difficult to scan, especially when nested
  inside a port identifier.
- Proposed replacement: `junction/u4-v1`, `slot/q1-r0`, and
  `port/junction/u4-v1/slot/q1-r0`. The explicit nouns and named axes make log
  output and route inspection self-explanatory without making the coordinates
  opaque.
- The user accepted this grammar for the isolated prototype. It is not yet a
  persisted workflow format or public API contract.

## 2026-09-02 — Prototype identity implementation

- `honeycomb-grid.ts` now generates canonical slot, junction, and concrete
  slot-port identities. The junction coordinate is computed from axial slot
  coordinates and the named vertex corner, avoiding pixel-derived identity.
- The inspiration canvas applies `data-junction-id` and `data-slot-port` to
  every passive port marker. Its existing `data-slot-id` uses the generated
  slot key.
- `docs/workflow-canvas-state-model.md` now uses the same slot grammar and
  documents the identity layers.
- `npm run build` passed. The Studio package has no configured unit-test
  runner. No routing or path layer has been reintroduced.
- A generated-ID invariant check found 210 unique concrete port IDs across 35
  slots and confirmed a three-owner canonical junction. `npm run lint` passed
  with three existing fast-refresh warnings outside the prototype.

## 2026-09-02 — Direct visible route baseline

- The prototype now consumes the sample workflow edges again. For each edge it
  evaluates the 36 possible source/target vertex-port pairs, selects the
  nearest pair, and renders a route segment in a stage-level SVG.
- Routes carry `data-source-port` and `data-target-port` identity attributes.
  Their plain solid cyan stroke is intentionally distinct from the muted
  slot-frame perimeter without using dashes, glow, or arrows.
- A fresh local preview visibly showed the two routes from Manual Trigger to
  Set System Volume and from Set System Volume to Search Solo Leveling. Build
  and lint passed; lint retained three existing warnings outside the prototype.
- No graph search or closed-port handling has been added. This is a visible
  direct-route baseline for refining the port model, not the final planner.
- The initial dashed-arrow visual was superseded at the user's direction.
  Routes now use a plain 3px solid cyan line without a glow or arrow marker,
  keeping flow distinct from the muted slot perimeter without added ornament.

## 2026-09-02 — Lower destination placement

- Moved `r-004` to `slot/q4-r2` in the isolated prototype. The AX tree and
  fresh preview identify Search Solo Leveling only in that slot.
- The direct SVG baseline continues to resolve its endpoint pair dynamically,
  so no route-specific placement override was needed. `npm run build` passed.

## 2026-09-02 — Complete prototype port traversal

- Superseded the direct endpoint segment after user review showed it did not
  follow the port topology. `traversePortGraph` now walks a graph of all
  prototype slot-owned ports.
- Legal prototype links are adjacent corner ports around a slot perimeter and
  zero-cost transfers between concrete stacked ports at the same junction.
  Every slot is presently a corridor; this is an explicit visual-policy choice,
  not a claim about final workflow semantics.
- Each route retains `data-source-port` and `data-target-port`, and now exposes
  the full `data-route-port-ids` and `data-route-junction-ids` sequences. The
  SVG contains a polyline through each distinct port coordinate.
- `npm run build` and `npm run lint` passed. Lint retained only the three
  existing fast-refresh warnings outside the honeycomb prototype.

## 2026-09-02 — Directional port rule

- Recorded the user-directed distinction between availability and traffic role:
  all ports may be open, but each individual port is exclusively `input` or
  `output`, never both.
- The intended directed traversal becomes output-to-input across a junction
  and input-to-output within a slot. A route begins at a source output and ends
  at a target input. Role assignment and enforcement remain a separate slice.

## 2026-09-02 — Dynamic directional-port implementation

- Implemented directional traversal rather than leaving the rule as a todo.
  The graph contains input and output state nodes per open concrete port,
  accepts only output-to-input junction transfers and input-to-output slot
  crossings, and begins/ends every route at source output/target input states.
- Selected route ports are role-locked for subsequent routes in the current
  sample order. Each marker exposes `data-port-availability="open"` and a
  `data-traffic-role` of `open`, `input`, or `output`; input markers are purple
  and output markers are cyan.
- The `r-004` placement was restored to `slot/q4-r2` after fresh preview
  validation caught a source-state regression during this implementation.

## 2026-09-02 — Inner-turn port allocation

- Implemented `flatTopHexInnerTurnPortAllocation`. It identifies the unique
  logical slot whose corner set contains a non-collinear path triple's previous,
  current, and next junctions, then returns that slot's concrete current-port.
- The example `u9-v4 -> u10-v5 -> u9-v6` resolves to
  `port/junction/u10-v5/slot/q2-r1`, the right port of the inner
  `slot/q2-r1`. Allocation remains separate from directional traffic role.
- Routes expose their selected turn port and slot IDs; their markers receive an
  amber allocation ring without replacing cyan output or purple input styling.

## 2026-09-02 — Allocation traffic-role boundary

- An inner-turn allocation identifies foreign pass-through geometry rather
  than traffic owned by the allocated slot/node only when its owning `slotId`
  differs from the allocating route's destination slot. In that case its
  marker retains `data-port-availability="open"` and
  `data-route-turn-allocation="true"`, but omits `data-traffic-role`.
- A destination-slot allocation is not suppressed: it retains its
  `data-traffic-role`, because the port identity establishes that it belongs to
  the route's destination node.
- `npm run build` and `npm run lint` passed after the attribute boundary was
  first introduced. Lint retained only the three existing fast-refresh warnings
  outside the prototype.
- `npm run build` and `npm run lint` also passed after the destination-slot
  ownership correction; the same external fast-refresh warnings remain.

## 2026-09-02 — Logical route spine and endpoint-only traffic roles

- Superseded the destination-slot exception. It confused a pass-through
  junction attachment with a node traffic binding.
- The graph still discovers a legal junction sequence, but the rendered route
  contains one concrete anchor per junction: the source port, the concrete
  port owned by the inward slot of every 120-degree turn, and the target port.
  The sample `r-002 -> r-004` spine is `q2-r0 -> q2-r1 -> q3-r1 -> q3-r1 ->
  q3-r2 -> q4-r2` at junctions `u9-v4 -> u10-v5 -> u9-v6 -> u10-v7 ->
  u12-v7 -> u13-v8`.
- `data-traffic-role` is now emitted only for source `output` and target
  `input` anchors. Every pass-through allocation omits it, including one whose
  slot happens to contain a node or matches a route destination in another
  connection.

## 2026-09-02 — Per-port connection counts

- Added `data-port-connection-count` to every concrete slot-port marker. The
  count is derived from distinct logical route-spine `portIds`, so each stacked
  port is counted independently even when it shares a junction coordinate with
  others.

## 2026-09-02 — Turn-allocation presentation

- Removed the gold allocation ring. Turn allocation remains available through
  `data-route-turn-allocation` but does not add a circular visual treatment to
  connected ports.

## 2026-09-02 — Straight-ahead output continuation

- Replaced the anonymous input routing state with one that retains the exact
  output port that crossed into it at a shared junction. The identity is
  transient planner context; `data-traffic-role` remains endpoint-only.
- The next perimeter transition scores the collinearity of the supplying
  output's slot-center axis and the candidate segment. The straight candidate
  gets no penalty; its sideward sibling stays legal but costs more.
- Direct route evaluation for `r-002 -> r-004` confirmed the requested first
  continuation: `port/junction/u7-v4/slot/q2-r0` then
  `port/junction/u6-v3/slot/q1-r1`, not `u6-v5`.

## 2026-09-03 — Current placement routing defect

- The current placement is `r-002 = slot/q2-r2`, `r-004 = slot/q-1-r2`.
  The solver initializes all six source outputs at cost zero, so it begins at
  `port/junction/u9-v6/slot/q2-r2` instead of selecting a closest eligible
  source-side connection. The raw path never visits the expected
  `port/junction/u4-v7/slot/q1-r2`.
- The collinearity penalty is only a local, unsigned ranking after a junction
  transfer. It does not select the source connection, distinguish forward from
  reverse travel, or prevent backtracking. Separately, rendered `portIds` are
  rebuilt from turn allocations rather than the raw graph-state path, so the
  visible line can misrepresent the concrete ports that carried traffic.

## 2026-09-03 — Route-variant boundary

- Extracted the existing graph decision logic into
  `honeycomb-prototype/route-variants/graph-route-variant.ts` behind the pure
  `HoneycombRouteVariant` interface.
- Every plan returns ordered concrete `ports`: index `0` is the output-origin
  hook and the final index is the input-destination hook. The canvas derives
  SVG points, data attributes, endpoint roles, and per-port counts from that
  shared contract rather than from graph-specific fields.

## 2026-09-03 — Canvas-end slots

- Added a one-slot visual boundary ring around the workflow lattice. A
  `canvas-end` slot is not a button, cannot contain a node, and has a clipped
  diagonal unavailable pattern.
- Its concrete ports are still identified normally; only junctions touching
  the workflow lattice remain `open`. All outward ports expose
  `data-port-availability="closed"`. End slots are excluded from route-policy
  inputs, so they cannot become an implicit traversal corridor.

## 2026-09-03 — Immediate-route seam control

- A direct workflow edge whose source and target placements are axial
  neighbors now owns their shared frame side. Both frame-side strokes are
  hidden, preventing a doubled line through the connected pair.
- The prototype renders one deduplicated, non-interactive flat-top hexagon at
  the nominal shared-side midpoint. It carries source and target slot IDs as
  data attributes for a later side-control interaction pass.
- This is adjacency presentation derived from the edge endpoints, not a change
  to graph-route selection. The control's side length, fill, and stroke remain
  public CSS properties: `--hwp-route-side-control-side`,
  `--hwp-route-side-control-fill`, and `--hwp-route-side-control-stroke`.
