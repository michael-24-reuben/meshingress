# Local Async External Handoff

`LOCAL_EXTERNAL` currently finishes a tool workspace by publishing to its external destination in the request path. A slow, unavailable, or transiently failing destination can therefore exhaust the MCP tool deadline after the tool has already downloaded and packaged its output.

Add a distinct `LOCAL_ASYNC_EXTERNAL` lifecycle: create and validate the workspace locally, durably queue the external handoff, and let a server-side worker publish it after the request returns. The workspace must retain enough local state to resume safely after retries or a process restart.

This is a planning record. It does not authorize implementation.

## Desired outcome

- A successful local package/download is not discarded merely because final external publication cannot finish within the request deadline.
- The tool result identifies a durable workspace or operation and reports that publication is queued or in progress.
- The external worker performs create-only publication, retries only safe transient failures, and makes the remote manifest its final completion marker.
- Local staging remains recoverable until the handoff reaches a terminal, recorded state.

## Scope boundaries

In scope: lifecycle/configuration design, durable handoff state, worker ownership and recovery, retry/collision semantics, progress/status reporting, and WebDAV directory/upload ordering.

Out of scope: changing the MCP deadline, changing credentials, treating a write-only external target as a read source, destructive external cleanup, and provider-managed upload-session support required by `EXTERNAL_EXTERNAL`.
