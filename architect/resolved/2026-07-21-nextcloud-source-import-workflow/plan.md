# Implementation Plan

1. Create a dedicated Nextcloud app, `meshingress`, with a versioned OCS API and an authenticated service identity scoped to the chosen Nextcloud user and selected `Workspace/` parent folder.
2. Establish the app-managed `Workspace/Meshingress/` namespace. Create only missing managed directories: `storage`, `docs`, `generated`, `repository/artifacts`, `repository/quarantine`, `repository/vendor`, and `var/logs`.
3. Add `POST /source-imports` and `GET /source-imports/{jobId}`. The authenticated Meshingress caller supplies `X-Meshingress-Tool-Id`; the body is only `{url, path}`. The controller validates ownership/syntax, persists a job, schedules a `QueuedJob`, and returns `202`; it never downloads on the request thread.
4. Build a durable import-job store with the URL, requested relative path, authenticated owner, tool ID, resolved stable `storage/<tool-id>/...` path, attempt count, state, timestamps, terminal error, and final metadata. Do not put session or request identifiers in the Nextcloud storage path. Define `QUEUED`, `RUNNING`, `COMPLETED`, and `FAILED` states before adding retries.
5. Implement strict tool-scoped path normalization. Reject missing/invalid tool IDs, absolute paths, empty segments, `.`/`..`, backslashes, reserved or duplicate final names, and any resolution outside `Workspace/Meshingress/storage/<tool-id>/`. Create only missing directories in that target namespace.
6. Fetch through `IClientService`, stream into a private temporary file or controlled temporary target, enforce configured scheme/host/redirect/size/time limits, then atomically publish through the Nextcloud Node API. Do not use shell commands, raw PHP filesystem writes into the data directory, or a WebDAV round trip from inside the app.
7. Return canonical Nextcloud metadata after success: tool ID, user-visible workspace path, file ID, size, MIME type, ETag, completed timestamp, and SHA-256 if the stream has been hashed. Preserve the source URL and a safe terminal error for diagnosis.
8. Add a Meshingress workspace client that submits source-import jobs and polls status without reading/listing the foreign filesystem. Preserve the existing WebDAV publisher only for tools that genuinely own local bytes.
9. Remove `EXTERNAL_EXTERNAL` from the Meshingress storage lifecycle enum, configuration docs, policy validation, and focused tests. Do not replace it with a source-import lifecycle; source import is a separate workspace operation.
10. Add focused Nextcloud app tests for route authorization, tool identity isolation, workspace/path containment, URL rejection, directory creation, successful metadata, source failure, retry behavior, and idempotency; then add Meshingress workspace-client and storage-lifecycle-removal tests.
11. In a later Meshingress-server slice, add a dedicated `meshingress.storage.lifecycle` value for tool-produced source links. That value submits `{url, path}` plus the stable tool identity to this app and reports the terminal Nextcloud metadata; it does not restore or reinterpret `EXTERNAL_EXTERNAL`.

## Design Decisions Required Before Implementation

- Which authenticated Nextcloud user owns the `Workspace/Meshingress/` namespace?
- Whether the existing `Workspace/` folder is the final selected parent name and whether the displayed `Meshingress/` folder casing is fixed.
- Whether repeated `(url, path)` requests are rejected, deduplicated, or create a new versioned file name.
- Whether only public/signed HTTPS URLs are supported in v1. The recommendation is yes: do not transmit external source credentials.
- The max file size, allowed hosts, redirect policy, concurrency, retry limit, and retention of failed job records.
- Whether Meshingress polls status or the Nextcloud app sends a signed callback. Start with polling; callback adds an externally reachable trust boundary.
