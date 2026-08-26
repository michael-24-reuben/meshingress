# Plan

## First Slice

- Define a repository publication eligibility configuration model with deterministic reason codes.
- Evaluate publication eligibility from durable assessment evidence, including required scanner/sandbox outcomes, approved scopes, review state, provenance, trusted builder, and signing-key status.
- Persist the eligibility decision with the publication record without trusting uploaded `requestedScopes` as authority.
- Add focused rejection tests for missing scanner/sandbox evidence and insufficient review before broad policy expansion.

## Boundaries

- Preserve requested, inferred, approved, and denied scope separation.
- Reuse retained `assessment.json`, `cyclonedx-sbom.json`, and `embedded-jar-sandbox.json` evidence from the resolved scanner pipeline.
- Do not add external scanner CLI integrations in this entry.
- Do not broaden runtime installation behavior unless repository publication eligibility requires a narrow contract change.
