# Implementation Plan

1. Define `LOCAL_ASYNC_EXTERNAL` in the storage lifecycle configuration and specify its public tool-result/status contract.
2. Add durable handoff job and per-file transfer state to the workspace metadata model, including queue time, worker lease, attempt count, retry-after, terminal error classification, and remote receipt information.
3. Change local publication for this lifecycle to validate/package locally, write local metadata, atomically enqueue the handoff, and return without calling the external publisher in the request thread.
4. Add a bounded background worker that claims jobs, recovers abandoned leases after restart, and applies retry/backoff policy.
5. Publish generated collection paths with `MKCOL`, upload content in a deterministic order, persist accepted operations, and upload `manifest.json` last.
6. Define idempotency and collision handling for a write-only destination. In particular, never infer that an unrecorded prior `PUT` succeeded merely because a retry finds a name collision.
7. Retain local files for queued, in-progress, and failed handoffs; specify eventual expiry and operator-directed recovery without touching foreign content.
8. Report queued/progress/terminal state through the existing progress and workspace-status surfaces without extending the fixed MCP request deadline.
9. Add focused tests for enqueue return behavior, worker claim/restart recovery, retry classification, manifest-last ordering, terminal failure retention, and generated nested-directory creation.
10. Document configuration, observability, operational recovery, and the distinction from synchronous `LOCAL_EXTERNAL`.
