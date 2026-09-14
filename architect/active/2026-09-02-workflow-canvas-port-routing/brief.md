# Workflow Canvas Port Routing

Define stable identities for the honeycomb canvas's stacked, slot-owned vertex
ports and a deterministic policy for choosing and routing between them.

The design must preserve the parent canvas model: a port remains dedicated to
its logical slot even when it shares a coordinate with neighboring ports. It
must also support closed ports, node movement, close-up route presentation,
and future input/output compatibility without prematurely restoring rendered
workflow paths.

## Scope

- Canonical lattice-junction IDs and slot-port IDs.
- Endpoint-selection and route-planning policy.
- Replanning behavior for unavailable or closed ports.
- Data-model examples and unresolved decisions.

## Non-goals

- Implementing paths, routing interaction, or a production migration.
- Merging the three ports that visually stack at one vertex.
- Making a node ID the durable owner of a physical slot port.
