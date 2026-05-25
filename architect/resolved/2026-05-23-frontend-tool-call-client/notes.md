# Notes

## Initial Notes

The user wants a frontend-side module, not a backend `toolspace/` module. The appropriate parent is `frontend/`, and the recommended module name is `tool-call-client`.

The requested page is effectively a development/operator console for HTTP MCP tool calls. It should be visually polished but remain simple enough to serve as a static frontend module.

## Design Direction

Use the MCP `tools/list` method as the method of truth. Avoid hard-coded forms except for generic schema-driven controls. This keeps the page compatible with new tools as they are added to Meshingress.

## Implementation Bias

Build HTTP POST first. Keep WebSocket as a later transport abstraction unless the submitted implementation prompt explicitly requires WS in the first pass.

## Naming

Recommended:

```txt
frontend/tool-call-client
```

Alternatives considered:

- `frontend/mcp-client`: accurate but too protocol-oriented.
- `frontend/tool-transport`: too infrastructure-focused and less UI-specific.
- `frontend/meshingress-client`: too broad for current scope.
- `frontend/tool-gateway-client`: reasonable if the server is framed as a gateway, but less direct.

## Response Rendering Detail

The UI should distinguish:

1. JSON-RPC transport error responses using top-level `error`.
2. Tool-level error results using `result.isError` and/or `_meta.errorCode` / `_meta.errorMessage`.
3. Network/client errors caused by failed fetch, CORS, timeout, or invalid JSON.

## Implementation Notes

- Created `frontend/tool-call-client` as plain static HTML/CSS/JavaScript.
- Used the backend `ToolsMcpController` contract for tool invocation: submitted calls use JSON-RPC `method: "tools/call"` with `params.name` and `params.arguments`. This corrects the earlier plan wording that implied the selected tool name should be used as the JSON-RPC method.
- Kept settings in memory only. Bearer token and session ID are optional request header fields and are not persisted.
- Implemented high-risk scope confirmation as advisory, with memory-only "do not ask again" suppression.
- Verified static rendering through headless Chrome. Live endpoint behavior still depends on the configured MCP endpoint being reachable from the browser and allowing CORS.
