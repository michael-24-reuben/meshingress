# Summary

Implemented the reviewed MCP WebSocket authentication entry as a thin transport layer at `/mcp/ws`.

## Superseded

Archived on 2026-08-15. The implementation relied on the now-retired raw
header/context security model. Its transport intent remains useful history,
but the active replacement is
`architect/resolved/2026-08-15-mcp-call-context-security-refactor`.

The WebSocket handshake now requires opaque header credentials, stores authenticated session/context attributes, and rejects missing or invalid credentials before a session is established. Incoming text frames are parsed as JSON and delegated directly to the existing `McpDispatcher` with `McpCallContext`, preserving current MCP method routing, role checks, and tool behavior.

Verification passed with the server reactor test slice on the local Java 22 runtime override: 34 tests passed.
