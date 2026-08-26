# Context

## Existing Project

The project is a Spring Boot application named `meshingress`.

Current known dependencies include:

- Spring Boot WebMVC
- Spring Boot Actuator
- Spring Security
- Validation
- Spring Data JPA
- PostgreSQL
- Springdoc OpenAPI
- Lombok

This work should add WebSocket support without disrupting the existing HTTP/API structure.

## Design Notes

Authentication should occur during the WebSocket handshake, before the session is accepted. This makes auth middleware explicit and prevents unauthorized sockets from being established.

The middleware should validate three credential classes:

1. access token
2. secret key
3. auth token

After validation, the middleware should attach a principal/session context to the WebSocket attributes so the handler can safely delegate authenticated messages.

## Service Preservation Instruction

Conserve the existing MCP service.

The prior design included a sample `McpService` for demonstration. That sample must not be included in implementation. The implementing agent should instead locate the current MCP service/application logic and delegate to it.

If the existing service API does not directly accept an authenticated principal, prefer a thin adapter or context wrapper rather than rewriting the service.

## Open Questions

- Are access tokens JWTs issued by `meshingress`, or opaque tokens validated against a database/external issuer?
- Should auth tokens be single-use, rotating, or long-lived?
- Does MCP need browser WebSocket support, or only server/client support where custom headers are available?
- What scope names should guard MCP methods such as `tools/list`, `tools/call`, and resource reads?


## Issue Review Summary

The review confirms the implementation should be limited to transport and authentication plumbing. The WebSocket route should not become a separate MCP implementation.

Primary integration point:

```java
McpDispatcher#dispatch(JsonNode request, McpCallContext context)
```

The WebSocket handler should mirror the current HTTP MCP controller behavior: collect context, parse JSON, call dispatcher, return dispatcher output.

## Concrete Integration Points From Review

Add/update:

- `app/meshingress-server/pom.xml`
- `dev.mrk.meshingress.config.McpWebSocketConfig`
- `dev.mrk.meshingress.mcp.McpAuthHandshakeInterceptor`
- `dev.mrk.meshingress.auth.McpCredentialValidator`
- optional `dev.mrk.meshingress.auth.AccessTokenValidator`
- `dev.mrk.meshingress.mcp.McpWebSocketHandler`
- `dev.mrk.meshingress.security.SecurityConfig`
- WebSocket handshake and message-flow tests

Reuse as-is:

- `app/.../controller/McpController.java`
- `app/.../controller/McpDispatcher.java`
- `McpMethodController` implementations
- `lib/meshingress-tool-api` `McpCallContext`
- tool SPI types
- existing role/permission services

## Risks

- Browser WebSocket clients cannot freely set custom upgrade headers.
- Query-param tokens can leak through logs if browser support is added.
- Long-lived WebSocket sessions require token revocation handling.
- Duplicating dispatcher logic would create drift from HTTP MCP behavior.
- Any mutable state in MCP controllers should be audited for concurrent WebSocket calls.

## Implementation Guardrail

Any implementation agent should treat this as the central guardrail:

```txt
Do not build a new MCP service.
Do not port the sample McpService.
Do not duplicate MCP method routing.
Authenticate the WebSocket connection.
Build McpCallContext.
Delegate to the existing McpDispatcher.
```
