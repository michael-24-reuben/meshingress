# Notes

## 2026-06-25

- Activated after resolving `2026-06-14-repository-scanner-sandbox-pipeline`.
- Scanner evidence now available for policy use:
  - required pipeline stages: `cyclonedx-sbom`, `embedded-jar-sandbox`, `bytecode-scope-scanner`
  - retained raw reports: `assessment.json`, `cyclonedx-sbom.json`, `embedded-jar-sandbox.json`
  - sandbox isolation metadata records no execution, no class loading, no process launch, no network access, no host-secret access, and no repository-internals access
- Next implementation should start with deterministic eligibility decision shape and reason codes, then add the smallest rejection tests for missing scanner/sandbox evidence and insufficient review.

## 2026-06-26

- Implemented the first publication eligibility slice:
  - added `PublicationEligibilityDecision` to signed publication records;
  - added repository `publication-eligibility` configuration;
  - evaluated required scanner evidence from persisted assessment results;
  - evaluated configured approval count from lifecycle events;
  - preserved approved/denied/inferred scope separation and did not treat requested scopes as authority;
  - evaluated required provenance fields, trusted-builder allowlist shape, and nonblank signing key metadata;
  - persisted accepted eligibility evidence with the publication record.
- Added focused rejection tests for:
  - `MISSING_REQUIRED_SCANNER:<scanner>`;
  - `INSUFFICIENT_REVIEW_APPROVALS`.
- Updated runtime publication signature verification to include the new eligibility decision in current signed payloads while keeping the legacy unsigned payload path for old blank-key HMAC records.
- Verification passed:
  - `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=EmbeddedCycloneDxJarSbomGeneratorTests,ArtifactRepositoryFlowTests,ArtifactRepositoryScannerPipelineTests,PowerShellCliArtifactRepositoryUploadInstanceTests,ArtifactRepositoryPublicationEligibilityTests,ArtifactRepositoryMissingScannerEligibilityTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=McpPublicationInstallTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Remaining work:
  - add untrusted-builder rejection coverage;
  - decide and implement repository-side non-revoked signing key policy instead of only checking nonblank signer metadata;
  - decide whether `minimumReviewerApprovals > 1` requires a new multi-review lifecycle model before enabling it outside tests.

## 2026-06-26 Final Slice

- Added repository-side signing-key eligibility policy lists:
  - `meshingress.repository.publication-eligibility.trusted-signing-key-ids`
  - `meshingress.repository.publication-eligibility.revoked-signing-key-ids`
- Publication now denies:
  - `SIGNING_KEY_UNKNOWN:<keyId>` when a trusted key allowlist is configured and the active signer is absent;
  - `SIGNING_KEY_REVOKED:<keyId>` when the active signer is explicitly revoked.
- Added focused rejection coverage for:
  - `UNTRUSTED_BUILDER:meshingress-repository`
  - `SIGNING_KEY_UNKNOWN:local-dev-hmac`
  - `SIGNING_KEY_REVOKED:local-dev-hmac`
- Verification passed:
  - `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=EmbeddedCycloneDxJarSbomGeneratorTests,ArtifactRepositoryFlowTests,ArtifactRepositoryScannerPipelineTests,PowerShellCliArtifactRepositoryUploadInstanceTests,ArtifactRepositoryPublicationEligibilityTests,ArtifactRepositoryMissingScannerEligibilityTests,ArtifactRepositoryUntrustedBuilderEligibilityTests,ArtifactRepositorySigningKeyEligibilityTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=McpPublicationInstallTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- The active checklist is complete. External scanner CLI integrations, richer multi-review workflow, dependency vulnerability feeds, and repository UI remain deferred follow-up scope and should use focused entries before implementation.
