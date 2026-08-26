# Assessment

The previous MCP security bridge treated caller-controlled `X-Mcp-Role` and
`X-Mcp-Admin` headers as administrator authority and exposed raw authorization
evidence through the public tool API. `tools/list` also bypassed the execution
policy. Those were development-stage placeholders, not an acceptable security
boundary.

This implementation removes that trust path. A server-only evidence record is
verified into `McpPrincipal`, then a single context factory creates typed,
credential-safe `McpCallContext` instances. The only retained development
adapter compares a configured bearer token in `dev` mode and produces a normal
principal; it cannot consume role headers and is disabled by default in the
type's defaults.

OIDC/JWT verification, Aegis persistence, and backend credential selection are
not part of this resolved slice. Their implementation records remain pending.
