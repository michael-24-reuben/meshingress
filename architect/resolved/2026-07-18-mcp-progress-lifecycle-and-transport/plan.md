# Proposed Implementation Plan

## 1. Finalize the public progress API

- Review and simplify the existing draft in `meshingress-tool-api`.
- Keep event data immutable and add server correlation fields needed by consumers: invocation/request identity, sequence, and timestamp.
- Represent planning and lifecycle policy separately so tools can report estimates without owning deadline enforcement.
- Remove direct WebSocket dependencies from the API package.
- Replace mutable singleton reporter instances with per-invocation factories.

## 2. Add framework injection support

- Teach `McpDispatchMethodScanner` that `McpProgressReporter` is an injectable runtime parameter, not an MCP schema/input parameter.
- Add a progress argument resolver and make its creation invocation-scoped.
- Ensure ordinary handlers remain behaviorally compatible.

## 3. Build server-owned lifecycle tracking

- Create an invocation record before submitting the tool call.
- Record `startedAt`, plan data, `lastActivityAt`, current progress, expected deadline, grace deadline, hard deadline, and terminal state.
- On estimate overrun, publish an explicit overrun event once and begin fixed grace timing.
- Enforce inactivity and hard-deadline cancellation without allowing events to move the hard deadline.
- Define idempotent terminal transitions and cancellation behavior.

## 4. Deliver and expose progress by transport

- Add a WebSocket adapter that emits a documented, correlated MCP notification/event envelope.
- Decide and document the HTTP job/status surface before implementing it; do not alter the current synchronous `/mcp` response shape implicitly.
- Persist or retain only the event history required by the chosen HTTP/status contract, with bounded memory/retention.

## 5. Adopt with Toonverse and verify

- Have `toonverse.download-book` publish a plan based on chapter count and update chapter/page progress.
- Use measured throughput only to improve client-facing estimates; it must not extend the hard deadline.
- Add API, resolver, lifecycle, WebSocket, HTTP-status, and late-event/cancellation tests.
- Run focused module/server tests only after activation and before resolution.
