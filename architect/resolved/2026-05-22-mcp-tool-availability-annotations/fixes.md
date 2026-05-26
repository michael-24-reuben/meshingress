# Fixes

## Files Updated
- `lib/meshingress-tool-framework/src/main/java/dev/mrk/meshingress/tools/framework/scanner/McpToolAnnotationScanner.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/annotation/AnnotatedMcpToolHandler.java`

## Behavioral Changes
- Availability annotations are scanned and validated during tool discovery.
- Availability policies are enforced during tool invocation, returning TOOL_UNAVAILABLE when denied.
- Missing @McpConfigureMapping defaults to McpAvailabilityMode.ALL at invocation time.

