# Implementation Plan

## 1. Establish durable policy contracts

- Extend the Aegis profile contract with a versioned `ResourceLimitPolicy` and
  usage-meter contract, keeping it portable and Spring-free.
- Define explicit inheritance and unlimited-value semantics.
- Add profile lifecycle states needed for safe admission if they are not already
  present.

## 2. Add server-owned profile admission

- Create a non-MCP HTTPS profile/session admission endpoint or protected
  request-time service that accepts only a Bearer token and derives all identity
  input from the authenticated Spring principal.
- Add deployment-owned admission configuration; default it to `CLOSED`.
- Keep privileged profile creation and grants in security-management routes.
- Make repeated admissions idempotent for the same verified issuer/subject.

## 3. Persist and resolve profile data

- Add schema migration for profile limits, identity uniqueness, usage records,
  and admission audit events.
- Enforce unique issuer/subject binding transactionally.
- Refresh non-authoritative display metadata only after successful token
  verification.

## 4. Enforce request limits

- Add a request-time limiter before `tools/list` and `tools/call`; reserve
  concurrency before execution and settle usage afterward.
- Apply the policy hierarchy and the two enforcement switches.
- Ensure unavailable meters fail closed or open only through an explicit,
  audited deployment policy.

## 5. Provide management and observability

- Add secure management methods for viewing/updating profile limits and
  reviewing usage/audit history.
- Return retry/limit metadata without exposing another profile's data.
- Keep admin-only changes in existing `roles/security/*` boundaries.

## 6. Verify security properties

- Unit-test JWT-derived identity binding, duplicate rejection, inheritance, and
  switches.
- Integration-test authorization plus rate/concurrency/quota denial paths.
- Test spoofed profile IDs, spoofed emails, changed email for same `sub`,
  expired/wrong-audience tokens, suspended profiles, and concurrent requests.

## Design Decisions Required Before Activation

1. Which admission modes are permitted for Meshingress deployments beyond the
   safe default `CLOSED`?
2. Should self-service profiles become `PENDING_REVIEW` or immediately active
   with a zero-privilege baseline?
3. Which storage/billing dimensions require hard quotas in the first release?
4. Should usage metering continue while enforcement is globally disabled?
