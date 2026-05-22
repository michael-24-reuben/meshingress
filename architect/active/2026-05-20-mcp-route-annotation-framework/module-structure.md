# Module Structure

## Recommended Library Split

Use the route framework as a small layered set of `lib/` modules rather than placing all types directly in the server application.

Do not add a centralized aggregate module. The first implementation slice should create only these route modules:

```txt
lib/
├─ meshingress-route-api/
├─ meshingress-route-annotations/
└─ meshingress-route-framework/
```

This split keeps stable contracts separate from annotations and separates both from the Spring/runtime execution layer.

## `lib/meshingress-route-api`

Purpose: shared route execution contracts that other modules can depend on without pulling in Spring-specific runtime behavior.

Owns:

- `HTTPRequest<Q, P, B>`
- `HTTPResponse<T>`
- `McpErrorResponse`
- `McpRouteExecutionContext`
- `McpMiddleware<Q, P, B>`
- `AvailabilityPolicy<A extends Annotation>`
- `AvailabilityDecision`
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
- `@McpAvailabilityPolicy`
- `@EnableWithinTimeRanges`
- `@EnableOnDays`
- `@EnableWhenFeatureFlagOn`
- future specific availability annotations
- route metadata enums such as HTTP method, availability mode, stability, or capability flags

May depend on:

- `meshingress-route-api`
- JetBrains annotations for `@Pattern`, if the project accepts that dependency

Should avoid:

- Spring runtime classes unless absolutely necessary
- generic `args = { "key=value" }` availability as the preferred model
- route execution code
- secret resolution code

## `lib/meshingress-route-framework`

Purpose: runtime execution engine for annotated MCP routes.

Owns:

- route scanner
- route registry
- startup validator
- middleware executor
- availability evaluator
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

Do not introduce the implementation into `app/meshingress-server` yet.

The first implementation slice should stop at the reusable library modules. The server app should not gain route-framework dependencies, controllers, middleware adapters, route scanning configuration, or execution bridge wiring in this pending slice.

Future server integration can consume the framework and define concrete routes and route-specific middleware/policies after the library modules are stable.

Future dependency direction:

```txt
app/meshingress-server
    depends on lib/meshingress-route-framework
    depends on lib/meshingress-route-annotations
    depends on lib/meshingress-route-api
```

Example controller package responsibilities:

```txt
app/meshingress-server/src/main/java/dev/mrk/meshingress/server/
├─ controllers/        route controllers
├─ middleware/         app-specific middleware implementations
├─ availability/       app-specific availability policy implementations if not reusable
├─ security/           secret resolver, auth token verifier, principal mapping
└─ config/             Spring configuration
```

## Naming Decision

`meshingress-route-framework` is a good module name only when the module contains the runtime framework: scanning, validation, execution, logging, and error handling.

Do not use `meshingress-route-framework` for an annotation-only module. Use `meshingress-route-annotations` for pure annotations and `meshingress-route-api` for stable contracts.

## Future Optional Split

If the runtime layer becomes too Spring-specific, split it later:

```txt
lib/meshingress-route-core/
lib/meshingress-route-spring/
```

Do not introduce this split until there is real pressure to support a non-Spring runtime.

## Explicit Non-Goals For First Slice

- Do not create `lib/meshingress-route` as a centralized route module.
- Do not place reusable route framework code in `app/meshingress-server`.
- Do not add route framework dependencies to `app/meshingress-server`.
- Do not convert existing MCP controllers or WebSocket/HTTP dispatch paths yet.
- Do not attach a live annotated route to the server module yet.
