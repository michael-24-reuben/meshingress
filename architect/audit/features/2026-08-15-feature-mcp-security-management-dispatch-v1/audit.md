# MCP security-management dispatch routes

**Feature ID:** `feature-mcp-security-management-dispatch-v1`  
**Recorded:** 2026-08-15T19:45:33-04:00  
**Status:** Implemented

## Feature memo

Meshingress has MCP dispatch endpoints for authenticated identity introspection and privileged security management:

- `security/whoami`
- `roles/security/profiles/*`, including status, identity, and credential lifecycle operations
- `roles/security/credential-bindings/list` and `get`
- `roles/security/tool-policies/*`, including evaluation

The route controllers are deliberately thin: they pass the validated MCP call context and request parameters into the security-management service. This keeps the public dispatch names stable while centralizing management behavior.

## Boundary

There is intentionally no `roles/security/credential-bindings/verify` route. Credential verification is not exposed as a generic management operation.

## Evidence snapshot

- `SecurityMcpController.java` maps caller introspection.
- `RolesSecurityMcpController.java` maps the privileged management family.
- `SecurityDispatchContractTest.java` verifies approved route registration and the verification-route exclusion.

## Origin and currency

This is an implementation snapshot from source inspected on 2026-08-15. It is not a source of truth; verify the cited files before depending on the route inventory or authorization details.
