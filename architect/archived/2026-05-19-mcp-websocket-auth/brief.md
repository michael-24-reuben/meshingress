# Brief

## Request

Add MCP as a WebSocket endpoint that requires authentication verification before accepting a connection.

The authentication mechanism should validate:

- access tokens
- secret keys
- auth tokens

The verification should be inserted as middleware during the WebSocket handshake, so invalid clients are rejected before the WebSocket session is established.

## Critical Constraint

Conserve the existing MCP service logic.

Do not replace, rewrite, or introduce a sample `McpService` implementation as part of this work. The previous draft's sample service section must be treated as illustrative only and excluded from implementation.

The WebSocket layer should delegate into the existing service/application logic after authentication succeeds.

## Goal

Create a secure MCP WebSocket entrypoint that:

1. Performs auth verification during the WebSocket handshake.
2. Validates all required credentials.
3. Attaches the authenticated identity to the WebSocket session.
4. Allows the existing MCP service logic to handle authenticated MCP messages.
5. Keeps authentication, transport, and service/business logic separated.

## Scope

In scope:

- WebSocket dependency addition.
- WebSocket endpoint configuration.
- Handshake authentication middleware.
- Credential validation service.
- Authenticated principal/session context.
- Final WebSocket handler/controller shape.
- Security configuration for the WebSocket upgrade path.

Out of scope:

- Replacing existing MCP service logic.
- Implementing sample MCP business methods such as `initialize`, `tools/list`, or `ping` unless they already exist.
- Changing unrelated REST controllers.
- Changing persistence beyond what is required to support credential lookup.


## Issue Review Incorporation

The implementation review confirms the design is implementable and should be limited to transport and authentication plumbing.

The WebSocket endpoint must be a thin transport layer:

1. authenticate during the WebSocket handshake;
2. capture the same request metadata currently used by the HTTP MCP route;
3. build `McpCallContext`;
4. delegate each incoming JSON-RPC payload to the existing `McpDispatcher`;
5. return dispatcher responses as WebSocket text frames.

The handler must not duplicate JSON-RPC parsing semantics, role checks, MCP method routing, or tool/business behavior already owned by `McpDispatcher`, `McpMethodController`, `RoleAuthorizationService`, or existing MCP services.

## Concrete Preservation Rule

The current HTTP MCP flow is the canonical reference:

```txt
McpController.post(...)
  -> reads headers
  -> builds McpCallContext
  -> parses body as JsonNode
  -> calls McpDispatcher#dispatch(JsonNode request, McpCallContext context)
  -> returns dispatcher response
```

The WebSocket flow must mirror that model:

```txt
McpAuthHandshakeInterceptor
  -> validates credentials
  -> stores authorization/header/context fields in session attributes

McpWebSocketHandler.handleTextMessage(...)
  -> parses frame as JsonNode
  -> builds McpCallContext from session attributes
  -> calls McpDispatcher#dispatch(JsonNode request, McpCallContext context)
  -> sends response text only when dispatcher returns a response
```
