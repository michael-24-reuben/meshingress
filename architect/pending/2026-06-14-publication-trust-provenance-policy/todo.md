# Todo

- [ ] Define publication signing key model with key id, algorithm, active/revoked state, and rotation behavior.
- [ ] Decide initial asymmetric signature algorithm.
- [ ] Extend publication records with provenance fields needed for runtime trust decisions.
- [ ] Define policy rules for publish eligibility.
- [ ] Require configured assessment stages before publication when policy demands them.
- [ ] Update runtime verifier to validate key id, algorithm, signature, revocation, and provenance requirements.
- [ ] Add tests for tampered provenance, revoked signing key, missing required scanner, and insufficient review.

