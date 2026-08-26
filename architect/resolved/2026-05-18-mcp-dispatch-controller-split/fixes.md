# Fixes

## Files Changed

- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/McpDispatcher.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/McpMethodController.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/internal/InternalMcpController.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/tools/ToolsMcpController.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/RolesMcpController.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/RoleAuthorizationService.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/RoleToolService.java`
- `lib/meshingress-tool-api/src/main/java/dev/mrk/meshingress/api/McpCallContext.java`
- `lib/meshingress-tool-api/src/main/java/dev/mrk/meshingress/api/tools/ToolExecutionResult.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/ToolRegistry.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/InMemoryToolRegistry.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/McpClient.java`
- `app/meshingress-server/src/test/java/dev/mrk/meshingress/mcp/McpControllerTests.java`
- `app/meshingress-server/src/test/java/dev/mrk/meshingress/mcp/McpControllerOutputTests.java`
- `pom.xml`
- `AGENTS.md`

## Behavior Changes

- Replaced `admin/tools/*` routing with `roles/tools/*`.
- Added `X-Mcp-Role: admin` as the role header while preserving legacy `X-Mcp-Admin: true`.
- Kept bearer token admin authorization through `meshingress.mcp.roles.admin-token`, falling back to the prior `meshingress.mcp.admin-token`.
- Fixed `ToolExecutionResult.text/error()` so they construct `ToolExecutionResult` directly instead of downcasting a base `DispatchExecutionResult`.
- Restored Maven Java settings to Java 22 to match the repository guidance and local JDK.
