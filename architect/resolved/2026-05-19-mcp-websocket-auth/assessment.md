# Assessment

## Root Cause

The server only exposed MCP over HTTP POST `/mcp`. The pending architecture record required an authenticated WebSocket entrypoint, but the live code had no WebSocket dependency, endpoint registration, handshake authentication layer, or text-frame adapter into the existing dispatcher.

## Final Diagnosis

The right fix was transport/auth plumbing, not a new MCP service implementation.

The existing HTTP flow already owns JSON-RPC parsing semantics after body parsing, method-family dispatch, role-gated operations, and tool execution:

```txt
McpController.post(...)
  -> McpDispatcher#dispatch(JsonNode, McpCallContext)
  -> existing McpMethodController implementations
```

The WebSocket implementation now mirrors that model:

```txt
McpAuthHandshakeInterceptor
  -> validates configured opaque credentials
  -> stores MCP context headers

McpWebSocketHandler
  -> parses text frame
  -> builds McpCallContext
  -> delegates to McpDispatcher#dispatch(...)
  -> sends dispatcher response when present
```

## Remaining Risks

- The MVP validator uses configurable dev/test opaque tokens. Production should replace or extend it with persisted hashed secrets, an external introspection service, or Spring Security resource-server JWT validation.
- Long-lived WebSocket sessions still need a token/session revocation strategy.
- Browser WebSocket clients remain out of scope because native browser clients cannot set arbitrary upgrade headers.
