# Context

## Source Report

- `architect/reports/redundant-code-inspection-2026-05-28-171500.md`

## Relevant Files

- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/McpController.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/McpWebSocketHandler.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/McpDispatcher.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/jsonrpc/JsonRpcResponses.java`

## Notes

- Both transports parse JSON and format parse errors similarly, but the logic is duplicated.
- Any changes should preserve JSON-RPC response shape and logging expectations.

