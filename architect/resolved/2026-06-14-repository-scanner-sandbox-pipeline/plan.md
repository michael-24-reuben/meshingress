# Plan

## First Slice

- Add a repository scanner pipeline configuration model for required scanners, optional scanners, scanner versions, timeouts, and failure policy.
- Preserve the existing embedded Java/library scanners as the first configured required stages.
- Attach pipeline metadata to persisted scanner summaries without adding external scanner CLI execution.
- Add focused tests proving a required blocked scanner leaves the artifact non-installable and prevents approval/publication.

## Deferred

- Real sandbox execution remains deferred until the pipeline contract is stable.
- Dependency vulnerability analysis remains deferred until dependency-aware SBOM metadata is richer than the embedded JAR inventory.
- Publication eligibility enforcement remains deferred to `2026-06-18-publication-eligibility-policy`.
