# Todo

- [x] Define the `LOCAL_ASYNC_EXTERNAL` configuration and public lifecycle contract.
- [x] Design durable handoff job and per-file transfer metadata, including migration and lease semantics.
- [x] Specify queue claiming, restart recovery, bounded concurrency, and retry/backoff policy.
- [x] Specify WebDAV create-only collision/idempotency behavior without foreign reads.
- [x] Integrate durable enqueue into local publication and return an accurate queued result.
- [x] Implement ordered external publication with generated collections and manifest-last completion.
- [x] Define local retention, expiry, and recovery for failed handoffs.
- [x] Expose status/progress without changing the fixed MCP request deadline.
- [x] Add focused worker, recovery, ordering, and failure-path tests.
- [x] Document configuration and operator recovery behavior.
- [x] Expose publication status through the Toonverse MCP surface.
- [x] Keep the WebSocket client open while polling the returned workspace until a terminal handoff state.
