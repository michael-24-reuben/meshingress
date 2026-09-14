# Todo

## Identity objective

- [x] Separate shared lattice-junction identity from slot-owned port identity.
- [x] Define integer canonical junction coordinates from axial slot coordinates
  and corner metadata.
- [x] Keep `slotId`, rather than `nodeId`, as the durable port owner.
- [x] Accept and implement `junction/u<u>-v<v>`, `slot/q<q>-r<r>`, and
  `port/junction/u<u>-v<v>/slot/q<q>-r<r>` in the isolated prototype.
- [x] Expose each concrete port and its shared junction through prototype DOM
  data attributes.

## Route selection objective

- [x] Constrain direct-line trajectory to a route-ranking heuristic rather
  than an identity or compatibility decision.
- [x] Render the sample workflow as visible routes with concrete source and
  target port identities.
- [x] Treat every intermediate prototype slot as a traversable corridor.
- [x] Define and implement legal graph transitions: adjacent perimeter ports
  within a slot and stacked slot-port transfers at a shared junction.
- [x] Keep `availability: open` distinct from endpoint traffic roles: only the
  source anchor is `output` and only the target anchor is `input`.
- [x] Render one slot-owned route anchor per junction: source endpoint, every
  inward 120-degree turn allocation, and target endpoint.
- [ ] Define user-controlled role assignment for each slot's six ports,
  including how drag/reconnection changes an existing binding.
- [ ] Define compatibility, capacity, and closed-port constraints.
- [x] Define a deterministic cost/tie-break policy: perimeter distance plus a
  small direct-trajectory bias, then lexical route ordering.
- [x] Preserve the prior output owner across a junction long enough to rank
  the next perimeter hop by collinearity, preferring straight-ahead traffic
  over an equal-length sideward sibling while retaining that sibling as a
  legal fallback.
- [ ] Replace all-source zero-cost initialization and the derived turn-only
  render spine with destination-aware closest-eligible source selection and
  raw traversal anchors; use signed travel direction and prevent immediate
  backtracking through the same junction.
- [x] Allocate each non-straight route turn to the concrete middle-junction
  port owned by the slot inside that angle.
- [ ] Define closed-port invalidation and replanning behavior.

## Presentation objective

- [x] Add a visual-only end-of-canvas slot ring that cannot host a node or
  receive pointer events; its outward concrete ports are closed.
- [x] Replace the two coincident frame strokes at a direct adjacent route with
  one shared seam control at that side's midpoint.
- [ ] Define compact-view stacked-port treatment.
- [ ] Define close-up endpoint fanning and directional curve tangents without
  changing logical endpoint identity.

## Implementation boundary

- [x] Receive approval and implement multi-hop port traversal in the isolated
  prototype; semantic port constraints and interaction remain future slices.
- [x] Extract the current graph policy behind a reusable route-variant
  interface whose ordered concrete ports define the origin and destination
  hooks.
- [ ] Add and compare trajectory, manual, and debug route variants without
  changing canvas rendering code.
