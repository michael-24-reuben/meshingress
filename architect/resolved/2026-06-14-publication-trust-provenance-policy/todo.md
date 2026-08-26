# Todo

- [x] Add publication signing key id to signer output, publication records, repository publish/revoke, runtime verifier, and install registration source metadata.
- [x] Stamp repository-generated provenance into publication records and verify provenance tampering fails signature verification.
- [x] Define publication verification key model with active/revoked state and rotation through multiple configured keys.
- [x] Select and implement Ed25519 as the initial asymmetric signature algorithm.
- [ ] Extend provenance policy beyond the existing source/generator fields when build command, input checksum, and generated-tool lineage are ready.
- [ ] Define policy rules for publish eligibility. Transferred to `2026-06-18-publication-eligibility-policy`.
- [ ] Require configured assessment stages before publication when policy demands them. Transferred to `2026-06-18-publication-eligibility-policy`.
- [x] Update runtime verifier to validate key id, algorithm, signature, revocation, and signed provenance.
- [x] Add tests for active and revoked asymmetric signing keys; missing-scanner and insufficient-review tests are transferred to `2026-06-18-publication-eligibility-policy`.

