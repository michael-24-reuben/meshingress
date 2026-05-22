# Plan

## 1. Request and Response API

Create or refine:

- `HTTPRequest<Q, P, B>`
- `HTTPResponse<T>`
- standard `McpErrorResponse`

The request envelope should carry user input and framework metadata carefully. Operational execution metadata should primarily live in `McpRouteExecutionContext` rather than being pushed into request fields.

## 2. Route Annotation

Create `@McpRoute` with:

- `id`
- `method`
- `path`

The route ID is server-authoritative. The framework should overwrite or verify any route ID present in the incoming request envelope.

## 3. Middleware Model

Create a middleware interface:

```java
public interface McpMiddleware<Q, P, B> {
    HTTPRequest<Q, P, B> apply(
            HTTPRequest<Q, P, B> request,
            McpRouteExecutionContext context
    );
}
```

Create annotation:

```java
public @interface McpRequestMiddleware {
    Class<? extends McpMiddleware<?, ?, ?>>[] value();
}
```

Each middleware class should implement exactly one middleware concern.

## 4. Availability Model

Use specific annotations instead of generic `args = { "key=value" }` policies.

Examples:

- `@EnableWithinTimeRanges`
- `@EnableOnDays`
- `@EnableWhenFeatureFlagOn`
- `@EnableForTenantPlan`
- `@EnableWhenDependencyHealthy`

Each availability annotation should be linked to a policy implementation by meta-annotation:

```java
@McpAvailabilityPolicy(EnableWithinTimeRangesPolicy.class)
public @interface EnableWithinTimeRanges { ... }
```

The policy interface should accept:

- request
- route execution context
- concrete annotation instance

## 5. Configure Mapping Annotation

Keep `@McpConfigureMapping` for cross-cutting settings:

- secrets
- `availabilityMode`
- audit enabled
- debug trace enabled
- timeout

Do not overload this annotation with deeply nested policy logic.

## 6. Secret References

Use secret references, not raw secret values.

```java
@McpSecret(name = "instagramApiToken", ref = "instagram-api-token")
```

Secrets must not be logged, copied into error responses, or exposed through request attributes.

## 7. Execution Context

Add `McpRouteExecutionContext` with fields such as:

- route ID
- request ID
- correlation ID
- principal
- tenant ID
- started timestamp
- attributes
- audit/debug trace collector

## 8. Execution Pipeline

Recommended order:

1. Normalize incoming request.
2. Resolve matched `@McpRoute`.
3. Inject/overwrite server-authoritative route ID.
4. Create route execution context.
5. Run security/request middleware.
6. Evaluate availability annotations.
7. Resolve secret references as needed.
8. Invoke controller method.
9. Emit audit/log/metric result.
10. Convert exceptions into standard error responses.

Authentication and authorization should happen before availability to avoid route probing by unauthenticated callers.

## 9. Startup Validation

At application boot, scan all MCP routes and validate:

- unique route IDs
- valid controller method signatures
- middleware classes are Spring beans or valid components
- middleware classes implement `McpMiddleware`
- availability annotations have policy mappings
- availability policy implementations are available
- time ranges parse
- zones parse
- feature flag names match expected patterns
- secret refs are syntactically valid
- route path/method metadata is non-empty

Startup validation should fail fast for invalid framework configuration.

## 10. Observability

Add structured logs and metrics around significant steps.

Log events:

- `route.execution.started`
- `route.middleware.started`
- `route.middleware.completed`
- `route.availability.started`
- `route.availability.completed`
- `route.execution.completed`
- `route.execution.rejected`
- `route.execution.failed`

Metrics:

- route request count
- route duration
- middleware duration
- availability denied count
- error count
- timeout count

Metric tags should include route ID, result, middleware/policy name, and exception type where safe.

## 11. Module Structure

Create only this module layout for the first implementation slice:

```txt
lib/
├─ meshingress-route-api/
├─ meshingress-route-annotations/
└─ meshingress-route-framework/
```

Responsibilities:

- `meshingress-route-api`: stable request/response/context/middleware/policy contracts.
- `meshingress-route-annotations`: route, middleware, secret, configure mapping, and specific availability annotations.
- `meshingress-route-framework`: scanner, registry, validator, pipeline, availability evaluator, middleware executor, error mapper, logs, metrics, and Spring integration.

Keep server-specific controllers, concrete tool clients, and app-level middleware in `app/meshingress-server`.

Do not create a centralized `lib/meshingress-route` aggregate module. Do not add these modules as dependencies of `app/meshingress-server` in this slice.

## 12. First Slice Completion Boundary

The first slice is complete when:

- the three route library modules exist in the Maven reactor
- dependency direction is `route-framework -> route-annotations -> route-api` where needed
- the modules compile
- unit tests cover the API/annotation/framework validation behavior that can be tested without the server app

Server integration, concrete MCP controllers, dispatch-path conversion, and live route attachment belong to a later architect entry or later activation phase.
