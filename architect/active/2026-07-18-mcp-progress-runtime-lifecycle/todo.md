# Todo

- [x] Review the public reporter API, event schema, and correlation fields.
- [x] Keep WebSocket delivery in the server invocation layer so tool methods only receive `McpProgressReporter`.
- [x] Add fresh invocation-scoped reporter injection to route scanning and argument resolution.
- [x] Define and implement the pre-plan guard plus estimate and fixed-grace deadline; progress updates do not renew the deadline.
- [x] Define the WebSocket notification envelope and per-session delivery behavior.
- [x] Defer HTTP progress to a future asynchronous job/status or event-stream design; HTTP retains its terminal request/response timeout.
- [x] Adopt reporter lifecycle events in `toonverse.download-book` and wire its WebSocket delivery.
- [x] Add focused unit coverage for pre-plan timeout, planned-duration grace, reporter injection, and WebSocket notification serialization.
- [x] Derive serial, observed client-only ETA updates from positive unit-count changes without changing the dispatch deadline.
- [ ] Add a live WebSocket integration test only when a server test fixture is introduced for that transport.
