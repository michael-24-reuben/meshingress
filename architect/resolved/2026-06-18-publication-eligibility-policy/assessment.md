# Assessment

The publication eligibility objective is complete for the repository evidence gate.

The core gap was that signed publication records could prove integrity, but they did not yet prove that durable repository evidence made the artifact eligible for publication. Eligibility now evaluates persisted scanner and sandbox results, approval events, inferred/approved/denied scope state, provenance, trusted-builder policy, and signing-key policy at publish time.

Uploaded `requestedScopes` remain claims only. The policy checks inferred scopes against reviewer-approved and denied scopes, preserving the requested/inferred/approved/denied separation.

Signing-key eligibility is repository-side configuration, not runtime verification state. The repository can now require the active signer to be in a configured trusted key allowlist and absent from a configured revoked key list before a publication record is created. Runtime install verification remains responsible for validating signatures and rejecting revoked verification keys during install.

Deferred follow-up scope remains separate: external scanner CLI integrations, dependency vulnerability feeds, richer multi-review identity semantics, repository UI, and broader runtime installation behavior.
