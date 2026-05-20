# Fixes

## Files Changed

- `app/meshingress-server/pom.xml`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/InMemoryToolRegistry.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/annotation/AnnotatedMcpToolHandler.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/annotation/AnnotatedMcpToolHandlerProvider.java`
- `lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/scanner/McpToolAnnotationScanner.java`
- `lib/meshingress-tool-annotations/src/test/java/dev/mrk/meshingress/api/tools/annotation/scanner/McpToolAnnotationScannerTests.java`

## Behavioral Changes

- The server now depends directly on `meshingress-tool-annotations`.
- Annotated Spring beans with `@McpTool` are scanned and wrapped as `McpToolHandler` adapters unless they already implement `McpToolHandler`.
- `InMemoryToolRegistry` registers legacy handlers and annotated adapters together, with duplicate tool name and handler key checks.
- Annotated tool functions can now use the cleaner `call(Args arguments, McpCallContext context)` shape for a single args object.
- Adapter invocation binds `tools/call` arguments into annotated function parameters, invokes the selected default function, and adapts non-`ToolExecutionResult` returns into normal MCP tool results.
