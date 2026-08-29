# Context

- The current `ToolStorageService` only accepts local `InputStream` bytes through `writeFile`.
- `LOCAL_ASYNC_EXTERNAL` stages those bytes locally and later sends them to a direct-final WebDAV target.
- The installed Nextcloud app already accepts one OCS delegated-source blueprint and performs its own URL downloads through its persistent worker.
- A Nextcloud job naturally contains many sources. A separate remote job must not be created per page or file.
- Toonverse currently resolves page URLs and immediately downloads each page through `ToonverseClient.downloadMedia`; the delegated branch must retain URL discovery while skipping those downloads.
- The selected design solves mixed content with a reserved workspace. The Nextcloud app will expose an OPEN reservation before source processing; Meshingress uploads native files through an authenticated app endpoint, appends source URLs, and seals the reservation exactly once.
- The proposal is recorded in `integrations/nextcloud/workspace-upload-predelegation.md`. It is a design input, not a deployed API contract.
