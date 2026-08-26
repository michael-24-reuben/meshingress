# Verification

## Automated Tests

- `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=ArtifactRepositoryFlowTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Result: pass.
  - Evidence: `ArtifactRepositoryFlowTests` ran 6 tests with 0 failures, 0 errors, and 0 skips.

## Coverage Notes

- The focused flow verifies real upload, assessment, approval, publication, persisted assessment evidence, and artifact-layout expectations.
- Role-gate tests cover unauthorized approve, reject, publish, revoke, delete, and restore attempts.
- Stage-order tests cover approval before assessment, rejection before assessment, publication before approval, revocation before publication, restore before delete, and deletion of an active non-revoked publication.
- Lifecycle audit assertions cover actor, request id, timestamp presence, `from_state`, and `to_state` for approval, rejection, revocation, deletion, and restore.

## Remaining Risks

- Header-based roles are an API foundation, not a complete authentication system.
- Delete is intentionally soft/state-only; physical retention and cleanup policy are not implemented in this slice.
- Publication signing and provenance remain MVP HMAC-based and are tracked by the next architect entry.
- External scanner sandboxing and runtime install hardening remain separate deferred entries.
