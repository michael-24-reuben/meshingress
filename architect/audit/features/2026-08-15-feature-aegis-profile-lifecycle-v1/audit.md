# Aegis profile lifecycle

**Feature ID:** `feature-aegis-profile-lifecycle-v1`  
**Recorded:** 2026-08-15T19:45:33-04:00  
**Status:** Implemented

## Feature memo

`packages/aegis` is a reusable Java module for an authorization profile, its authenticated identities, and secret references rather than raw secrets. It supports create, get, list, metadata update, activate, disable, revoke, identity link/unlink, and credential bind/replace/revoke/unbind operations.

Mutations use an expected revision. Revocation is terminal, revokes associated credential bindings, and prevents later reactivation. An identity cannot be removed when it would leave no identity or when credential bindings still refer to it.

## Integration boundary

The server-side resolver consults active, persisted profiles only after an external JWT has already been verified. It matches exact issuer and subject and returns local roles, grants, profile ID, and tenant ID only for one active match.

This does **not** currently auto-create a local profile from a Google credential. Persistent Google-user admission/provisioning and configurable quota enforcement remain separate follow-up work.

## Evidence snapshot

- `ProfileService.java` is the framework-free lifecycle implementation.
- `ProfileServiceTest.java` exercises concurrency, lifecycle, integrity, and secret-safety boundaries.
- `AegisProfileIdentityResolver.java` is the server integration point for verified identities.

## Origin and currency

This is an implementation snapshot from source inspected on 2026-08-15. It is not a source of truth; verify the cited files before relying on its API or persistence behavior.
