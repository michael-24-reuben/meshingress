# Summary

`LOCAL_ASYNC_EXTERNAL` is implemented and enabled in the checked-in server configuration. Tools return after durable enqueue rather than wait for external WebDAV completion; a bounded worker publishes the retained local staging files with recovery and retry support. This improves deadline resilience but intentionally does not remove the underlying download-then-upload copy path.

The bundled Toonverse WebSocket runner now keeps its connection open only as an observer: it polls `toonverse.publication-status` using the returned workspace identity and closes after a terminal handoff state. The handoff itself remains server-owned and does not depend on that WebSocket connection.
