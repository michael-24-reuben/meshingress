# MCP Tool Module Lifecycle — `meshingress-tool-framework`

## Package Role

This package is the runtime-independent scanner and policy layer for annotation-based MCP tools.

## User-Visible Contribution

It converts annotated methods into discoverable tool/function metadata, including input schemas and availability behavior, so tool authors need not implement `McpToolHandler` directly.

## Position in the Feature Path

```text
@McpTool / @McpFunction class
  -> McpToolAnnotationScanner
  -> annotated handler adapter in the server
  -> MCP discovery and invocation
```

## Entry Points

- `framework.scanning.McpToolAnnotationScanner`.
- Availability annotations and policies under `tools.availability`.

## Local Execution Flow

The scanner reads tool/function annotations, evaluates supported metadata, and produces descriptors consumed by the server-side annotated handler. The currently active progress lifecycle is treated by the scanner as infrastructure rather than client schema input.

## Dependencies

- Upstream: `meshingress-tool-annotations` and annotated tool classes.
- Downstream: `meshingress-tool-api` descriptors/results and the server's `AnnotatedMcpToolHandler`.

## Failure Behavior

Invalid annotation usage or unsupported method signatures is surfaced during scanning/registration rather than as a successful MCP call.

## Verification

```powershell
.\mvnw.cmd -pl lib/meshingress-tool-framework -am test "-Dtest=McpToolAnnotationScannerTests"
```

## Evidence and Open Questions

Confirmed by `McpToolAnnotationScanner` and its tests. Policy-specific configuration is supplied by the consuming application; this package owns no main resource.
