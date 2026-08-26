# Active Slice

The owner requested one immediate capability: tool methods may append an unannotated `McpProgressReporter` parameter, for example:

```java
DispatchExecutionResult downloadBook(
        ToonverseDownloadBookArgs arguments,
        McpCallContext context,
        McpProgressReporter progress
) {
    // progress reporting will be adopted later
}
```

This slice makes the parameter recognizable by route scanning and injectable by method invocation. Each invocation receives its own reporter, avoiding shared mutable reporter state. On WebSocket calls it is backed by the current WebSocket session; on HTTP calls it remains non-streaming because the current endpoint has one terminal response.

## Completed

- `McpToolAnnotationScanner` excludes `McpProgressReporter` from bindable function parameters and generated input schemas.
- `AnnotatedMcpToolHandler` creates a new no-op `McpProgressReporter` for each tool call.
- The annotated route framework recognizes the same infrastructure parameter through `McpProgressReporterArgumentResolver`.
- The API README documents `arguments, context, progress` as the intended appended-method shape.
- `ToonverseTool.downloadBook` now uses that exact appended signature and emits lifecycle events to its invocation-scoped reporter.
- Each WebSocket JSON-RPC request receives an independent `McpProgressLifecycle`, including individual requests in a batch. Notifications use `notifications/progress` with the JSON-RPC `callId`, MCP session ID, optional request ID, and the serialized lifecycle event.
- A reporter-capable WebSocket tool first has the existing `meshingress.dispatch.default-timeout` to emit a `plan`. The plan creates one hard deadline: `estimate + @McpConfigureMapping(timeoutMs)`. Later progress events are observable but never extend it. A tool that never plans is cancelled by the pre-plan guard. HTTP and tools without a reporter marker retain their existing immediate configured timeout.
- `McpProgressEstimator` receives each WebSocket call's progress events on a serial virtual-thread executor. After two positive completed-unit samples, it publishes `notifications/progress/estimate` with an EWMA of observed seconds per unit, elapsed time, remaining ETA, and total ETA. It ignores message-only, duplicate, regressive, and terminal updates. It is client-observability only: it never changes the planned estimate or dispatch deadline, and stops after a terminal event or cancellation.

Verification passed on 2026-07-18:

```powershell
    .\mvnw.cmd -pl app/meshingress-server -am test "-Dtest=McpWebSocketHandlerProgressTests,McpDispatchExecutorTests,McpToolAnnotationScannerTests,AnnotatedMcpToolHandlerProgressReporterTests,McpDispatchRouteFrameworkTests" "-Dsurefire.failIfNoSpecifiedTests=false"
```

The focused runtime run passed the observed-ETA calculation test, two WebSocket notification tests, the three executor deadline-policy tests, and the existing annotation and route-framework tests. No application launcher, server instance, or download was started.

The signature and lifecycle-event adoption were verified with the two focused `ToonverseToolTests` download cases and `OpenInkLibraryAutoConfigurationTests` (3 passed). The success case captures planned, chapter, packaging, publish, and successful terminal events; the unavailable-range case captures a failed terminal event. The broader `ToonverseToolTests` class still has one unrelated local configuration mismatch: its reflection assertion expects a 300-second timeout while the ignored local source is configured for 900 seconds. That timeout was left unchanged.
