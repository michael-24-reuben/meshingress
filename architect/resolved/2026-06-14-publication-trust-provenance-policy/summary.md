# Summary

Publication records now support independently verifiable Ed25519 signatures selected by key ID, with explicit active/revoked key status and overlapping active keys for rotation. Runtime verification rejects unknown, revoked, algorithm-mismatched, malformed, or invalid signatures while preserving existing HMAC records. Signed provenance remains integrity-protected. Evidence-dependent publication eligibility rules were split into a pending follow-up rather than being claimed as complete.
