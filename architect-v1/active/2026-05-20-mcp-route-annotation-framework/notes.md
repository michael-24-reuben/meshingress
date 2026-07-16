# Notes

## Key Tradeoff

The design improves route inspectability by moving route identity and server path metadata into annotations. That creates framework behavior behind the scenes, so startup validation, route introspection, structured logs, and metrics are required to keep behavior understandable.

## Recommended First Implementation Slice

Start with:

1. `HTTPRequest`
2. `HTTPResponse`
3. `@McpRoute`
4. `McpMiddleware` interface
5. `@McpRequestMiddleware`
6. `@McpConfigureMapping`
7. startup validation
8. structured route execution logs

Keep this slice inside the three route library modules:

- `lib/meshingress-route-api`
- `lib/meshingress-route-annotations`
- `lib/meshingress-route-framework`

## Module Naming Note

`meshingress-route-framework` is accepted as the runtime module name. It should contain the framework machinery, not just annotations. The preferred supporting modules are `meshingress-route-api` and `meshingress-route-annotations`.

## 2026-05-21 First Library Slice

Implemented the first code slice as three `lib/` modules only:

- `lib/meshingress-route-api`
- `lib/meshingress-route-annotations`
- `lib/meshingress-route-framework`

No centralized `lib/meshingress-route` aggregate module was introduced.

Implemented source coverage:

- request/response/error/context contracts
- middleware contracts
- route, middleware, secret, configuration, HTTP method, and stability annotations
- framework scanner, registry, validator, middleware executor, execution pipeline, standard error mapper, and structured logging observer

## 2026-05-22 Server Attachment

The current `/mcp` transport methods now carry route metadata:

- `mcp.transport.post.v1` -> `POST /mcp`
- `mcp.transport.get.v1` -> `GET /mcp`
- `mcp.transport.delete.v1` -> `DELETE /mcp`

The server now creates an `McpRouteRegistry` bean from the annotated `McpController` methods and validates the registry at startup. MCP `tools/list` and `tools/call` remain JSON-RPC methods inside `POST /mcp`.

## 2026-05-22 Controller Dispatch Integration

Moved the annotation-based MCP method dispatch surface out of `app/meshingress-server` and into the route modules:

- `lib/meshingress-route-annotations` owns dispatch annotations.
- `lib/meshingress-route-api` owns dispatch error contracts.
- `lib/meshingress-route-framework` owns dispatch scanning, registry, schema records, argument binding, return adaptation, and handler invocation.

`McpDispatcher` now invokes the route-framework dispatch registry directly. `InternalMcpController`, `ToolsMcpController`, and `RolesMcpController` expose their JSON-RPC methods through annotations instead of `McpMethodController` implementations and local `switch` routing.

Verification:

- `mvnw.cmd -pl app/meshingress-server -am test-compile` passes with Java 22 compiler overrides.
- `mvnw.cmd -pl app/meshingress-server -am clean test` runs the new dispatch framework tests successfully. The full server test target still has the pre-existing `McpControllerTests.roleMethodsRequireAdminAuthorization` failure because `roles/tools/list` returns a successful result without admin instead of a JSON-RPC auth error.
