# Fixes

## Files Changed

- `app/meshingress-server/pom.xml`
- `app/meshingress-server/src/main/resources/application.properties`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/security/SecurityConfig.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/config/McpWebSocketConfig.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/auth/McpCredentialValidator.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/auth/McpAuthenticatedSession.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/auth/InvalidMcpCredentialException.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/McpAuthHandshakeInterceptor.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/McpWebSocketHandler.java`
- `app/meshingress-server/src/test/java/dev/mrk/meshingress/mcp/McpWebSocketAuthTests.java`

## Behavior

- Added `spring-boot-starter-websocket` to the server module.
- Added configurable `/mcp/ws` WebSocket registration with allowed-origin patterns.
- Permitted the configured WebSocket upgrade path in `SecurityConfig`.
- Added an MVP `McpCredentialValidator` that validates opaque header credentials:
  - `Authorization: Bearer <access-token>`
  - `X-Secret-Key`
  - `X-Auth-Token`
- Added handshake middleware that rejects missing credentials with `401` and invalid credentials with `403`.
- Stored authenticated session and MCP context headers in WebSocket session attributes.
- Added a text-frame handler that parses JSON, builds `McpCallContext`, delegates to `McpDispatcher#dispatch(...)`, and sends dispatcher responses as WebSocket text.
- Kept JSON-RPC routing, role checks, MCP method handling, and tool execution in the existing dispatcher/controller stack.

## Deliberate Non-Changes

- Did not add the sample `McpService`.
- Did not duplicate JSON-RPC method routing in the WebSocket handler.
- Did not introduce JWT/resource-server support yet.
- Did not add browser query-string, cookie, or initial-auth-frame authentication.
- Did not implement production token revocation; that remains a future security decision.
