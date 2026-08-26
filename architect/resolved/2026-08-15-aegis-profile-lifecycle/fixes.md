# Fixes

## Aegis API

- Added `ProfileStore`, `ProfileSnapshot`, `ProfileQuery`, and explicit
  not-found/revision-conflict errors.
- Added `InMemoryProfileStore` as the synchronized local-test reference.
- Added `ProfileDraft` and `ProfileService` with create, read, bounded list,
  metadata, profile-status, identity, and credential-binding lifecycle methods.
- Added revision-aware mutation semantics and terminal profile revocation.
- Revoking a profile marks all retained bindings revoked; unlinking an identity
  is rejected while any binding references it.
- Detaching a binding only removes profile metadata and never performs an
  external secret-store operation.

## Documentation and routes

- Expanded the Aegis README with the reusable lifecycle contract and its host
  boundaries.
- Recorded the exact future privileged MCP dispatch names in `plan.md` without
  installing application routes or Spring wiring.
