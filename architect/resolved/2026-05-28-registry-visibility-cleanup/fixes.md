# Fixes

## Files Changed

- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/registry/InMemoryToolRegistry.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/registry/DelegatingToolRegistry.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/tools/ToolsMcpController.java`
- `lib/meshingress-tool-framework/src/main/java/dev/mrk/meshingress/tools/framework/scanning/McpToolAnnotationScanner.java`
- `lib/meshingress-tool-framework/src/test/java/dev/mrk/meshingress/framework/scanning/McpToolAnnotationScannerTests.java`
- `app/meshingress-server/src/test/java/dev/mrk/meshingress/mcp/McpControllerTests.java`

## Changes

- Added a shared registry policy helper for enabled, visibility, allow-list, and deny-list checks.
- Added a function owner index so function-to-tool ownership lookup no longer scans all descriptors on each call.
- Removed the public `tools/register` stub from `ToolsMcpController`; registration remains under role-gated `roles/tools/register`.
- Reshaped `DelegatingToolRegistry` into an ordered composite that reads across delegates and sends mutations to the primary delegate.
- Made method-level `@McpInputSchema.description` populate generated default input schemas.
