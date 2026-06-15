# Notes

## 2026-06-05

- Activated chapter 2 after the user confirmed chapter 1 is complete and asked to move on to 2.
- Created this objective as an active entry because implementation should begin from the chapter 2 assignment, not from the resolved chapter 1 record.
- Initial safe next action is no-code inspection of artifact model and repository assessment fields before adding CycloneDX dependencies or changing assessment output.

## 2026-06-06

- Inspected `ArtifactAssessmentSummary`, `ArtifactRecord`, `ArtifactPublicationRecord`, `ScannerResult`, `ScannerAdapter`, and `ArtifactService`.
- `ArtifactRecord` already carries `assessment`, and `ArtifactPublicationRecord` already embeds `scanSummary`, so the first SBOM slice does not need a new publication record field.
- `ArtifactService.assess` already stores full scanner results in `assessment.json`, then builds a compact `ArtifactAssessmentSummary` that is written into artifact metadata and later into publication records.
- Decision: add CycloneDX as an assessment scanner result named `cyclonedx-sbom`, persist raw SBOM JSON beside `assessment.json`, and include compact SBOM metadata in `ArtifactAssessmentSummary.summary`.
- The next implementation step can add a deterministic embedded CycloneDX generator and test before wiring it into repository assessment.

## 2026-06-06 Verification

- Ran `.\mvnw.cmd -pl lib/meshingress-artifact-scope-scanner test`: passed, 3 tests, 0 failures/errors/skips.
- Ran `.\mvnw.cmd -pl app/meshingress-repository -am test`: passed, including `ArtifactRepositoryFlowTests`, 1 repository flow test plus scanner upstream tests, 0 failures/errors/skips.
- Ran broad smoke command `.\mvnw.cmd -pl app/meshingress-server -am "-Dtest=*Smoke*" "-Dsurefire.failIfNoSpecifiedTests=false" test`: failed in `ToolRuntimeLoaderSmokeTestResults.loadsSampleJarThroughEveryRuntimeLoaderEntryPoint` with `IllegalStateException: Duplicate MCP function descriptor name: helloworld.text`.
- Ran focused runtime smoke command `.\mvnw.cmd -pl app/meshingress-server -am "-Dtest=ToolRuntimeLoaderSmokeTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: passed, 1 test, 0 failures/errors/skips.
- The broad smoke failure appears unrelated to the SBOM storage-shape decision, but it is a failing baseline that should be handled before claiming the repo has clean smoke coverage.

## 2026-06-07 Smoke Baseline

- Created `architect/active/2026-06-07-runtime-smoke-duplicate-tool` for the broad smoke failure, then resolved it after verification.
- Root cause: `ToolRuntimeLoaderSmokeTestResults` used the real Spring registry while startup scanning was enabled, so runtime activation of the sample JAR collided with an already registered `helloworld.text` function.
- Fix: set `meshingress.tools.registry.scan-on-startup=false` for `ToolRuntimeLoaderSmokeTestResults`, preserving production duplicate detection while giving the runtime-install smoke test an empty registry baseline.
- Ran `.\mvnw.cmd -pl app/meshingress-server -am "-Dtest=ToolRuntimeLoaderSmokeTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: passed, 1 test, 0 failures/errors/skips.
- Ran `.\mvnw.cmd -pl app/meshingress-server -am "-Dtest=*Smoke*" "-Dsurefire.failIfNoSpecifiedTests=false" test`: passed, 2 tests, 0 failures/errors/skips.
- Next action returns to the embedded CycloneDX generator interface and deterministic representative-JAR test in `lib/meshingress-artifact-scope-scanner/`.

## 2026-06-08 SBOM Slice Resolution

- Added a small embedded SBOM API in `lib/meshingress-artifact-scope-scanner`: `CycloneDxSbomGenerator`, `CycloneDxSbom`, and `EmbeddedCycloneDxJarSbomGenerator`.
- The generator inventories non-directory JAR entries, sorts entries by path, hashes the root artifact and each entry with SHA-256, and emits deterministic CycloneDX 1.6 JSON without timestamps.
- Added `EmbeddedCycloneDxJarSbomGeneratorTests`, which builds a representative temporary JAR and verifies stable JSON, component count, sorted components, and compact summary fields.
- Wired repository assessment to create a `cyclonedx-sbom` scanner result for JAR artifacts, write `cyclonedx-sbom.json` under the coordinate assessment directory, and expose compact SBOM metadata through `ArtifactAssessmentSummary.summary.sbom`.
- The SBOM scanner result has no findings and does not alter inferred, approved, denied, or requested scopes; bytecode scope inference remains the authority for inferred scope evidence.
- Updated `ArtifactRepositoryFlowTests` to verify SBOM scanner presence, summary fields, raw report path, raw report file storage, and unchanged scope inference behavior.
