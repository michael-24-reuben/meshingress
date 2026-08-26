# Assessment

`LOCAL_ASYNC_EXTERNAL` now decouples final external publication from the MCP request path. It intentionally keeps the local staging copy so the server can retry after a destination outage or restart; it does not attempt to solve source-to-destination streaming efficiency.

The implementation records a durable handoff job and accepted file receipts. A bounded worker claims queued or expired-lease jobs, retries transient failures with capped backoff, leaves terminal failure evidence locally until normal expiry, and treats a recorded `manifest.json` upload as the external completion marker.
