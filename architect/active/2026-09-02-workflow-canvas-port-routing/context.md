# Context

## Parent Design

- Parent record: `architect/active/2026-08-31-workflow-canvas-honeycomb-slots`.
- A flat-topped logical slot owns six `SlotPort`s at its nominal frame
  vertices: `top-left`, `top-right`, `right`, `bottom-right`,
  `bottom-left`, and `left`.
- Three logical slots meet at every such vertex. Their markers may share the
  same coordinate, but they are deliberately distinct slot-owned endpoints.
- Port coordinates use the nominal `--hwp-frame-side` geometry; visual frame
  shrinkage through `--hwp-grid-gap` does not move them.
- The isolated prototype now renders the two sample workflow edges as visible
  direct SVG routes between concrete vertex ports. It remains an inspiration
  environment, not a production routing implementation.

## Identity Layers

A shared coordinate is not itself a bindable port. Use two identities:

1. **Lattice junction** — a canonical, shared topology location.
2. **Slot port** — a concrete, slot-owned endpoint positioned at that junction.

For a slot at axial `(q, r)`, calculate its vertex in integer lattice units
`(u, v)`. These values avoid pixel rounding and remain stable at every zoom:

| Corner | `u` | `v` |
|---|---:|---:|
| `top-left` | `3q + 1` | `2r + q` |
| `top-right` | `3q + 3` | `2r + q` |
| `right` | `3q + 4` | `2r + q + 1` |
| `bottom-right` | `3q + 3` | `2r + q + 2` |
| `bottom-left` | `3q + 1` | `2r + q + 2` |
| `left` | `3q` | `2r + q + 1` |

The former `j:<u>:<v>` / `p-j:<u>:<v>:q:<q>,r:<r>` text form is rejected as
too dense and punctuation-heavy. The accepted identifier grammar makes every
layer explicit:

```txt
junction/u<u>-v<v>
slot/q<q>-r<r>
port/junction/u<u>-v<v>/slot/q<q>-r<r>

junction/u4-v1
slot/q1-r0
port/junction/u4-v1/slot/q1-r0
```

The slash-delimited form is intentionally a structured key, not an HTML `id`
or CSS selector. The prototype renders it through `data-*` attributes; a
future workflow model may retain it directly. The corner remains metadata for
orientation and rendering: the canonical junction plus owning slot identifies
exactly one corner on a regular honeycomb. Negative coordinates remain
unambiguous, for example
`port/junction/u-2-v3/slot/q-1-r2`.

The owner must be the **slot**, not the node. A node can move away while the
slot's physical endpoint and its route history remain intelligible. The node
currently occupying that slot is referenced separately.

```json
{
  "junctionId": "junction/u4-v1",
  "slotPortId": "port/junction/u4-v1/slot/q1-r0",
  "slotId": "slot/q1-r0",
  "corner": "top-left",
  "occupyingNodeId": "r-005",
  "direction": "bidirectional",
  "availability": "open",
  "capacity": 1
}
```

## Routing Principle

The user-proposed straight trajectory is valuable as a **routing bias**, but
not as the route or endpoint identity. A geometric line alone cannot decide
whether a port is open, compatible, already at capacity, or part of a valid
directional traversal.

The intended planner therefore works in this order:

1. Resolve source and destination slots/nodes to eligible concrete slot ports.
2. Project a direct source-to-destination line only to score likely corridor
   junctions and reduce visual detours.
3. Search a topology graph whose concrete endpoint nodes are `SlotPort`s and
   whose shared coordinate nodes are `JunctionId`s. Keep stacked ports distinct
   while allowing a transition at their common junction.
4. Score candidates by travel distance, deviation from the direct trajectory,
   turn count/angle, port compatibility, capacity, closed-port exclusion, and
   stable deterministic tie-breakers.
5. Commit the selected concrete source and destination `SlotPortId`s plus the
   resulting hop sequence. A close-up view may fan the presentation of stacked
   endpoints but cannot change these logical IDs.
