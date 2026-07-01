# Fixes

- Added `Ed25519PublicationRecordSigner` using Java's Ed25519 provider and Base64 signatures.
- Added configured publication verification keys with key ID, algorithm, X.509 public key, and `ACTIVE` or `REVOKED` status.
- Extended `PublicationRecordVerifier` to accept active Ed25519 keys, reject revoked/unknown/mismatched keys, and retain keyed plus legacy HMAC compatibility.
- Preserved signature coverage of the complete publication record, including provenance.
- Split scanner/reviewer/trusted-builder eligibility rules into pending entry `2026-06-18-publication-eligibility-policy` so this resolution does not overclaim unavailable assessment policy.
