# Todo

- [x] Inspect artifact model publication and scan summary fields for SBOM attachment points.
- [x] Choose raw CycloneDX, summarized SBOM fields, or combined storage for the first slice.
- [x] Add embedded CycloneDX SBOM generator interface and implementation.
- [x] Add deterministic test fixture for representative JAR SBOM generation.
- [x] Wire SBOM generation into repository assessment if the model/storage path is ready.
- [x] Run focused scanner or repository tests.
- [x] Update `architect/ASSIGNMENT.md` with results and the next narrow action.

## Follow-Up Slices

- [ ] Add SpotBugs embedded Java assessment adapter for general Java bug/risk patterns.
- [ ] Add FindSecBugs rules to the SpotBugs adapter for Java security findings.
- [ ] Expand scope-rule coverage for environment, secrets, network, process, and file behavior.
- [ ] Add source-aware matcher support for artifacts with source.
- [ ] Generate CodeQL query packs from the repository scope rule catalog.
- [ ] Add CodeQL result importer/normalizer for inferred scope findings.
- [ ] Document CodeQL source/build requirements and JAR-only fallback behavior.
