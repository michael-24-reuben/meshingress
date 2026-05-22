# Guardrails

## Primary Objective

Build a small, inspectable MCP route execution framework for Spring Boot controllers. The goal is not to build a general-purpose policy engine or a complex workflow runtime.

## Do Not Deviate Into

- Dynamic route generation unless explicitly required later.
- Complex nested boolean availability DSLs.
- Runtime mutation of annotations.
- String-named middleware registries.
- Generic `args = { "key=value" }` availability configuration as the preferred path.
- Logging raw secrets, auth tokens, full request bodies, or sensitive headers.
- Trusting client-provided route IDs.
- Moving domain/business logic into annotations or middleware.
- Making `@McpConfigureMapping` responsible for every possible route concern.

## Required Design Properties

- Middleware references should be class-based.
- Middleware execution order should be deterministic.
- Every route should have a unique route ID.
- Startup validation should catch framework misuse before traffic is served.
- Availability should be implemented through specific annotations and policy handlers.
- All route execution failures should map to a standard error response.
- Observability should be structured and redact sensitive data.

## Simplicity Boundary

A route method should remain readable. If a route requires more than a few availability annotations or many middlewares, prefer moving grouped behavior into a named middleware/policy class rather than expanding annotation complexity indefinitely.

## Module Structure Guardrails

- Do not create a centralized `lib/meshingress-route` aggregate module.
- Do not place runtime execution code in `meshingress-route-annotations`.
- Do not make `meshingress-route-api` depend on Spring MVC, Spring AOP, or application-specific server code.
- Do not add the new route modules as dependencies of `app/meshingress-server` in the first implementation slice.
- Do not attach a live route implementation to the server module in the first implementation slice.
- Do not put app-specific Instagram, tenant, workspace, or external API logic into the reusable route framework modules.
- Do not name a pure annotation module `meshingress-route-framework`; reserve that name for the runtime framework.
- Do not introduce `meshingress-route-core` / `meshingress-route-spring` unless non-Spring runtime support becomes a real requirement.
