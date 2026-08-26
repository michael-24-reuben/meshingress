# Fixes

- Replaced raw `McpCallContext` fields with request IDs, verified principal,
  declared client metadata, workflow lineage, and execution control.
- Added server-only `McpTransportEvidence`, a principal resolver, and one
  context factory for HTTP, MCP WebSocket, and workflow ingress.
- Removed `X-Mcp-Role` and `X-Mcp-Admin` from MCP HTTP, WebSocket, workflow,
  and CORS privilege handling. Administrator checks now use principal roles.
- Added `ToolAccessService` and made `tools/list` and tool execution consume
  its common decision. Admin inventory remains observational only.
- Replaced raw-evidence audit/cache usage with the verified principal subject.
- Added execution deadline/cancellation state and parent-preserving workflow
  child contexts.
