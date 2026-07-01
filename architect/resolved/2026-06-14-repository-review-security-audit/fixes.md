# Fixes

## Files Changed

- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/ArtifactController.java`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/ArtifactLifecycleGuard.java`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/ArtifactService.java`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/RepositoryAccessPolicy.java`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/RepositoryAction.java`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/store/ArtifactMetadataStore.java`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/artifact/store/SqlArtifactMetadataStore.java`
- `app/meshingress-repository/src/test/java/dev/mrk/meshingress/repository/artifact/ArtifactRepositoryFlowTests.java`
- `lib/meshingress-artifact-model/src/main/java/dev/mrk/meshingress/artifact/model/ArtifactTrustStatus.java`

## Behavioral Changes

- Repository endpoints now use explicit repository actions and header-based role checks.
- Pending review artifacts are available through a role-gated API-only review queue.
- Reject, revoke, delete, and restore transitions have explicit endpoints and lifecycle audit events.
- Revoke requires prior durable publication and stores a re-signed revoked publication record.
- Delete is admin-only and soft/state-only; it refuses active non-revoked publications and repeat deletion.
- Restore is admin-only and restores the prior trust status from the latest durable `DELETE` event.
- `DELETED` is a non-installable artifact trust status.
