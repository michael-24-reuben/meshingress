# Context

## Existing Boundary

`SecurityConfig` validates Google JWT issuer, audience, signature, and expiry.
`DefaultMcpPrincipalResolver` then calls `AegisProfileIdentityResolver`, which
only reads active local profiles matching the verified JWT `issuer + subject`.
It does not create or update a profile.

Studio currently receives the Google ID token, keeps it in browser memory, and
sends it as a Bearer credential on later API/MCP requests. It has no profile
creation call and no persistent browser session.

## Existing Durable Model

`packages/aegis` provides the reusable profile lifecycle and authenticated
identity model. Security-management dispatch routes already own privileged
profile creation and policy assignment. This work must extend those server-side
boundaries rather than trust Studio-provided profile data.

## Identity Rule

For Google, the durable external-key tuple is:

```text
issuer = https://accounts.google.com
subject = JWT sub
```

Email, display name, and avatar are optional display metadata. They can change
and must never select a profile or confer authority.

## Related Records

- `architect/active/2026-07-16-mcp-oidc-principal-and-header-migration`
- `architect/resolved/2026-08-15-aegis-profile-lifecycle`
- `architect/active/2026-08-15-studio-login-interactions`
- `architect/pending/2026-07-16-mcp-tool-authorization-and-credential-strategies`
