# Fixes

## Files Changed

- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/McpMethodController.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/McpDispatcher.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/internal/InternalMcpController.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/tools/ToolsMcpController.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/RolesMcpController.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/dispatch/**`
- `app/meshingress-server/src/test/java/dev/mrk/meshingress/mcp/AnnotationMcpMethodControllerTests.java`
- `app/meshingress-server/src/test/java/dev/mrk/meshingress/mcp/McpAnnotationDispatchMvcTests.java`

## Behavioral Changes

- `McpMethodController` now exposes `supportedMethods()` for startup duplicate mapping checks.
- `McpDispatcher` discovers all controller beans and fails fast when two controllers declare the same MCP method.
- `AnnotationMcpMethodController` adapts annotated Spring beans into the existing controller SPI.
- Annotation scanning supports `@McpDispatchMapping`, `@McpDispatchMethod`, `@McpDispatchParam`, and `@McpSchema`.
- Argument resolution supports `params`, `args`, `name`, `method`, and `McpCallContext`.
- Typed binding supports raw `JsonNode` values, object nodes, scalar values, concrete DTOs, and explicit interface implementations.
- Return values can be `JsonNode`, maps, records, POJOs, or null converted through Jackson.
