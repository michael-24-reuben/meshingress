# Google OIDC resource authentication

**Feature ID:** `feature-google-oidc-resource-authentication-v1`  
**Recorded:** 2026-08-15T19:45:33-04:00  
**Status:** Implemented

## Feature memo

When OIDC is enabled, Meshingress configures Spring Security as a JWT resource server. MCP routes require authentication when `meshingress.security.require-authentication` is enabled. The JWT decoder requires a configured issuer and audience, discovers signing keys from the issuer, and validates issuer, signature/standard claims, and audience.

The server also publishes RFC 9728 protected-resource metadata at `/.well-known/oauth-protected-resource/mcp`. The metadata reports the configured MCP resource URI and, when configured, the authorization server.

## Configuration and identity boundary

The `issuer-uri` and `audience` values bind through the canonical `MeshingressProperties.Security` constructor; the focused regression test covers this path. A verified OIDC token can later receive local profile roles and grants through the Aegis resolver, but token verification itself does not create a profile.

## Evidence snapshot

- `SecurityConfig.java` establishes MCP protection and issuer/audience validation.
- `McpOAuthResourceMetadata.java` builds the discovery values from public deployment configuration.
- `MeshingressOidcPropertiesBindingTest.java` covers the binding regression that previously made direct properties appear blank.

## Origin and currency

This is an implementation snapshot from source inspected and focused-tested on 2026-08-15. It is not a source of truth; verify the cited files and deployment properties before enabling OIDC in another environment.
