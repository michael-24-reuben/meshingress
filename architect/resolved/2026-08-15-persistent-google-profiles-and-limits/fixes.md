# Fixes

## Aegis

- Added provider-neutral `VerifiedIdentity`.
- Added versioned `ResourceLimitPolicy` with explicit inherit, unlimited, and
  limited values.
- Added `PENDING_REVIEW` lifecycle state and profile limit-policy updates.
- Extended the packaged profile JSON Schema and Aegis documentation.

## Server

- Added an issuer/subject identity index to `JdbcProfileStore` and used it for
  exact local-profile resolution.
- Added server-owned profile admission plus `POST /api/v1/auth/profile/admit`
  and `GET /api/v1/auth/profile/me`; both accept only Spring's verified JWT.
- Added closed-by-default admission and configurable deployment-wide limits to
  `MeshingressProperties` and `application.properties`.
- Added request, tool-execution, and concurrency reservations to `tools/list`
  and `tools/call`; denials use JSON-RPC `RATE_LIMITED` with a stable
  `PROFILE_LIMIT_EXCEEDED` reason.
- Added security-management dispatch methods:
  `roles/security/profiles/limits/get`,
  `roles/security/profiles/limits/update`, and
  `roles/security/profiles/usage/list`.

## Provider Reuse

Google currently supplies `VerifiedIdentity` through the verified JWT adapter.
Native and future providers can use the same admission service once they have
completed their own server-side credential verification.
