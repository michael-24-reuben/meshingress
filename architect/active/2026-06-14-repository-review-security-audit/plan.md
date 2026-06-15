# Plan

## First Slice

- Inventory repository endpoints in `ArtifactController`.
- Add explicit repository role checks without introducing a Spring Security dependency.
- Carry actor and request id into lifecycle events for upload, assess, approve, and publish.
- Add focused tests for unauthorized approve/publish and persisted audit metadata.

## Deferred

- Review queue/read API.
- Revoke/delete/restore lifecycle operations.
- Admin UI.
- External scanner sandboxing and publication provenance policy entries.

## Next Slice

- Decide whether pending-review listing belongs in `ArtifactController` or a separate review controller.
- Add an API-only review queue endpoint backed by SQL artifact metadata and assessment rows.
- Extend lifecycle event coverage when reject/revoke/delete/restore transitions are introduced.
