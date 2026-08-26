# Plan

1. Extend the tool-storage SPI with backward-compatible delegated-source registration and an explicit transfer-mode capability. Keep it an interface; unsupported implementations use the default failure.
2. Add `DELEGATED_EXTERNAL` and validate a Nextcloud OCS target independently from the existing direct-final WebDAV publisher.
3. Add Nextcloud delegated-workspace persistence: owner, tool ID, session/request IDs, root path, OPEN/SEALED/job states, native-file metadata, and ordered source references.
4. Implement authenticated OCS endpoints:
   - `POST /delegated-workspaces` to reserve an OPEN root;
   - `PUT /delegated-workspaces/{workspaceId}/files/{relativePath}` for native content;
   - `POST /delegated-workspaces/{workspaceId}/sources` for an atomic source batch;
   - `POST /delegated-workspaces/{workspaceId}/seal` to create exactly one queued import;
   - `GET /delegated-workspaces/{workspaceId}` for status and final manifest.
5. Enforce ownership, header-derived tool identity, relative path validation, collision checks, maximum source count, and the reserved manifest filename on every mutating endpoint.
6. Implement the Meshingress Nextcloud OCS client. `openWorkspace` reserves; `writeFile` uploads native bytes while OPEN; `delegateFile` registers source URL/output pairs; `publish` seals once; status reads the remote job.
7. Add the Toonverse delegated branch: resolve source metadata and register page URLs, but never open image download streams. Continue writing `chapter.json` and `book.json` as native workspace files before sealing.
8. Cover lifecycle policy, endpoint state transitions, authorization/collision cases, OCS request/status mapping, and Toonverse's no-local-media path. Deploy and test one delegated book after the Nextcloud file-size metadata repair is included.
