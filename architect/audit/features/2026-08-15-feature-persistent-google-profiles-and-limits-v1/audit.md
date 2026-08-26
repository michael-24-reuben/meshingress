# Persistent Google profiles and configurable limits

**Feature ID:** `feature-persistent-google-profiles-and-limits-v1`  
**Recorded:** 2026-08-15T20:14:42-04:00  
**Status:** Implemented

## Feature memo

Google-authenticated users are matched to durable Aegis profiles strictly through a provider-neutral `VerifiedIdentity` derived from Spring Security's verified JWT issuer (`iss`) and subject (`sub`). The browser or client cannot supply a profile ID, role, tenant, or limit quota to claim authority.

Profile admission is closed by default. When self-service admission is explicitly enabled, unmatched verified identities receive an unprivileged profile initialized to `PENDING_REVIEW` with no elevated roles or grants. Request-time rate, tool-execution, and concurrency limits are evaluated per profile with independent global deployment and per-profile enforcement switches. Over-limit requests are denied via standard JSON-RPC `RATE_LIMITED` error codes with a stable `PROFILE_LIMIT_EXCEEDED` reason.

## Identity and admission boundary

1. Token signature, issuer, audience, and subject are validated by Spring Security before identity resolution.
2. `AegisProfileIdentityResolver` looks up matching active profiles via `JdbcProfileStore.findByIdentity(issuer, subject)`.
3. `ProfileAdmissionService` controls new profile provisioning:
   - `CLOSED` mode (default): Unmatched identities authenticate but receive no local profile or privileges.
   - `SELF_SERVICE_UNPRIVILEGED`: Provisions a minimal profile with `PENDING_REVIEW` status and zero initial roles.
4. HTTP endpoints `/api/v1/auth/profile/admit` and `/api/v1/auth/profile/me` expose identity admission and introspection.

## Resource limits and quota enforcement

`ProfileLimitService` manages request and tool execution quotas:
- Evaluates deployment-wide (`meshingress.security.limits.enabled`) and per-profile limit switches (`ResourceLimitPolicy.enforcement`).
- Reserves rolling window requests, tool execution counts, and concurrent in-flight calls.
- Denies requests exceeding thresholds with `RATE_LIMITED` (`-32005`) and logs audit events to `security_profile_limit_events`.
- Exposes security management dispatch methods for limit retrieval, updates, and usage auditing.

## Evidence snapshot

- `VerifiedIdentity.java` encapsulates verified external identity details without storing tokens or secrets.
- `AegisProfileIdentityResolver.java` enforces exact issuer/subject profile matching.
- `ProfileAdmissionService.java` manages server-owned admission rules and unprivileged provisioning.
- `ProfileAdmissionController.java` provides verified identity HTTP introspection boundaries.
- `ProfileLimitService.java` handles rate/concurrency reservation and limit denial auditing.
- `ProfileAdmissionServiceTest.java` and `ProfileLimitServiceTest.java` cover closed mode, self-service provisioning, limit enforcement, and global limit switches.
- `architect/resolved/2026-08-15-persistent-google-profiles-and-limits/prd.md` documents the authoritative PRD requirements.

## Origin and currency

This feature memo is an audit snapshot recorded on 2026-08-15 following the resolution of architect entry `2026-08-15-persistent-google-profiles-and-limits`. Referenced source files and tests are authoritative.
