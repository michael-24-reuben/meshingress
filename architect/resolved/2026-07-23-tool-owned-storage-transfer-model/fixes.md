# Implementation

- Replaced the lifecycle enum with `LOCAL_LOCAL` and `LOCAL_EXTERNAL`; legacy lifecycle values now fail property binding instead of silently selecting a conflicting adapter.
- Added `delegated-target` beside the existing local-byte `default-target`, permitting a WebDAV local-byte destination and a Nextcloud delegated-source destination to be configured together.
- Added `ToolStorageLocalPublicationMode` and extended the workspace request/result contracts with the resolved transfer and local-byte publication modes.
- Added `ToolStorageRouter`, which opens local and delegated workspaces concurrently and uses the persisted delegated viewer capability to route status calls after a restart.
- Changed local external publication to queue only when that local-byte workspace requests `QUEUED`; the durable worker now starts for `local-external` and processes only those jobs.
- Changed Toonverse `download-book` to request `DELEGATED_SOURCE_URLS` explicitly and branch from the returned workspace, not from a process-wide service mode.
- Updated the default application configuration and storage reference documentation.

No qBittorrent module was added. A completed qBittorrent job can now open a `LOCAL_BYTES` workspace and choose `INLINE` or `QUEUED` under `LOCAL_EXTERNAL` without changing the global lifecycle.
