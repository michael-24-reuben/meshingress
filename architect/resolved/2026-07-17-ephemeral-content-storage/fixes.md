# Fixes

- Moved the public tool integration contract to `meshingress-tool-api` as `ToolStorageService`, `ToolStorageWorkspace`, and file request/result types.
- Replaced opaque token blobs and `meshingress-storage-api` with a session/request workspace model.
- Stores staged and published files under `.staging/{sessionId}/{requestId}` and `tools/{sessionId}/{requestId}/files` respectively; only server code handles filesystem paths.
- Persists workspace and file metadata in `storage_workspaces` and `storage_workspace_files`; `files/manifest.json` is generated from that metadata, including the tool ID, and is not authoritative.
- Replaced the route with `GET` and `HEAD /storage/{sessionId}/{requestId}/files/{relativePath}`.
- Retained TTL, staging/published limits, request budgets, active-stream tracking, and bounded recursive workspace cleanup.
