# Implementation

- Added framework-free bootstrap value types, lifecycle states, stable denial
  reasons, verifier/predicate/store SPIs, and `BootstrapEnrollmentService`.
- Added explicit `issue`, `rotate`, and `revoke` operations. Construction and
  restart do not issue credentials.
- Added `InMemoryBootstrapEnrollmentStore`, whose synchronized enrollment
  transaction admits one first administrator and consumes its issuance.
- Added `BootstrapEnrollmentProperties` and module defaults in
  `packages/aegis/src/main/resources/application.properties`:
  `enabled=false`, action, TTL, attempt limit, and administrator role.
- Added property and lifecycle tests, and documented the host responsibilities
  in `packages/aegis/README.md`.

No Meshingress server, Spring Security, MCP endpoint, OIDC adapter, concrete
secret store, or durable persistence adapter was wired or changed.
