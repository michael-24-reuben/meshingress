# Fixes

- Added the Aegis dependency to the server and tenant ownership to the reusable
  profile value model.
- Added durable JDBC profile storage with atomic revision replacement and
  tenant-safe route ownership checks.
- Added durable JDBC tool-policy storage, an evaluator shared by `tools/list`
  and `tools/call`, and structured security-management audit events.
- Added `security/whoami` plus every approved `roles/security/*` profile,
  binding, and tool-policy dispatch method. Raw credential material and the
  excluded `credential-bindings/verify` route are rejected/not exposed.
- Added optional OIDC resource-server configuration and verified claim mapping
  for roles, grants, tenant, profile ID, issuer, and expiry.
- Preserved the caller-safe MCP context boundary; no role header authority was
  restored.
