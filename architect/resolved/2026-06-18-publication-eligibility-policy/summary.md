# Summary

Publication eligibility is now enforced at repository publish time from durable evidence rather than only from a signed payload. The repository checks required scanner/sandbox evidence, approval count, inferred and approved scope consistency, required provenance, trusted builder policy, and trusted/non-revoked signing key policy before creating a signed publication record. Accepted decisions are stored with the publication record and are part of the current signed payload, while runtime install verification keeps legacy HMAC compatibility.

Focused repository and runtime verification passed. Follow-up work such as external scanner CLI integrations, dependency vulnerability feeds, richer multi-review workflow, repository UI, and broader runtime install behavior remains outside this resolved objective.
