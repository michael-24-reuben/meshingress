# Fixes

## Files Changed

- `lib/meshingress-artifact-scope-scanner/src/main/java/dev/mrk/meshingress/artifact/scope/CycloneDxSbom.java`
- `lib/meshingress-artifact-scope-scanner/src/main/java/dev/mrk/meshingress/artifact/scope/EmbeddedCycloneDxJarSbomGenerator.java`
- `lib/meshingress-artifact-scope-scanner/src/test/java/dev/mrk/meshingress/artifact/scope/EmbeddedCycloneDxJarSbomGeneratorTests.java`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/config/MeshingressRepositoryProperties.java`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/ArtifactService.java`
- `app/meshingress-repository/src/main/resources/application.properties`
- `app/meshingress-repository/src/test/java/dev/mrk/meshingress/repository/artifact/ArtifactRepositoryFlowTests.java`
- `app/meshingress-repository/src/test/java/dev/mrk/meshingress/repository/artifact/ArtifactRepositoryScannerPipelineTests.java`
- `app/meshingress-repository/src/test/java/dev/mrk/meshingress/repository/artifact/PowerShellCliArtifactRepositoryUploadInstanceTests.java`
- `architect/active/2026-06-14-repository-scanner-sandbox-pipeline/meta.json`
- `architect/active/2026-06-14-repository-scanner-sandbox-pipeline/todo.md`
- `architect/active/2026-06-14-repository-scanner-sandbox-pipeline/notes.md`

## Behavioral Changes

- Added explicit scanner pipeline configuration metadata for required scanner stages, optional stages, timeouts, versions, and failure policy.
- Added `embedded-jar-sandbox` to the default required scanner pipeline between `cyclonedx-sbom` and `bytecode-scope-scanner`.
- Extended embedded CycloneDX output with Maven coordinate, package URL, and dependency component metadata when packaged Maven files are available.
- Added raw report retention defaults for scanner and sandbox outputs under the artifact assessment directory.
- Added a static embedded JAR sandbox report that records no-execution isolation guarantees and suspicious executable payload findings.
- Persisted raw sandbox output as `embedded-jar-sandbox.json` beside `assessment.json` and `cyclonedx-sbom.json`.
- Extended repository assessment summaries with `rawReportRetention`, SBOM dependency counts, scanner pipeline metadata, and sandbox isolation metadata.

## Compatibility

- Existing embedded scanner flow remains Java/library-oriented and does not invoke external scanner CLIs.
- Uploaded `requestedScopes` remain claims; assessment still preserves requested, inferred, approved, and denied scope separation.
- Publication eligibility policy remains deferred to `2026-06-18-publication-eligibility-policy`.
