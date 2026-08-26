# Brief

Consolidate duplicate visibility and allow/deny list checks in the tool registry while preserving current `tools/list` and `tools/call` behavior.

## Scope

- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/registry/InMemoryToolRegistry.java`

## Constraints

- Preserve allowlist/denylist semantics.
- Keep visibility rules for tools and functions consistent with current behavior.

