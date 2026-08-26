# Fixes

- Replaced the flat storage properties with lifecycle, local, external-target, and shared metadata sections in `MeshingressProperties` and `application.properties`.
- Added `StorageFileApi`: complete create/write/append/complete/abort/contains/metadata/size/read/delete vocabulary, capabilities, handles, receipts, and stable result categories.
- Added a fully capable local implementation with atomic `CREATE_NEW` creation.
- Added a WebDAV write-only implementation that denies foreign observation and deletion, and a local-staging WebDAV handoff publisher that sends conditional creates only.
- Added startup lifecycle selection: local-external currently selects enabled direct-final WebDAV; external-external requires a provider-session adapter and fails closed until one is installed.
- Made the metadata store use the configured `entries`, `usage`, and `events` table names. It persists completed and failed handoff audit events without credentials, URLs, foreign paths, or bytes.
- Updated cleanup so it acts only on local staging/published directories; foreign objects are never addressed by cleanup.
- Updated `storage.properties.md`, including the currently supported `env:<VARIABLE>` WebDAV credential reference convention.
