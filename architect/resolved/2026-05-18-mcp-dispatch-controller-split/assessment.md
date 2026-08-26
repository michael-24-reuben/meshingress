# Assessment

The old dispatcher mixed JSON-RPC envelope handling with every method-family implementation. That made `dispatchSingle` the growth point for unrelated native, tool, and privileged registry behavior.

The better boundary is:

- `McpDispatcher` owns JSON-RPC envelope validation, batch handling, response wrapping, and method-family lookup.
- `InternalMcpController` owns native MCP lifecycle methods.
- `ToolsMcpController` owns public tool discovery and execution.
- `RolesMcpController` owns role-gated registry methods.

The privileged registry family is now named `roles/tools/*`, with admin retained as the first concrete role credential.
