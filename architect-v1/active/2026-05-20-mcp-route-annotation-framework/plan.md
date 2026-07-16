# Plan

## 1. Request and Response API

Keep:

- `HTTPRequest<Q, P, B>`
- `HTTPResponse<T>`
- standard `McpErrorResponse`

The request envelope should carry user input and framework metadata carefully. Operational execution metadata should primarily live in `McpRouteExecutionContext` rather than being pushed into request fields.

## 2. Route Annotation

Use `@McpRoute` with:

- `id`
- `method`
- `path`

The `path` value must be the actual server path, such as `/mcp`, not a JSON-RPC method name.

## 3. Middleware Model

Keep a middleware interface:

```java
public interface McpMiddleware<Q, P, B> {
    HTTPRequest<Q, P, B> apply(
            HTTPRequest<Q, P, B> request,
            McpRouteExecutionContext context
    );
}
```

Keep annotation:

```java
public @interface McpRequestMiddleware {
    Class<? extends McpMiddleware<?, ?, ?>>[] value();
}
```

Each middleware class should implement exactly one middleware concern.

## 4. Configure Mapping Annotation

Keep `@McpConfigureMapping` for cross-cutting route settings:

- secrets
- audit enabled
- debug trace enabled
- timeout
- stability

Do not overload this annotation with tool policy logic or nested policy DSL behavior.

## 5. Secret References

Use secret references, not raw secret values.

```java
@McpSecret(name = "instagramApiToken", ref = "instagram-api-token")
```

Secrets must not be logged, copied into error responses, or exposed through request attributes.

## 6. Execution Context

Keep `McpRouteExecutionContext` with fields such as:

- route ID
- request ID
- correlation ID
- principal
- tenant ID
- started timestamp
- attributes
- audit/debug trace collector

## 7. Execution Pipeline

Recommended order:

1. Normalize incoming request.
2. Resolve matched `@McpRoute`.
3. Inject/overwrite server-authoritative route ID.
4. Create route execution context.
5. Run security/request middleware.
6. Resolve secret references as needed.
7. Invoke controller method.
8. Emit audit/log/metric result.
9. Convert exceptions into standard error responses.

## 8. Startup Validation

At application boot, scan MCP routes and validate:

- unique route IDs
- route paths start with `/`
- route method metadata is present
- middleware classes implement `McpMiddleware`
- timeout values are non-negative
- secret refs are syntactically valid

Startup validation should fail fast for invalid framework configuration.

## 9. Observability

Add structured logs and metrics around significant steps.

Log events:

- `route.execution.started`
- `route.middleware.started`
- `route.middleware.completed`
- `route.execution.completed`
- `route.execution.failed`

Metrics:

- route request count
- route duration
- middleware duration
- error count
- timeout count

Metric tags should include route ID, result, middleware name, and exception type where safe.

## 10. Module Structure

Use this module layout:

```txt
lib/
├─ meshingress-route-api/
├─ meshingress-route-annotations/
└─ meshingress-route-framework/
```

Responsibilities:

- `meshingress-route-api`: stable request/response/context/middleware contracts.
- `meshingress-route-annotations`: route, middleware, secret, configure mapping, method, and stability annotations.
- `meshingress-route-framework`: scanner, registry, validator, pipeline, middleware executor, error mapper, logs, metrics, and Spring integration helpers.

Keep server-specific controllers, concrete tool clients, and app-level middleware in `app/meshingress-server`.

## 11. Server Attachment

Attach route annotations to actual Spring server paths first:

```txt
POST /mcp
GET /mcp
DELETE /mcp
```

Do not replace `tools/list` or `tools/call` with server paths. Those remain MCP JSON-RPC methods inside `POST /mcp`.
