# Artifact Review and Publication — `meshingress-artifact-storage`

## Package Role

This package provides filesystem-backed artifact storage and deterministic repository layout helpers.

## User-Visible Contribution

Uploaded artifacts can be placed, retrieved, quarantined, and inspected from the configured repository root while the review service receives the stored blob and extracted file inventory.

## Position in the Feature Path

```text
artifact upload
  -> FileSystemArtifactStorage
  -> repository layout / quarantine
  -> ArtifactService review and publication
```

## Entry Points

- `FileSystemArtifactStorage`.
- `RepositoryLayout`, `StoredArtifactBlob`, and `ProjectRootResolver`.
- `ArtifactStorageException`.

## Feature Contract

```yaml
input: artifact coordinate and artifact content/path
output: StoredArtifactBlob and extracted ArtifactFileEntry values
sideEffects:
  - filesystem writes under configured repository root
  - quarantine extraction for review
```

## Configuration and Resources

The repository root is supplied by server-side repository configuration (`MeshingressRepositoryConfiguration` creates this storage). This package does not embed a root path or expose a configuration resource.

## Dependencies

- Upstream: `ArtifactService` and repository configuration.
- Downstream: local filesystem and artifact model types.

## Failure Behavior

I/O, layout, archive, or extraction failures surface as `ArtifactStorageException` for repository policy to handle.

## Verification

Use the server repository-flow tests after changing layout or extraction behavior.

## Evidence and Open Questions

Confirmed by `FileSystemArtifactStorage`, layout/blob records, and its server configuration wiring. Retention and cleanup policy belongs to the consuming repository service.
