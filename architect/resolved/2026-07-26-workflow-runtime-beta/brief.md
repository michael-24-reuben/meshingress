# Workflow Runtime Beta Draft

## Goal

Define the beta contract for server-side executable workflows. A workflow must be a versioned graph of native and tool-call nodes, compiled and executed independently of the Workflow Studio's canvas layout.

## Scope

- Typed node results backed by JSON values.
- Stable node graph addressing through definition-level `requestId` values.
- Port-based control edges and argument bindings to named node results.
- Direct result routing for boolean, string, number, object, array, and null results.
- Separate `merge` and `join` native nodes.
- Per-node failure handling with explicit error-flow edges instead of a generic `try/catch` wrapper node.
- A later bridge from executable workflow nodes to the existing internal tool executor.

## Non-goals

- Workflow Studio UI layout, canvas routing, or React migration.
- Backend implementation in this entry.
- Treating workflow layout coordinates or widths as executable data.
- Defining final workflow-instance durability, recovery, cancellation, or scheduling behavior.

## Expected Outcome

The next implementation entry can build a workflow compiler and runtime against a reviewed beta definition contract without re-deciding node identity, result routing, join semantics, or error-flow structure.
