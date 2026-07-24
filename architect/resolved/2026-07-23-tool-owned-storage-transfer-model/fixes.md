# Implementation

- Replaced the lifecycle enum with `LOCAL_LOCAL` and `LOCAL_EXTERNAL`; legacy lifecycle values now fail property binding instead of silently selecting a conflicting adapter.
- Added `delegated-target` beside the existing local-byte `default-target`, permitting a WebDAV local-byte destination and a Nextcloud delegated-source destination to be configured together.
- Added `ToolStorageLocalPublicationMode` and extended the workspace request/result contracts with the resolved transfer and local-byte publication modes.
- Added `ToolStorageRouter`, which opens local and delegated workspaces concurrently and uses the persisted delegated viewer capability to route status calls after a restart.
- Changed local external publication to queue only when that local-byte workspace requests `QUEUED`; the durable worker now starts for `local-external` and processes only those jobs.
- Changed Toonverse `download-book` to request `DELEGATED_SOURCE_URLS` explicitly and branch from the returned workspace, not from a process-wide service mode.
- Updated the default application configuration and storage reference documentation.

No qBittorrent module was added. A completed qBittorrent job can now open a `LOCAL_BYTES` workspace and choose `INLINE` or `QUEUED` under `LOCAL_EXTERNAL` without changing the global lifecycle.

## Reopened regression fix

- Added `DelegatedSourceTargetConfiguredCondition`, which evaluates the configured target name after trimming whitespace.
- Applied it to both `delegatedViewerService` and `DelegatedViewerController`, so blank configuration disables the optional delegated surface instead of throwing at startup.

## Reopened configuration-binding fix

- Annotated the canonical `MeshingressProperties.Storage.External` record constructor with `@ConstructorBinding`, while retaining the nine-argument compatibility overload.
- Added a regression test that binds `meshingress.storage.external.delegated-target=nextcloud-primary` into `MeshingressProperties` and asserts the resulting immutable configuration retains that exact target name.

## Reopened dispatch-boundary fix

- Added the server-owned `StoragePublicationMcpController` with `@McpDispatchMapping("storage")` and the direct method `storage/publication-status`.
- Moved request validation and storage-failure normalization to that controller, returning `ToolStoragePublicationStatus` directly in the JSON-RPC result.
- Removed `toonverse.publication-status`, its Toonverse-only argument type, and its tool-discovery test expectation.
- Updated the Toonverse WebSocket publication monitor to send the new direct-dispatch method and consume its direct result payload.
