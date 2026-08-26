# PRD: MCP WebSocket Authentication Middleware

## Problem

MCP traffic needs to be served over WebSocket while enforcing authentication based on access tokens, secret keys, and auth tokens. The authentication should happen before a WebSocket session is accepted.

## Requirements

### Functional Requirements

- Add a WebSocket endpoint for MCP traffic, expected path: `/mcp/ws`.
- Validate the following during the HTTP-to-WebSocket upgrade request:
  - `Authorization: Bearer <access-token>`
  - `X-Secret-Key: <secret-key>`
  - `X-Auth-Token: <auth-token>`
- Reject unauthenticated or invalid clients before the WebSocket connection is established.
- Attach an authenticated principal or session context to the WebSocket session after successful verification.
- Route authenticated MCP messages to the existing MCP service/application logic.
- Preserve existing MCP behavior wherever possible.

### Security Requirements

- Do not store raw secret keys or raw auth tokens.
- Persist only hashes of secret keys and auth tokens.
- Use constant-time password/hash verification through Spring Security's `PasswordEncoder`.
- Use TLS/WSS in production.
- Keep auth validation separate from MCP method dispatch.
- Add scope/permission checks per MCP method where applicable.

### Non-Goals

- Do not implement a replacement `McpService`.
- Do not use the sample `McpService` from the initial design draft.
- Do not modify existing service behavior unless required to accept an authenticated context.
- Do not introduce STOMP unless a later design requires it.

## Acceptance Criteria

- A client with valid access token, secret key, and auth token can open `/mcp/ws`.
- A client missing any required credential is rejected during handshake.
- A client with expired or revoked credentials is rejected during handshake.
- The WebSocket handler receives authenticated session context.
- Existing MCP service logic remains intact.
- The handler delegates to the existing service rather than containing business logic.
- Tests cover successful and failed handshakes.


## Review-Derived Requirements

- Reuse `McpDispatcher#dispatch(JsonNode request, McpCallContext context)` for all WebSocket messages.
- Reuse `McpCallContext` from `lib/meshingress-tool-api`.
- Preserve `McpController.java` and the existing HTTP MCP transport.
- Preserve existing `McpMethodController` implementations.
- Preserve `RoleAuthorizationService` behavior by mapping WebSocket handshake metadata into `McpCallContext`.
- Support JSON-RPC notifications by sending no reply when dispatcher returns an empty response.
- Support JSON-RPC batches by returning the dispatcher-produced response array unchanged.
- Do not introduce a parallel MCP method router.
- Do not introduce duplicate method permission checks in the WebSocket handler.
