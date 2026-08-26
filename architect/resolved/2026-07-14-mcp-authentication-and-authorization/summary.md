# Summary

The MCP authentication and authorization architecture is resolved as a design decision. Meshingress will use standard OAuth 2.1/OIDC resource-server authentication, audience-bound bearer tokens, application-native per-tool authorization, and explicit backend credential strategies. Header-derived privilege remains development-only migration behavior and will be removed by the incoming-identity child entry.

The immediate traceability gap is also closed: MCP initialization now captures bounded reported client metadata and associates later request audit logs through `Mcp-Session-Id`. This is intentionally not represented as a verified caller until the OIDC principal is available.

Follow the two pending child entries before enabling production authentication.
