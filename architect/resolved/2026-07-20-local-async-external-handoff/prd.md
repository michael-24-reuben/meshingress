# Product Requirements

## Problem

The synchronous `LOCAL_EXTERNAL` publication path makes one request responsible for both producing content and completing an external transfer. The request can time out while external upload is still viable, leaving no durable route to finish or clearly report the handoff.

## Lifecycle contract

`LOCAL_ASYNC_EXTERNAL` must use these durable handoff states in addition to the existing workspace lifecycle:

1. `HANDOFF_QUEUED` — local files and metadata are complete; a worker may claim the handoff.
2. `HANDOFF_IN_PROGRESS` — one worker has claimed the handoff with a lease/attempt record.
3. `AVAILABLE` — all content was accepted by the external destination and the completion manifest was accepted last.
4. `HANDOFF_FAILED` — publication reached a non-retryable or exhausted-retry failure; local evidence remains available for diagnosis and an explicitly designed recovery action.

The tool request must return after durable enqueue, not after remote completion. It must surface the workspace/operation identity and the queued publication state without claiming that remote content is already available.

## External publication contract

- Keep the existing create-only, write-only foreign-storage rule: no foreign reads, listings, overwrite, delete, or cleanup.
- Create generated WebDAV collection segments before uploads. An already-existing collection response is acceptable; a missing configured base path is not silently created.
- Upload all generated content before `manifest.json`; upload the manifest last so it is the remote completion marker.
- Persist transfer receipts/attempt state after each accepted operation so a worker restart can distinguish recorded success from work that still needs an explicit retry decision.
- Treat authentication/configuration failures and a rejected base path as terminal until configuration changes. Treat network failures and selected server failures as retryable with bounded backoff. Define collision behavior explicitly because write-only create-only storage cannot safely inspect a conflicting remote object.

## Recovery and retention

The worker must recover queued and expired-lease work after a process restart. It must bound concurrency and retry pressure. Local workspace files and metadata must not be deleted on a queued, in-progress, or failed async handoff; the eventual retention/expiry policy must preserve enough evidence for retries and diagnosis.

## Acceptance criteria

- A request with `LOCAL_ASYNC_EXTERNAL` returns after durable enqueue even when the external destination is slow.
- Restarting the server does not lose queued handoffs.
- A retry resumes from persisted local state and never uploads `manifest.json` before all required content is accepted.
- Terminal failures retain a diagnosable local record and expose an accurate state to the caller/operator.
- Existing `LOCAL_LOCAL`, `LOCAL_EXTERNAL`, and capability-gated `EXTERNAL_EXTERNAL` behavior remains unchanged unless a separately approved migration says otherwise.
