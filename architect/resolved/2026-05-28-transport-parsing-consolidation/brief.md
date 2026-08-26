# Brief

Consolidate JSON parsing and parse-error handling between the HTTP MCP controller and WebSocket handler while keeping JSON-RPC envelope behavior and logging consistent.

## Scope

- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/McpController.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/McpWebSocketHandler.java`
- New shared helper under `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/`

## Constraints

- Preserve JSON-RPC response formatting.
- Keep behavior for parse errors and oversized messages intact.
- Avoid changing MCP dispatch semantics.

