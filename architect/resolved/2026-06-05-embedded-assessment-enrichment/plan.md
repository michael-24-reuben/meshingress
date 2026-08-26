# Plan

## Phase 1: SBOM Shape

- Completed 2026-06-06: inspected artifact model, publication, scanner result, and repository assessment storage.
- Decision: do not add a new publication or artifact model field for the first slice.
- Store the raw CycloneDX JSON document as a scanner raw report under the artifact assessment directory.
- Add a `cyclonedx-sbom` scanner result with summary keys for format, spec version, component count, and raw report location.
- Let `ArtifactAssessmentSummary.summary` carry a compact `sbom` object, because `ArtifactRecord.assessment` and `ArtifactPublicationRecord.scanSummary` already preserve assessment summaries through metadata and publication.
- Keep the shape compatible with later Syft output by using scanner/source fields rather than hard-wiring SBOM data to embedded CycloneDX only.

## Phase 2: Embedded CycloneDX Generation

- Add a focused SBOM generator behind a small interface.
- Generate inventory metadata for a representative JAR without shelling out to external tools.
- Keep generated output deterministic enough for tests.

## Phase 3: Repository Assessment Wiring

- Wire SBOM generation into the existing repository assessment pipeline.
- Store raw scanner report or SBOM artifact metadata consistently with current scanner report storage.
- Ensure publication or review summaries can reference the SBOM without treating it as scope authority.

## Phase 4: Verification

- Add module-level tests for SBOM generation.
- Add or update repository tests only if assessment pipeline behavior changes.
- Record verification in this entry and in `architect/ASSIGNMENT.md`.

## Deferred Chapter 2 Slices

- SpotBugs embedded Java assessment adapter.
- FindSecBugs rules layered onto SpotBugs findings.
- Environment, secrets, network, process, and file scope-rule expansion.
- Source-aware matcher support.
- CodeQL query-pack generation and result normalization when source/build context exists.
