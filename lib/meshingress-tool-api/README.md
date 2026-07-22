# MCP Tool Module Lifecycle — `meshingress-tool-api`

## Package Role

This is the public SPI and contract package for attachable Meshingress MCP tools. Tool modules depend on it instead of the server.

## User-Visible Contribution

It defines what a tool may expose through `tools/list` and execute through `tools/call`: identity, functions, visibility, structured results, call context, storage access, and invocation-scoped progress.

## Position in the Feature Path

```text
tool module or annotated handler
  -> meshingress-tool-api
  -> server registry / dispatcher
  -> MCP client
```

## Entry Points

- `api.tools.McpToolHandler` — executable handler SPI.
- `api.tools.McpToolDescriptor` and `function.McpFunctionDescriptor` — published tool/function metadata.
- `api.McpCallContext` — transport context passed to handlers.
- `api.result.DispatchExecutionResult` — terminal tool result contract.
- `api.result.progress.McpProgressReporter` — server-injected, per-invocation progress channel.
- `api.storage.ToolStorageService` — tool-facing workspace/file storage boundary.

## Feature Contract

```yaml
handler: McpToolHandler
inputs:
  descriptor: McpToolDescriptor
  call: arguments plus McpCallContext
output: DispatchExecutionResult
sideEffects:
  - optional workspace or file storage through ToolStorageService
  - optional progress events through McpProgressReporter
```

`McpToolDescriptor.toMcpJson(ObjectMapper)` serializes the public metadata. The progress reporter is infrastructure, not client input; the active server path injects it for each invocation.

## Dependencies

- Upstream: `toolspace/*`, annotated-tool adaptation, and the server's registry/executor.
- Downstream: `meshingress-tool-api-dispatch` provides typed structured-content values.

## Failure Behavior

The SPI itself does not translate errors. Handlers return `DispatchExecutionResult`; server transport code maps execution failures to JSON-RPC responses.

## Verification

Run the server and tool-framework focused tests when changing an SPI signature:

```powershell
.\mvnw.cmd -pl app/meshingress-server -am test
```

## Evidence and Open Questions

Confirmed by `McpToolHandler`, `McpToolDescriptor`, `McpCallContext`, and the progress/storage contracts under `src/main/java`. This package has no package-owned runtime resource.
