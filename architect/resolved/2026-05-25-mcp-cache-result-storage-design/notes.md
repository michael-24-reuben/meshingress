# Notes

## 2026-05-25 Activation

- Implementation target is the annotated tool runtime path, specifically `AnnotatedMcpToolHandler`, because it is the current point where annotated MCP functions are invoked and adapted into `DispatchExecutionResult`.
- The annotation remains declarative policy. Runtime storage, key generation, TTL handling, and serialization belong in server-side cache classes.
- MVP scope is local filesystem storage with no-op and memory store support for configuration and tests.

## 2026-05-25 Implementation Notes

- Added cache runtime classes under `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/cache`.
- Wired `AnnotatedMcpToolHandler` to resolve `@McpCacheResult` and wrap annotated function invocation with cache lookup/write behavior.
- Added `meshingress.cache.*` typed configuration and default application properties.
- Fixed reactor packaging drift encountered during verification: tool modules should not be repackaged as Boot executable jars, and `toolspace/powershell-cli-tool` now points at the live parent POM.
