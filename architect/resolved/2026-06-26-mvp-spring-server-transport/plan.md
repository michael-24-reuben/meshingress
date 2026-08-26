# Plan

1. Create or adapt the server application module after the user creates/approves it.
2. Bind external config into Spring properties.
3. Wire account registry and Nextcloud clients.
4. Wire tool modules into the runtime.
5. Expose health, tools list, and tools call endpoints.
6. Add optional `/mcp` JSON-RPC mapping if within MVP.
7. Add controller/service tests with fake runtime or fake clients.

## Acceptance Criteria

- Controllers delegate to runtime services instead of duplicating tool logic.
- Server startup fails clearly on invalid config.
- Tool invocation path applies the same validation and policy checks as CLI.
- Health and tool listing can be verified without live destructive operations.
