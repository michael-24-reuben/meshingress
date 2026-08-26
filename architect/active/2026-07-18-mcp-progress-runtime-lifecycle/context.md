# Context

The resolved predecessor `2026-07-18-mcp-progress-lifecycle-and-transport` renamed the public hook to `MeshigressWebSocketReporter`. That class remains a transport-adapter draft in `meshingress-tool-api` and has not been wired into server dispatch.

Current `McpDispatchExecutor` imposes configured call timeouts while waiting for terminal execution. A future implementation must distinguish the estimated normal execution window from one fixed grace period and must not extend a hard deadline for every progress event. Activity/inactivity detection, if added, is a separate early-failure policy.

WebSocket can deliver progress notifications on the existing connection. The current HTTP JSON-RPC request/response endpoint cannot emit a live stream under its present contract; any HTTP progress feature needs an explicitly designed asynchronous job/status or event-stream surface.
