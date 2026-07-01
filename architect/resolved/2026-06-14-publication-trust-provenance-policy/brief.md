# Brief: Publication Trust Provenance Policy

## Goal

Harden publication trust beyond the current MVP HMAC signature by adding stronger signing, provenance, and policy-based approval requirements.

## Scope

- Replace or extend shared-secret HMAC publication signing with asymmetric signatures, key IDs, key rotation, and revoked-key handling.
- Record provenance such as source repo URL, commit hash, builder identity, build command, input artifact checksum, and generated-tool lineage.
- Add policy rules that can require SBOM, sandbox pass, approved scopes, reviewer count, trusted builder, and non-revoked signing key before publication.
- Ensure runtime install can verify the stronger publication record format.

## Dependency

This should consume durable state from SQL storage and assessment outcomes from the scanner/sandbox pipeline.

