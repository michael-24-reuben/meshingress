# Verification

## Passed

```powershell
$env:JAVA_HOME='C:\Users\jbeas\scoop\apps\temurin25-jdk\current'
.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=EmbeddedCycloneDxJarSbomGeneratorTests,ArtifactRepositoryFlowTests,ArtifactRepositoryScannerPipelineTests,PowerShellCliArtifactRepositoryUploadInstanceTests,ArtifactRepositoryPublicationEligibilityTests,ArtifactRepositoryMissingScannerEligibilityTests,ArtifactRepositoryUntrustedBuilderEligibilityTests,ArtifactRepositorySigningKeyEligibilityTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result: pass; 12 tests, 0 failures/errors/skips; `BUILD SUCCESS`.

```powershell
$env:JAVA_HOME='C:\Users\jbeas\scoop\apps\temurin25-jdk\current'
.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=McpPublicationInstallTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result: pass; 13 tests, 0 failures/errors/skips; `BUILD SUCCESS`.

## Coverage Added

- Missing required scanner evidence denial.
- Insufficient reviewer approval denial.
- Untrusted builder denial.
- Unknown and revoked signing key denial.
- Happy-path accepted eligibility evidence persisted with signed publication records.
- Runtime publication install signature verification compatibility.

## Remaining Risks

- Broad worktree dirt remains from prior PAS runs and unrelated tool work.
- Multi-review semantics are still lifecycle-event-count based; enabling reviewer counts above 1 in normal flow should wait for a richer review identity model.
- External scanner CLIs, dependency vulnerability feeds, repository UI, and broader runtime install expansion remain separate follow-up objectives.
