# MCP Progress Lifecycle and Transport

Turn the existing `meshingress-tool-api` progress draft into a transport-neutral execution-progress contract for long-running MCP tools.

The design must let a tool report plans, progress, warnings, completion, and errors without letting the tool or client extend execution indefinitely. A planned duration establishes the expected completion window; an incomplete operation may use one fixed grace duration, after which the dispatcher must stop it. WebSocket callers should receive live correlated progress events. The existing HTTP JSON-RPC request/response endpoint must retain its single-response contract and use a separate job-status or polling design if it needs progress visibility.

This is a planning and review record only. No production runtime behavior is approved or implemented by this entry.
