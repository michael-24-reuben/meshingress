# Assessment

The original workspace storage implementation was local-filesystem-only despite having SQL table configuration for `entries`, `usage`, and `events`; the implementation derived different workspace table names and did not use the configured names. Foreign retention also had no distinct lifecycle state, so any future external destination would have risked inheriting local retrieval and cleanup assumptions.

The resolved design makes storage location a closed lifecycle choice: `local-local`, `local-external`, or `external-external`. The invalid `external-local` direction is not representable. Local-local preserves current short-lived serving. Local-external stages locally, makes create-only WebDAV handoff requests, records a local receipt/event, and removes only local staging. It never reads, lists, modifies, moves, or deletes the foreign destination. External-external is rejected unless a provider-session adapter is installed; this avoids silently falling back to local staging.

WebDAV is the first provider because a conditional `PUT` with `If-None-Match: *` provides the enforceable no-overwrite request primitive. Its target is direct-final-only and therefore cannot satisfy external-external staging.
