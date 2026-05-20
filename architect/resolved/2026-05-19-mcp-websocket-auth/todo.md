# Todo

## Dependency / Config

- [x] Add `spring-boot-starter-websocket` dependency to the server app.
- [x] Add MCP WebSocket path config, defaulting to `/mcp/ws`.
- [x] Update `SecurityConfig` to permit the WebSocket upgrade route.
- [x] Configure allowed origins for the WebSocket endpoint.

## Auth Middleware

- [x] Create or adapt `McpCredentialValidator`.
- [x] Validate `Authorization`.
- [x] Validate `X-Secret-Key`.
- [x] Validate `X-Auth-Token`.
- [x] Keep persisted secrets/tokens out of scope for MVP; the validator is a configurable dev/test stub.
- [x] Create `McpAuthHandshakeInterceptor`.
- [x] Reject failed handshakes with 401/403.
- [x] Store authenticated principal/context fields in WebSocket session attributes.

## Dispatcher Preservation

- [x] Locate current `McpController.post(...)` flow.
- [x] Locate current `McpDispatcher#dispatch(JsonNode request, McpCallContext context)`.
- [x] Reuse `McpCallContext`.
- [x] Map `Authorization`, `X-Mcp-Role` / `X-Mcp-Admin`, `Mcp-Session-Id`, and `X-Request-Id` from handshake data.
- [x] Implement `McpWebSocketHandler` as transport-only code.
- [x] Ensure handler delegates to `McpDispatcher`.
- [x] Ensure handler does not implement MCP method routing.
- [x] Ensure handler does not duplicate role/permission checks.
- [x] Do not implement or copy the sample `McpService` from the initial draft.

## JSON-RPC Behavior

- [x] Parse incoming text frames to `JsonNode`.
- [x] Send dispatcher response as text when present.
- [x] Send no reply for notifications / empty dispatcher response.
- [x] Preserve batch response behavior exactly as dispatcher returns it.
- [x] Handle malformed JSON with an appropriate JSON-RPC parse error.

## Tests

- [x] Add valid handshake test.
- [x] Add missing access-token test.
- [x] Add invalid secret-key test.
- [x] Add invalid auth-token coverage through shared invalid credential handling.
- [x] Defer revoked-token test until a production revocation strategy is selected.
- [x] Add single request -> response WebSocket test.
- [x] Add notification -> no reply WebSocket test.
- [x] Preserve batch behavior by returning dispatcher output unchanged.
- [x] Add test proving `McpCallContext` reaches dispatcher.
- [x] Add test proving legacy role/admin handshake mapping matches HTTP MCP route context construction.

## Decisions Needed

- [x] Decide whether access tokens are JWTs, opaque DB tokens, or externally introspected tokens: opaque configurable token for MVP.
- [x] Decide whether browser WebSocket clients are in scope: out of scope for MVP.
- [x] Decide production token revocation strategy for long-lived sessions: deferred production follow-up, not part of the stub MVP.
