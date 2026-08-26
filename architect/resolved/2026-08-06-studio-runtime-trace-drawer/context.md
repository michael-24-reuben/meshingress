# Context

Studio already opens a workflow WebSocket and receives run-started, node-started, node-completed, and run-completed events. Those events do not currently carry timestamps, elapsed time, payload sizes, or a cancellation command. The initial UI therefore captures timings in the browser for a session-local trace.

The current beta `WorkflowRuntime` processes its ready queue serially. The timeline must still use interval lanes so it shows concurrent events in shared temporal slots when the runtime later gains concurrent scheduling. With today's runtime, normal runs occupy one lane.

Cancellation, persisted trace history, authoritative server timing, and measured network transfer bytes remain separate follow-up work.
