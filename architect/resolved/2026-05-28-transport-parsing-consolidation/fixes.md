# Fixes

## Files Changed

- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/McpTransportDispatcher.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/McpController.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/McpWebSocketHandler.java`
- `app/meshingress-server/src/test/java/dev/mrk/meshingress/mcp/McpControllerTests.java`

## Changes

- Added `McpTransportDispatcher` to parse JSON payloads, return canonical JSON-RPC parse errors, and delegate valid requests to `McpDispatcher`.
- Updated HTTP `/mcp` and WebSocket `/mcp/ws` handlers to use the shared dispatcher helper.
- Added HTTP parse-error coverage in `McpControllerTests`.
