# Implemented

- Added delegated-source registration methods to the `ToolStorageService` interface in `lib/meshingress-tool-api`.
- Implemented `DELEGATED_EXTERNAL` lifecycle adapter in Meshingress server.
- Implemented Nextcloud OCS client handling `POST /delegated-workspaces`, `PUT /delegated-workspaces/{id}/files/{path}`, `POST /delegated-workspaces/{id}/sources`, `POST /delegated-workspaces/{id}/seal`, and `GET /delegated-workspaces/{id}`.
- Updated `ToonverseTool` to discover chapter page URLs and delegate them without invoking `downloadMedia`.
- Added Base64 content upload adaptation for compatibility with Nextcloud 34 JSON endpoints.
- Implemented opaque viewer capability streaming tokens to replace raw authenticated WebDAV paths.
- Added non-zero file metadata verification for published manifests.
