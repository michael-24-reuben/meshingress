# Plan

## First Slice

- Done: inventory the current publication signing and verification path in the repository and runtime install modules.
- Done: implement the smallest compatible record-shape change for key id, algorithm, and provenance signature coverage.
- Done: add focused tests around publication record creation and runtime verification before expanding policy rules.
- Next: define the initial asymmetric signing path and compatibility behavior for HMAC records.
- Next: add a signing-key registry/policy shape for active, revoked, and rotated keys.
- Next: define publish eligibility rules for required scanner evidence and review sufficiency.

## Boundaries

- Do not start external scanner CLI or sandbox execution work in this entry.
- Do not collapse repository publication approval and runtime install into one opaque flow.
- Keep SBOM/scanner evidence as inputs to publication policy, not as trusted authority by itself.
- Preserve requested, inferred, approved, and denied scope separation.

## Deferred Within This Entry

- Full key-rotation administration UI.
- Multi-reviewer workflow.
- Runtime install durability beyond the publication verifier changes needed for this policy.
