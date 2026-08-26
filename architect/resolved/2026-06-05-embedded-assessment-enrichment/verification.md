# Verification

## Commands

- `.\mvnw.cmd -pl lib/meshingress-artifact-scope-scanner test`
  - Result: passed; 4 tests, 0 failures, 0 errors, 0 skipped.
- `.\mvnw.cmd -pl app/meshingress-repository -am test`
  - Result: passed; scanner module tests and `ArtifactRepositoryFlowTests` passed, 0 failures, 0 errors, 0 skipped.
- `.\mvnw.cmd -pl app/meshingress-server -am "-Dtest=*Smoke*" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Result: passed; broad server smoke passed with 2 tests, 0 failures, 0 errors, 0 skipped.

## Coverage

- Deterministic SBOM generation for a representative JAR fixture.
- Root artifact and JAR entry SHA-256 hash generation.
- Stable component ordering and compact SBOM summary fields.
- Repository assessment scanner result inclusion.
- Raw CycloneDX report file storage.
- Existing bytecode scope inference still produces inferred scopes independently of uploaded requested scope claims.
- Server smoke baseline remained clean after the repository and scanner changes.

## Remaining Risks

- The SBOM generator is intentionally minimal and inventories JAR entries; it does not yet infer dependency coordinates from Maven metadata.
- External scanner CLI integrations remain deferred.
- Direct registration hardening remains a separate follow-up objective.
