# Verification

## Commands

- `.\mvnw.cmd -pl lib\meshingress-artifact-scope-scanner -am "-Dtest=EmbeddedCycloneDxJarSbomGeneratorTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Result: pass; SBOM generator focused verification passed.
- `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=ArtifactRepositoryFlowTests#uploadAssessApproveAndPublishArtifactWithRealAssessmentScanners" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Result: pass; repository flow retained dependency-aware SBOM evidence.
- `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=ArtifactRepositoryFlowTests,ArtifactRepositoryScannerPipelineTests,PowerShellCliArtifactRepositoryUploadInstanceTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Result: pass; 8 repository tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=EmbeddedCycloneDxJarSbomGeneratorTests,ArtifactRepositoryFlowTests,ArtifactRepositoryScannerPipelineTests,PowerShellCliArtifactRepositoryUploadInstanceTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Result: pass; 2 SBOM generator tests plus 8 repository tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- `git diff --check -- app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/ArtifactService.java app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/config/MeshingressRepositoryProperties.java app/meshingress-repository/src/main/resources/application.properties app/meshingress-repository/src/test/java/dev/mrk/meshingress/repository/artifact/ArtifactRepositoryFlowTests.java app/meshingress-repository/src/test/java/dev/mrk/meshingress/repository/artifact/ArtifactRepositoryScannerPipelineTests.java app/meshingress-repository/src/test/java/dev/mrk/meshingress/repository/artifact/PowerShellCliArtifactRepositoryUploadInstanceTests.java`
  - Result: pass; no whitespace errors; Git reported existing LF-to-CRLF working-copy warnings.

## Final Rerun

- `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=EmbeddedCycloneDxJarSbomGeneratorTests,ArtifactRepositoryFlowTests,ArtifactRepositoryScannerPipelineTests,PowerShellCliArtifactRepositoryUploadInstanceTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Result: pass on 2026-06-25T11:01:45-04:00; SBOM generator tests ran 2 tests with 0 failures/errors/skips, repository tests ran 8 tests with 0 failures/errors/skips, and the reactor ended with `BUILD SUCCESS`.

## Coverage

- Dependency-aware SBOM metadata is present in raw `cyclonedx-sbom.json` and compact assessment summaries.
- Required scanner pipeline metadata includes `cyclonedx-sbom`, `embedded-jar-sandbox`, and `bytecode-scope-scanner`.
- Raw scanner and sandbox reports are retained with the artifact assessment directory.
- Static sandbox summaries record no execution, no class loading, no process launch, no network access, no host-secret access, and no repository-internals access.
- Required blocked scanner results prevent approval or publication.

## Remaining Risks

- The sandbox strategy is static inspection, not dynamic malware execution.
- External scanner CLI integration and dependency vulnerability feeds remain deferred.
- Publication eligibility policy still needs to consume the retained scanner, sandbox, scope, review, provenance, builder, and signing-key evidence.
- The worktree contains broad prior dirty/untracked changes that were preserved.