6. If a port closes or becomes invalid, invalidate only affected graph edges
   and rerun the planner. Do not merely redraw the same straight line.

## Current Graph-Traversal Baseline

The prototype now routes through an explicit slot-port graph. Each concrete
`SlotPort` has two adjacent perimeter links within its owner slot. All concrete
ports at one `JunctionId` have zero-cost transfer links, preserving stacked
ownership while allowing the route to cross between frames only at the shared
vertex. The route begins at any source-slot port and ends at any target-slot
port; it then renders every distinct port coordinate as a plain solid SVG
polyline beneath frames and nodes.

Every prototype slot is currently a traversable corridor. The graph cost is
perimeter length plus a small distance-from-direct-trajectory penalty; exact
ties use lexically ordered port IDs. That makes the result stable without
reintroducing a straight-line shortcut.

At a junction, a temporary input state now includes the concrete output port
that supplied it. The immediate in-slot transition compares that output port's
radial geometric axis with each adjacent perimeter segment and adds a
collinearity penalty. An aligned continuation is preferred over its sideward
sibling but does not make the sibling unavailable. This is transient routing
context, not a third identity layer or a persistent traffic role.

The current sample confirms the rule at `r-002`: traffic from
`port/junction/u7-v4/slot/q2-r0` is redirected through `slot/q1-r1` to
`port/junction/u6-v3/slot/q1-r1`, rather than its sideward
`port/junction/u6-v5/slot/q1-r1` alternative.

The remaining product decision is now the **semantic traversal policy**:
whether occupied, unavailable, directionally incompatible, or at-capacity
ports remove graph links or merely increase their costs. Closure invalidation
and rerouting remain future work.

## Directional Port Rule

The user established that every port is initially available to traffic, but an
available port must still have one exclusive traffic role: `input` or `output`.
`open` is availability, not a synonym for bidirectional traffic. An input port
may receive but never emit; an output port may emit but never receive.

The graph may use directional state internally to reject invalid physical
handoffs, but that is not a traffic-role claim by the slot/node. The visible
and persisted route spine owns only two traffic roles:

```txt
source node -> output port
source output -> pass-through anchors selected by inward turns
pass-through anchors -> target input
input port -> target node
```

The prototype assigns `data-traffic-role` only to route endpoints: output at
the origin slot and input at the destination slot. Every middle anchor retains
`data-port-availability="open"` and can expose its turn-allocation marker, but
has no traffic-role attribute. User-controlled assignment and reassignment
remain future work.

Each concrete marker also exposes `data-port-connection-count`, calculated from
the exact `SlotPortId`s in distinct logical route spines. It never aggregates
the stacked ports sharing a `JunctionId`.

## Inner-Turn Allocation

For a route junction triple `previous -> current -> next`, the current junction
has three stacked concrete ports. The interior slot is the unique axial slot
whose six-corner set contains all three junction coordinates. The prototype
allocates that slot's `current`-junction port to the turn and records it on the
route separately from the input/output role. The allocation marker retains its
availability and always omits `data-traffic-role`; it has no additional
allocation-ring treatment. An interior allocation represents the inward side
of the path's 120-degree turn, never a node traffic binding.

For example, `junction/u9-v4 -> junction/u10-v5 -> junction/u9-v6` allocates
`port/junction/u10-v5/slot/q2-r1`, the `right` port of `slot/q2-r1`. This is
the 120-degree interior-side owner. Collinear triples have no inner side and
therefore no allocation.

## Current Sample Placement

- The isolated placement bridge maps `r-004` to `slot/q4-r2`. This serves as a
  lower-right direct-route example for the baseline, while `r-001` and `r-002`
  retain their existing placements.
- Moving the node does not alter either slot-port identity grammar or routing
  policy: the baseline simply recalculates its nearest source/target vertex
  pair from the new coordinates.
