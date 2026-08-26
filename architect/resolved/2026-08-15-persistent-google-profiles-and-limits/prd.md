# Product Requirements

## Goals

1. Persist a profile's local authorization and resource policy across Google
   logins.
2. Ensure identity-to-profile selection occurs only after the server verifies a
   Google JWT.
3. Make self-provisioning a deliberate deployment policy, disabled by default.
4. Enforce configurable request-rate, concurrency, execution, and usage quotas
   at request time.
5. Preserve an auditable decision trail for profile admission, profile changes,
   limit denials, and administrative overrides.

## Required Identity and Admission Rules

- Validate token signature, exact issuer, audience, expiry, and subject on the
  server before profile lookup or creation.
- Derive the identity key exclusively from verified JWT `iss` and `sub`.
- Ignore client-provided profile ID, tenant, roles, grants, status, quota, and
  display claims for authorization decisions.
- Reject ambiguous identity bindings: an issuer/subject may bind to exactly one
  active profile globally within its tenant model.
- Lookup only matching active profiles; suspended, revoked, and pending
  profiles must not receive their prior grants.
- The default admission mode is `CLOSED`: an unmatched identity is
  authenticated but not locally provisioned and receives no local authority.
- Supported future admission modes must be explicit configuration: `CLOSED`,
  `INVITE_ONLY`, `SUBJECT_ALLOWLIST`, and `SELF_SERVICE_UNPRIVILEGED`.
- Self-service provisioning, if enabled later, creates only a minimal profile
  with no privileged role, no privileged scope, and a configurable initial
  status such as `PENDING_REVIEW`. It cannot assign a tenant, administrator
  role, or elevated quota from browser input.

## Persistent Profile Data

Store server-owned data in Aegis/JDBC persistence:

- immutable profile ID and tenant ID;
- lifecycle status, created/updated/last-authenticated timestamps;
- verified external identity bindings (`issuer`, `subject`);
- server-assigned roles, grants/scopes, tool-policy bindings, and approval
  settings;
- optional display metadata (name/email) marked as non-authoritative and
  refreshed only from a verified token;
- resource-limit policy and usage counters; and
- audit records for admission, binding changes, policy changes, limit denials,
  and administrative overrides.

Never persist a Google ID token, refresh token, client secret, raw credential,
or a browser-provided authorization claim as profile authority.

## Limits and Quotas

The durable profile policy must support, at minimum:

- requests per rolling window;
- tool executions per rolling window;
- concurrent in-flight calls;
- per-tool and global execution budgets;
- workspace/storage bytes and published-byte allowance where applicable; and
- optional daily/monthly consumption ceilings.

Enforcement must have two independent switches:

```text
meshingress.security.limits.enabled       # deployment-wide enforcement switch
profile.limitPolicy.enabled               # per-profile enforcement switch
```

When the deployment switch is off, limits are not denied but usage may still be
metered if `recordUsage` is enabled. Disabling limits must never bypass identity
verification, tool authorization, scope checks, or approvals.

Use a deterministic default-policy hierarchy:

```text
global default -> tenant default -> profile override -> tool-specific override
```

An override may only narrow a caller's authority unless applied by an authorized
security manager. Missing policy values inherit rather than silently mean
unlimited.

## Request Flow

```text
Google ID token
  -> HTTPS Bearer request
  -> server JWT verification
  -> issuer + subject identity lookup
  -> active profile match or configured admission decision
  -> local roles/grants/tool policy evaluation
  -> rate/concurrency/quota reservation
  -> tool list/call decision
  -> usage settlement and audit
```

For unknown identities, apply a bounded anonymous/pre-provisioning limiter
keyed by a privacy-preserving server-side identifier. Do not use a supplied
profile ID as a limiter key.

## Non-Goals

- Google refresh-token storage or access to Google APIs on a user's behalf.
- Automatically granting administrators or privileged roles to Gmail accounts.
- Trusting email as the durable identity key.
- Making mutable infrastructure settings live-editable without an explicit
  runtime-policy design.

## Acceptance Criteria

- A valid token for an existing issuer/subject receives exactly its saved local
  profile policy after a later login.
- A valid token for a different subject cannot select, update, or consume the
  original profile's authority or quota.
- Forged client profile/role/quota fields have no effect.
- Duplicate identity binding is rejected and audited.
- Global and profile limit switches are independently tested.
- Limit-denied requests return a stable machine-readable reason and are audited
  without logging a credential.
