# MCP Tool Module Lifecycle — `meshingress-tool-api-dispatch`

## Package Role

This package supplies the typed, reusable structured-content vocabulary returned by MCP tool execution.

## User-Visible Contribution

Clients can receive consistently shaped tables, files, media, search results, diagnostics, notifications, and health values rather than a tool-specific untyped payload for each result.

## Position in the Feature Path

```text
tool implementation
  -> structured-content record in this package
  -> DispatchExecutionResult
  -> JSON-RPC tools/call response
```

## Entry Points

- `dispatch.StructuredContentKind` — kind marker.
- `dispatch.data.TableColumn`, `dispatch.file.FileEntry`, and `dispatch.search.SearchResult`.
- Media records under `dispatch.media`.
- `dispatch.process.DiagnosticIssue` and `dispatch.tool.HealthCheckResult`.

## Feature Contract

```yaml
content: typed record
examples:
  - TableColumn
  - FileEntry
  - SearchResult
  - MediaItemRef
consumer: DispatchExecutionResult serialization
```

The package models values only: it does not execute a tool, choose a transport response, or perform storage.

## Dependencies

- Upstream: `meshingress-tool-api` exposes `DispatchExecutionResult` through its handler SPI.
- Downstream: tool implementations and server serialization consume these values.

## Failure Behavior

There is no package-level error translation. Invalid semantics are the responsibility of the producer and server transport.

## Verification

Compile the API reactor and exercise a `tools/call` result in server tests after changing a public record.

## Evidence and Open Questions

Confirmed by the public records under `src/main/java/dev/mrk/meshingress/dispatch`. No runtime configuration or resources are owned here.
