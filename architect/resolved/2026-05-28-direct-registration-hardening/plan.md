# Plan

## Phase 1: Inspect Direct Registration Paths

- Review `ExperimentalToolRegistrationStrategy`, `BundleToolRegistrationStrategy`, and adjacent registration service tests.
- Confirm where checksum, source, provenance, and phase behavior currently diverge from repository-backed publication installation.
- Keep repository-backed installation as the trusted production path.

## Phase 2: Add Focused Hardening

- Start with checksum mismatch coverage for direct experimental local-JAR registration.
- Preserve development and smoke-test usability for direct registration phases.
- Avoid changing repository publication install behavior unless direct registration tests expose a shared verifier gap.

## Phase 3: Document Boundary

- Record whether direct experimental/staging registration remains development-oriented or gains stricter publication-record requirements.
- Keep any larger production policy changes as follow-up work.

## Phase 4: Verification

- Run focused server registration tests first.
- Run broad server smoke if registration behavior changes.
