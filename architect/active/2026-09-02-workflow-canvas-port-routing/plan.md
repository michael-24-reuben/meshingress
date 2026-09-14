# Plan

1. Confirm the canonical junction and slot-port ID grammar with the user.
2. Decide whether paths can traverse intermediate slots and, if so, whether
   they may traverse occupied nodes, empty slots, or only explicit relay nodes.
3. Define the initial graph: junction transitions, permitted intra-slot
   transitions, directional constraints, capacity, and obstruction rules.
4. Define a deterministic route cost and tie-break policy, using direct-line
   trajectory and straight-ahead perimeter continuity as biases rather than
   binding rules.
5. Model closed-port invalidation and incremental replanning.
6. Specify compact versus close-up presentation of stacked endpoints and route
   tangents.
7. Only after those decisions, implement a prototype routing slice and verify
   it before considering production integration.
