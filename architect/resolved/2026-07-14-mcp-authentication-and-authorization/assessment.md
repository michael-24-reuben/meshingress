# Assessment

## Confirmed Baseline

- HTTP and WebSocket requests were logged with request/session identifiers, and tool calls additionally logged their tool name.
- `initialize.params.clientInfo` was parsed only for protocol behavior and then discarded. No client name or version was retained, attached to later requests, or persisted in an audit event.
- `McpCallContext` contains a raw Authorization header, role hint, session ID, and request ID. It has no normalized, verified principal.
- `X-Mcp-Role`, `X-Mcp-Admin`, and `Bearer dev-admin` can currently grant administrative compatibility authority. They are not a production-safe identity system.

## Resolved Decisions

1. Meshingress will be an OAuth 2.1/OIDC resource server for production HTTP MCP, exposing protected-resource metadata and validating issuer, signature, expiry, and exact MCP-resource audience.
2. The first production token type is a signed JWT from a configured external issuer. Opaque token introspection, delegated OAuth storage, and workload identity remain explicit later adapters, not implicit fallbacks.
3. A normalized principal will contain issuer, subject, audience, expiry, authentication method, scopes, and mapped groups/roles. `clientInfo` is retained only as reported trace metadata and must never authorize a request.
4. Authorization remains application-native for the first release: a single decision service takes principal, action, resource, and allowlisted request context. It filters `tools/list` and independently checks `tools/call` immediately before execution.
5. Every backend binding must declare `none`, `secret-reference`, `header-passthrough`, `token-exchange`, `delegated-oauth`, or `workload-identity`. Caller-token forwarding has no default path.
6. Production rejects `X-Mcp-Admin`, `X-Mcp-Role`, `X-Auth-Token`, `X-Secret-Key`, and the shared `dev-admin` bearer shortcut. `Authorization: Bearer <access-token>`, `Mcp-Session-Id`, and `X-Request-Id` remain, with only the bearer token carrying caller identity.

## Client Trace Contract

An HTTP request without `Mcp-Session-Id` receives a generated session identifier. `initialize` records the bounded `clientInfo.name`, `clientInfo.version`, and requested protocol version against that session; each MCP request logs those values with its method, session, and request identifier. Values are length-bounded and control-character-normalized. No token, secret, tool arguments, or client-supplied role is logged.

Until the OIDC child entry is completed, this is explicitly `clientProfile=reported`, not proof of the human or workload behind the request. The OIDC work must add the verified principal identifier and issuer to the same audit event.
