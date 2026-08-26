# Context

## Design Reference

This record borrows the design pattern, not implementation, from ToolHive Virtual MCP Server:

- one incoming identity boundary: client authenticates to the MCP gateway;
- a distinct outgoing credential boundary: the gateway selects credentials per backend;
- authorization decisions use principal, action, resource, and selected request context;
- discovery must not reveal tools that the caller would be unable to invoke.

Primary references consulted on 2026-07-14:

- [ToolHive authentication and authorization](https://docs.stacklok.com/toolhive/guides-vmcp/authentication)
- [ToolHive authorization policy reference](https://docs.stacklok.com/toolhive/reference/authz-policy-reference)

## Current Meshingress Baseline

- `security/SecurityConfig.java` currently permits all HTTP requests and disables CSRF; it is not a production authentication boundary.
- `security/McpAccessPolicyService.java` can require a nonblank Authorization header, but treats `X-Mcp-Role: admin` (or the legacy boolean header) as admin and otherwise accepts a configurable shared bearer token with a `dev-admin` fallback.
- `mcp/McpController.java` puts the caller Authorization header, role hint, session ID, and request ID into `McpCallContext`.
- Scope and secret configuration exists, but it is not a substitute for caller identity, per-tool authorization, or an outbound credential model.

## Why Separate the Boundaries

A client token answers who may use Meshingress. It should not automatically become the credential for every downstream service. A tool may be unauthenticated, use a shared service key, act with delegated user authority, or need a narrower exchanged credential. Treating every backend as passthrough risks a confused-deputy problem and gives the tool the caller's full downstream reach.

## Usability Principle

Interactive authentication belongs at connection/consent time and token renewal time, not at every tool call. Per-call work should be token validation plus a local authorization decision; backend credentials should be acquired or selected by their configured strategy without presenting secret values to the client.
