# Brief: Publication Eligibility Policy

## Goal

Define and enforce repository publication eligibility from durable assessment, review, scope, provenance, builder, and signing-key evidence.

## Scope

- Configurable requirements for SBOM, scanner/sandbox pass, approved scopes, reviewer count, trusted builder, required provenance, and non-revoked signing key.
- Persisted policy decision and reason codes.
- Focused rejection tests for missing scanner evidence and insufficient review.

## Dependency

Scanner-dependent rules require `2026-06-14-repository-scanner-sandbox-pipeline`; cryptographic verification is supplied by resolved entry `2026-06-14-publication-trust-provenance-policy`.
