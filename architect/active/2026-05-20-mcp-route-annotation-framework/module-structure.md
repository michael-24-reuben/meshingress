# Module Structure

## Library Split

Use the route framework as a small layered set of `lib/` modules rather than placing all types directly in the server application.

Do not add a centralized aggregate module. The route system should use only these route modules:

```txt
lib/
├─ meshingress-route-api/
├─ meshingress-route-annotations/
└─ meshingress-route-framework/
```

This split keeps stable contracts separate from annotations and separates both from the runtime execution layer.

## `lib/meshingress-route-api`

Purpose: shared route execution contracts that other modules can depend on without pulling in Spring-specific runtime behavior.

Owns:

- `HTTPRequest<Q, P, B>`
- `HTTPResponse<T>`
- `McpErrorResponse`
- `McpRouteExecutionContext`
- `McpMiddleware<Q, P, B>`
- framework exception base types where appropriate

Should avoid:

- Spring component scanning
- controller scanning
- route registry implementation
- reflection-heavy runtime code
- concrete application secrets or feature-flag providers

## `lib/meshingress-route-annotations`

Purpose: compile-time/declarative route metadata annotations.

Owns:

- `@McpRoute`
- `@McpConfigureMapping`
- `@McpSecret`
- `@McpRequestMiddleware`
- route metadata enums such as HTTP method or stability

May depend on:

- `meshingress-route-api`
- JetBrains annotations for `@Pattern`, if the project accepts that dependency

Should avoid:

- Spring runtime classes unless absolutely necessary
- route execution code
- secret resolution code
- MCP tool metadata policy logic

## `lib/meshingress-route-framework`

Purpose: runtime execution engine for annotated MCP routes.

Owns:

- route scanner
- route registry
- startup validator
- middleware executor
- route execution aspect/interceptor
- standard error mapper
- structured logging bridge
- metrics/audit hooks
- route introspection/debug metadata

May depend on:

- `meshingress-route-api`
- `meshingress-route-annotations`
- Spring context / AOP / MVC integration where needed
- Micrometer or Actuator integration if metrics are implemented here

Should avoid:

- domain-specific tool logic
- concrete external API clients
- hardcoded app secrets
- business validation that belongs inside services

## Server Application Boundary

The server app may consume the framework to declare and validate actual server paths.

Current server route attachment:

```txt
app/meshingress-server
    depends on lib/meshingress-route-framework
    depends on lib/meshingress-route-annotations
    depends on lib/meshingress-route-api
```

Current annotated paths:

```txt
POST /mcp
GET /mcp
DELETE /mcp
```

## Naming Decision

`meshingress-route-framework` is a good module name only when the module contains the route framework machinery: scanning, validation, execution, logging, and error handling.

Do not use `meshingress-route-framework` for an annotation-only module. Use `meshingress-route-annotations` for pure annotations and `meshingress-route-api` for stable contracts.

## Future Optional Split

If the runtime layer becomes too Spring-specific, split it later:

```txt
lib/meshingress-route-core/
lib/meshingress-route-spring/
```

Do not introduce this split until there is real pressure to support a non-Spring runtime.
