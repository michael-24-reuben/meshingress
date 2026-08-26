# Context

## Current Review Findings

`McpCallContext` in `lib/meshingress-tool-api` currently exposes an
`authorizationHeader`, a caller-controlled `roleHeader`, session ID, request
ID, and a progress reporter. HTTP, MCP WebSocket, workflow HTTP, and workflow
WebSocket routes directly construct this public context from raw headers or
WebSocket attributes.

`McpAccessPolicyService` currently treats `X-Mcp-Role: admin` and the legacy
`X-Mcp-Admin: true` as administrator authority. When authentication is
configured as required, it checks only that an authorization header is
present. `SecurityConfig` permits all requests. This is an intentionally
development-stage placeholder and must not be preserved as a production
identity boundary.

`tools/list` has no `McpCallContext` parameter and reads public enabled
functions directly from the registry. `tools/call` passes the context to
`DefaultToolExecutor`, which checks enabled registry state and server-wide
scope configuration but has no principal-aware authorization decision.

The annotation scanner evaluates current availability policy at scan time with
no arguments or caller identity, and exports availability metadata in function
annotations. Argument-dependent availability cannot safely decide ordinary
`tools/list` visibility because list requests have no tool arguments; it must
be enforced at call time.

`McpClientTraceService` separately retains declared `initialize.clientInfo`
per session, but that unverified client metadata is not in the call context.
WebSocket messages reuse handshake request correlation, and workflow tool calls
replace the parent request ID with a run/node/attempt string while copying raw
authorization and role values.

## Boundaries

- Raw authorization evidence, cookies, client certificates, and legacy role
  headers stay inside a server-side transport/authentication boundary. They
  must not be public `McpCallContext` fields or tool-handler inputs.
- The tool API must not depend directly on `packages/aegis`; an optional host
  adapter maps Aegis or OIDC verification output into the MCP principal shape.
- Declared client metadata is traceability-only and is never authorization
  input.
- Existing role administration remains a separately authorized administrator
  surface; it may observe disabled tools but cannot execute them through an
  administrative listing response.
- This record does not change the active Nextcloud objective or authorize
  application deployment, token issuance, secret storage, or OIDC provider
  configuration.
