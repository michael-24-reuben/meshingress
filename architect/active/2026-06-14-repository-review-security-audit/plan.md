# Plan

## First Slice

- Inventory repository endpoints in `ArtifactController`.
- Add explicit repository role checks without introducing a Spring Security dependency.
- Carry actor and request id into lifecycle events for upload, assess, approve, and publish.
- Add focused tests for unauthorized approve/publish and persisted audit metadata.

## Deferred

- Revoke/delete/restore lifecycle operations.
- Admin UI.
- External scanner sandboxing and publication provenance policy entries.

## 2026-06-16 Review Queue Slice

- Keep the first review surface API-only in `ArtifactController`.
- Add `GET /artifact/reviews/pending` as a read endpoint requiring any repository role.
- Back the queue with SQL artifact metadata and assessment rows for artifacts in `REVIEW_PENDING`.
- Preserve the existing scope split in the response: uploaded `requestedScopes` are claims, inferred scopes are assessment evidence, and approved/denied scopes stay empty until review.

## Next Slice

- Add reject/revoke/delete/restore lifecycle operations only when their state semantics are defined.
- Extend lifecycle event coverage for the new transitions.
- Keep scanner sandboxing and publication provenance policy in their pending follow-up entries.
