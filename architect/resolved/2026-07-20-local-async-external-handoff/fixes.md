# Implementation Record

- Added the `LOCAL_ASYNC_EXTERNAL` lifecycle and configurable worker interval, lease, attempt, and backoff policy.
- Added durable handoff-job and per-file acceptance metadata tables, including queue, lease, retry, completion, and terminal-error state.
- Changed publication to write the local manifest and atomically enqueue external work before returning `HANDOFF_QUEUED` to the tool caller.
- Added a bounded background worker that recovers expired leases, records accepted files, uploads the manifest last, retries transient failures, and retains terminal failures locally.
- Added publication-status and explicit failed-handoff requeue methods to the storage SPI.
- Kept foreign storage create-only and write-only: no remote reads, listings, overwrite, deletion, or cleanup.
- Enabled `local-async-external` in the checked-in server configuration and documented operation/recovery settings.
- Added `toonverse.publication-status`, keyed by the `sessionId` and `requestId` returned from `toonverse.download-book`.
- Updated the bundled WebSocket client to poll that status every second and close only on `HANDED_OFF`, `AVAILABLE`, `HANDOFF_FAILED`, or `FAILED`.
