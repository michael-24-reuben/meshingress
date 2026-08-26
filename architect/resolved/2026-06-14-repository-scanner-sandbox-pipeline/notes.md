# Notes

## 2026-06-24

- Activated from `pending/` according to PAS.
- Current assessment flow already persists `cyclonedx-sbom` and `bytecode-scope-scanner` results beside the artifact.
- The first implementation slice keeps the scanner execution in-process and embedded/library-oriented, but makes required/optional pipeline metadata explicit.
- Existing `BLOCKED_POLICY` status and lifecycle gates are enough to prevent approval/publication when a required scanner reports `BLOCKED`; the new test locks that behavior.

## 2026-06-25

- Extended the embedded `CycloneDxSbom` model and `EmbeddedCycloneDxJarSbomGenerator` to keep deterministic JAR-entry inventory while also reading packaged Maven `pom.properties` and `pom.xml` metadata.
- The raw CycloneDX document now includes Maven coordinate fields (`group`, `version`, `purl`) for the root artifact when available, plus `library` components for declared Maven dependencies with scope metadata.
- `CycloneDxSbom.summary()` now includes `dependencyComponentCount` so repository assessment summaries can expose whether dependency metadata was discovered without replacing the raw `cyclonedx-sbom.json` report.
- Updated `ArtifactRepositoryFlowTests` only where needed to prove repository assessment storage preserves dependency-aware SBOM data in both the assessment summary and raw on-disk `cyclonedx-sbom.json`.
- Verification passed:
  - `.\mvnw.cmd -pl lib\meshingress-artifact-scope-scanner -am "-Dtest=EmbeddedCycloneDxJarSbomGeneratorTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=ArtifactRepositoryFlowTests#uploadAssessApproveAndPublishArtifactWithRealAssessmentScanners" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=EmbeddedCycloneDxJarSbomGeneratorTests,ArtifactRepositoryFlowTests,ArtifactRepositoryScannerPipelineTests,PowerShellCliArtifactRepositoryUploadInstanceTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Remaining objective work is still active: raw report retention policy, first malware/sandbox strategy, sandbox isolation guarantees, and raw sandbox report storage.

## 2026-06-25 Retention and Sandbox Slice

- Added explicit repository raw-report retention configuration with default policy `retain-with-artifact`, location `artifact-assessment-directory`, and separate scanner/sandbox retention switches.
- Added `embedded-jar-sandbox` as a required scanner pipeline stage for repository assessment defaults.
- Implemented the first embedded/library-oriented JAR sandbox strategy in `ArtifactService`: static quarantine-entry inspection only, with no artifact execution, no class loading, no process launch, no network access, no host-secret access, and no repository-internals access.
- The sandbox scanner records executable payloads for reviewer attention and writes `embedded-jar-sandbox.json` beside `assessment.json` and `cyclonedx-sbom.json` under the artifact assessment storage directory.
- Repository assessment summaries now expose both `rawReportRetention` and sandbox summary metadata, including the isolation guarantees and raw sandbox report name.
- Updated repository tests to prove:
  - `embedded-jar-sandbox` is included in the configured required scanner pipeline.
  - raw SBOM and raw sandbox reports are retained beside the artifact.
  - sandbox summaries record no-network and no-host-secret access.
  - SQL assessment persistence now carries the three scanner results.
- Verification passed:
  - `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=ArtifactRepositoryFlowTests,ArtifactRepositoryScannerPipelineTests,PowerShellCliArtifactRepositoryUploadInstanceTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=EmbeddedCycloneDxJarSbomGeneratorTests,ArtifactRepositoryFlowTests,ArtifactRepositoryScannerPipelineTests,PowerShellCliArtifactRepositoryUploadInstanceTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
