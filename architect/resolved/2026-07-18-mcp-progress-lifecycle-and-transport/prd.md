# Product Requirements

## Goal

Provide a safe, reusable way for long-running MCP tools to report execution progress and for the server to distinguish active work from inactivity.

## Proposed tool-facing contract

- A handler opts in with an injected `McpProgressReporter` parameter, alongside its typed arguments and `McpCallContext`.
- The reporter is created fresh for each invocation. It is never a mutable singleton.
- A tool reports a plan, detailed or message-only updates, warnings, completion, and failure.
- A tool may estimate its duration from its input and observed throughput. The estimate and grace duration are server execution policy, not caller-controlled JSON input.

## Required lifecycle policy

1. A plan establishes `estimatedDuration`, work-unit total where known, and optional phases.
2. The dispatcher records each accepted event as activity and exposes it to the active transport.
3. At `startedAt + estimatedDuration`, an incomplete operation enters an overrun state.
4. It receives exactly one fixed grace interval.
5. `hardDeadline = startedAt + estimatedDuration + graceDuration`; progress events cannot extend this deadline.
6. A separate inactivity threshold may fail an operation earlier when it has stopped emitting progress.
7. Timeout, cancellation, and terminal failure must produce one correlated terminal outcome; later events are ignored or rejected safely.

## Transport requirements

- WebSocket calls receive progress as correlated server-to-client notifications on the existing session.
- The HTTP `POST /mcp` JSON-RPC endpoint does not emit interim responses. If HTTP progress is required, it returns or exposes a job reference whose state/events are obtained through a separately designed status or polling surface.
- The public API module must not depend on Spring WebSocket, HTTP controllers, or a concrete session implementation.

## Non-goals

- Do not convert the existing HTTP MCP endpoint into a streaming response in this slice.
- Do not let arbitrary tool logging extend deadlines.
- Do not make a caller-provided timeout or estimate authoritative.
- Do not replace existing `DispatchExecutionResult`; it remains the terminal tool result.
- Do not implement the architecture until this record is reviewed and activated.

## Acceptance criteria for implementation

- A tool handler can accept `McpProgressReporter` without declaring it as an MCP JSON parameter.
- Every invocation gets isolated reporter state and correlation metadata.
- The dispatcher enforces estimate, one non-sliding grace duration, hard deadline, and inactivity behavior deterministically.
- WebSocket progress is delivered only to the correct correlated session/request.
- HTTP behavior is explicitly specified and tested without pretending to stream interim JSON-RPC responses.
- Terminal result, cancellation, and emitted progress have focused tests for race and late-event behavior.
