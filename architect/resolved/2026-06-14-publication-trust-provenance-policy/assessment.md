# Assessment

The MVP publication trust path used one shared HMAC secret. The first hardening slice added signed provenance and explicit HMAC key IDs, but the runtime still had no independently verifiable asymmetric key or revocation state.

Ed25519 is available in Java 25 without an additional cryptography dependency. A configured list of verification keys provides the narrow lifecycle boundary needed now: multiple active keys support overlap during rotation, while revoked keys are rejected before signature acceptance. Repository publication eligibility rules depend on scanner/review evidence and remain separate from cryptographic record verification.
