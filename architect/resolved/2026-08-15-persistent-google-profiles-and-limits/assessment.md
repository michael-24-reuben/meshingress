# Assessment

## Decision

Persistent identity and resource policy are now provider-neutral Aegis
capabilities. An authentication provider must first produce a credential-safe
`VerifiedIdentity`; the server alone maps that verified issuer and subject to a
profile.

## Security Boundary

- Browser-provided profile IDs, roles, grants, tenants, and quotas are ignored.
- A JDBC uniqueness index enforces one external `issuer + subject` binding per
  profile identity.
- An unmatched OIDC identity receives no local Aegis authority.
- Admission defaults to `CLOSED`. The optional self-service mode creates a
  zero-role, zero-grant `PENDING_REVIEW` profile only after JWT verification.

## Limit Scope

Request rate, tool-execution rate, and concurrent calls are enforced before
tool listing/calling. Global configuration and a profile policy have separate
enable controls. Usage keys are derived from a profile ID or a SHA-256 digest
of the verified external identity; raw credentials are never stored or logged.

Storage bytes remain a policy field but are not enforced yet: current storage
lifecycle records do not carry the calling profile identity. That attribution
should be implemented as a separate storage-boundary change rather than guessed
from tool arguments.
