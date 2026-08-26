# Fixes

## Files Changed

- `lib/meshingress-artifact-scope-scanner/src/main/java/dev/mrk/meshingress/artifact/scope/CycloneDxSbomGenerator.java`
- `lib/meshingress-artifact-scope-scanner/src/main/java/dev/mrk/meshingress/artifact/scope/CycloneDxSbom.java`
- `lib/meshingress-artifact-scope-scanner/src/main/java/dev/mrk/meshingress/artifact/scope/EmbeddedCycloneDxJarSbomGenerator.java`
- `lib/meshingress-artifact-scope-scanner/src/test/java/dev/mrk/meshingress/artifact/scope/EmbeddedCycloneDxJarSbomGeneratorTests.java`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/config/MeshingressRepositoryConfiguration.java`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/ArtifactService.java`
- `app/meshingress-repository/src/test/java/dev/mrk/meshingress/repository/artifact/ArtifactRepositoryFlowTests.java`
- `architect/resolved/2026-06-05-embedded-assessment-enrichment/meta.json`
- `architect/resolved/2026-06-05-embedded-assessment-enrichment/todo.md`
- `architect/resolved/2026-06-05-embedded-assessment-enrichment/notes.md`
- `architect/resolved/2026-06-05-embedded-assessment-enrichment/assessment.md`
- `architect/resolved/2026-06-05-embedded-assessment-enrichment/fixes.md`
- `architect/resolved/2026-06-05-embedded-assessment-enrichment/verification.md`
- `architect/resolved/2026-06-05-embedded-assessment-enrichment/summary.md`

## Behavioral Changes

- Added a scanner-module SBOM generator interface and deterministic JAR implementation.
- Generated CycloneDX 1.6 JSON documents include root artifact metadata, SHA-256 hashes, sorted JAR entry components, and compact summary fields.
- Repository assessment now adds a `cyclonedx-sbom` `ScannerResult` for JAR artifacts.
- Repository assessment writes raw SBOM JSON to `assessments/<coordinate>/cyclonedx-sbom.json`.
- Artifact assessment summaries now expose compact SBOM metadata under `summary.sbom`.
- Requested, inferred, approved, and denied scope semantics remain unchanged.
