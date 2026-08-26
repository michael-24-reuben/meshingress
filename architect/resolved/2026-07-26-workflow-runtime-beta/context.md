# Context

## Existing Boundaries

- The standalone Workflow Studio currently holds sample topology locally and serializes it; its canvas, node positions, widths, and edge geometry are frontend-only concerns.
- `app/meshingress-server` already has an internal `ToolExecutor` boundary with `execute(String toolName, ObjectNode arguments, McpCallContext context)`.
- MCP tools remain the extension surface. Workflow native nodes must not be represented as arbitrary external tools merely to fit that surface.

## Decisions Captured From Discussion

- A node's `requestId`, rather than a generic display name, is the stable graph address used by `from` and `to` edge endpoints.
- Named results are JSON values with declared JSON types, not untyped strings.
- Result routing is local to the producing node and writes control to named output ports.
- Boolean values route directly through `true` and `false` ports.
- Non-boolean values evaluate ordered boolean predicates and use `default` when no case matches.
- Object and array cases name their target with a JSON Pointer.
- `merge` and `join` have intentionally different semantics and must remain separate native nodes.
- Failure policy belongs on the executable node and uses an explicit `error` route to a separate downstream flow.

## Important Constraint

The definition-level `requestId` must not be confused with existing request identifiers used by HTTP, MCP, storage, or audit context. A workflow run needs a separate per-attempt runtime trace identity.

## Related Work

- `architect/pending/2026-05-23-runtime-tool-creation-and-registry`
- `architect/active/2026-07-18-mcp-progress-runtime-lifecycle`
