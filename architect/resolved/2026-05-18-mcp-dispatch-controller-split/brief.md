# Brief

`McpDispatcher#dispatchSingle` currently owns protocol validation, native MCP lifecycle methods, public tool methods, and admin-only registry methods. The method is too broad for continued growth.

Refactor the MCP JSON-RPC dispatch surface so method-family handling is separated into:

- internal native MCP methods such as `initialize`, `notifications/initialized`, and `ping`
- public tool methods such as `tools/list` and `tools/call`
- role-gated methods replacing the `admin/tools/*` naming with `roles/tools/*`

Place the new handler classes under `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller` by package and gather them through the main MCP controller/dispatcher path.
