# Current Context

## Draft API present today

`lib/meshingress-tool-api/src/main/java/dev/mrk/meshingress/api/result/progress/` currently contains the user-provided draft:

- `McpProgressReporter`
- `Reporter`
- `ProgressUpdate`
- `ConsumeProgressUpdate`
- console, no-op, and WebSocket reporter hooks

The draft already preserves phase/unit coordinates for message-only updates and supports planned, in-progress, warning, successful-terminal, and failed-terminal events. Its README correctly identifies that `McpProgressReporter.NOOP` is unsafe because `Reporter` retains mutable progress and terminal state; the final API must use a fresh reporter per invocation or a genuinely stateless no-op implementation.

## Current dispatcher constraints

- `McpDispatchExecutor` submits the whole call to a worker and waits once with `Future.get(timeout)`.
- When a function has no positive `timeoutMs`, `McpDispatchExecutor` falls back to `meshingress.dispatch.default-timeout`, currently 30 seconds.
- `ToonverseTool#downloadBook` was locally changed in the ignored `toolspace/x-open-ink-library` source tree to use a 15-minute annotation timeout. It has not been built, run, or verified and is not the proposed lifecycle solution.
- `McpDispatchMethodScanner` currently permits only `McpCallContext` as an unannotated injected parameter. A progress reporter requires a scanner rule and an `McpDispatchArgumentResolver` supplied by server runtime wiring.

## Transport constraints

- `McpWebSocketHandler` already has the connection/session context needed to associate outbound events with a client.
- `McpController` serves one JSON-RPC response per HTTP request. It cannot use the existing response contract for interim progress events.
- The API module currently contains `WebSocketReporter`; that transport-specific adapter should move to server-side code or be replaced by a neutral event-sink abstraction before the public API is finalized.

## Initial implementation candidates

- API: immutable `McpProgressPlan`, correlated `McpProgressEvent`, reporter interface/facade, and transport-neutral listener/sink abstraction.
- Route framework: explicit reporter parameter recognition and resolver support.
- Server: invocation lifecycle registry, deadline/inactivity policy, WebSocket event adapter, and HTTP job/status design.
- Tool module: Toonverse as the first adopter after infrastructure tests pass.
