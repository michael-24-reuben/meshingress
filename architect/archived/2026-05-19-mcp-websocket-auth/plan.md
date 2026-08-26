# Plan

## Design Decision

Implement MCP WebSocket support as a thin authenticated transport, not as a new MCP service layer.

The implementation must authenticate during the WebSocket handshake, then delegate every JSON-RPC text frame to the existing dispatcher:

```java
Optional<JsonNode> response = dispatcher.dispatch(request, context);
```

The earlier sample `McpService` must not be implemented, copied, or used as a replacement for the current MCP method/dispatch/service stack.

## Proposed Architecture

```txt
Client
  -> HTTP WebSocket upgrade /mcp/ws
  -> McpAuthHandshakeInterceptor
      - validate Authorization
      - validate X-Secret-Key
      - validate X-Auth-Token
      - store authenticated principal and MCP header context
  -> McpWebSocketHandler
      - parse incoming text frame to JsonNode
      - build McpCallContext from session attributes
      - call existing McpDispatcher
      - send dispatcher response if present
```

## Existing Logic to Preserve

The following existing components are the source of truth and should remain unchanged unless a narrow adapter is required:

- `app/.../controller/McpController.java`
- `app/.../controller/McpDispatcher.java`
- existing `McpMethodController` implementations
- `lib/meshingress-tool-api` types such as `McpCallContext`
- existing role/permission logic such as `RoleAuthorizationService`

## Header Mapping

During the WebSocket handshake, capture the same metadata the HTTP MCP controller currently uses.

Recommended mapping:

| Upgrade request header | `McpCallContext` field |
|---|---|
| `Authorization` | `authorizationHeader` |
| `X-Mcp-Role` | `roleHeader` |
| legacy `X-Mcp-Admin` | fallback/admin compatibility input if currently supported |
| `Mcp-Session-Id` | `sessionId` |
| `X-Request-Id` | `requestId` |

Additional auth-only headers:

| Header | Purpose |
|---|---|
| `X-Secret-Key` | client/app secret validation |
| `X-Auth-Token` | rotatable MCP auth token validation |

## Implementation Steps

### 1. Add WebSocket Dependency

Add to the server app Maven file:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### 2. Update Security Config

Permit the WebSocket upgrade route so the request can reach the handshake interceptor.

Example target:

```java
requestMatchers("/mcp", "/mcp/ws", "/actuator/health").permitAll()
```

Exact matcher style should follow the existing `SecurityConfig`.

### 3. Add Credential Validation Layer

Add or adapt:

- `dev.mrk.meshingress.auth.McpCredentialValidator`
- optional `AccessTokenValidator`
- optional authenticated principal/session object
- `InvalidMcpCredentialException`

Validation inputs:

- `Authorization`
- `X-Secret-Key`
- `X-Auth-Token`

Validation output:

- authenticated principal/session information
- enough header/context data to build `McpCallContext`

Secrets and auth tokens must be stored as hashes if persisted.

### 4. Add Handshake Middleware

Create `dev.mrk.meshingress.mcp.McpAuthHandshakeInterceptor`.

Responsibilities:

- implement `HandshakeInterceptor`;
- read headers from `ServerHttpRequest.getHeaders()`;
- validate credentials;
- reject missing/invalid credentials with 401/403;
- store authenticated principal and context headers in WebSocket session attributes.

### 5. Add WebSocket Config

Create `dev.mrk.meshingress.config.McpWebSocketConfig`.

Responsibilities:

- register `McpWebSocketHandler` at `/mcp/ws`;
- attach `McpAuthHandshakeInterceptor`;
- configure allowed origins.

### 6. Add WebSocket Handler

Create `dev.mrk.meshingress.mcp.McpWebSocketHandler`.

Responsibilities:

- require authenticated session attributes;
- parse each text frame into `JsonNode`;
- build `McpCallContext` from session attributes;
- call `McpDispatcher#dispatch(JsonNode request, McpCallContext context)`;
- if dispatcher returns `Optional.empty()`, send no reply;
- if dispatcher returns a response, serialize and send it as a text frame;
- do not perform method routing or role checks locally.

Pseudo-flow:

```java
JsonNode request = objectMapper.readTree(message.getPayload());
McpCallContext context = buildContext(session);
Optional<JsonNode> response = dispatcher.dispatch(request, context);

if (response.isPresent()) {
    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response.get())));
}
```

### 7. Add Tests

Handshake tests:

- valid `Authorization`, `X-Secret-Key`, `X-Auth-Token` accepts the connection;
- missing access token rejects the connection;
- missing/invalid secret key rejects the connection;
- missing/invalid/expired auth token rejects the connection;
- revoked token behavior is covered once revocation strategy is chosen.

Message-flow tests:

- single JSON-RPC request receives dispatcher response;
- notification produces no WebSocket reply;
- batch request returns dispatcher-produced array;
- `McpCallContext` is populated correctly;
- role/admin compatibility behaves the same as HTTP MCP route.

## Browser Client Decision

Native browser WebSocket clients cannot freely set custom headers.

Default position:

- target server-to-server WebSocket clients first;
- use header-based auth for the initial implementation.

If browser support becomes required, choose and document one of:

1. query parameter auth with strict log redaction;
2. cookie auth with CSRF controls;
3. initial auth message after connection establishment.

This is a product/security decision and should not be silently mixed into the server-to-server design.

## Revocation Decision

Long-lived WebSocket sessions require a revocation strategy.

Acceptable approaches:

1. revalidate credentials periodically;
2. revalidate credentials on each request;
3. create server-side authenticated sessions and revoke by session id.

At least one strategy must be selected before production release.
