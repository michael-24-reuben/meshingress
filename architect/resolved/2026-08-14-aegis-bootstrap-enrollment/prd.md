# Product Requirements: Aegis Bootstrap Administrator Enrollment

The implemented core provides a portable contract for securely enrolling the
first administrator, then permanently retiring the bootstrap credential until
an explicit recovery or rotation operation is authorized.

The implementation satisfies the record's requirements: only a configured
special action accepts bootstrap evidence; host stores expose an atomic
enrollment boundary; issuance state contains no raw secret; stable safe denial
reasons cover disabled, invalid, expired, consumed, revoked, and exhausted
states; and no restart behavior reissues a credential. Concrete persistence,
secret storage, HTTP, OIDC, JWT, MCP, and Spring adapters remain excluded.
