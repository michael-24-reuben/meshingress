# Annotated MCP Route Execution — `meshingress-route-api`

## Package Role

This package defines stable request, response, middleware, execution-context, validation, and error contracts for Meshingress routes.

## User-Visible Contribution

It gives an annotated route a consistent request/response shape and stable JSON-RPC error semantics before the server turns it into an MCP response.

## Position in the Feature Path

```text
MCP request
  -> HTTPRequest / route context
  -> annotated route framework
  -> HTTPResponse or McpErrorResponse
  -> MCP transport response
```

## Entry Points

- `HTTPRequest`, `HTTPResponse`, and `McpRouteExecutionContext`.
- `McpMiddleware` and `McpConfigureRouteMapping`.
- `McpDispatchException`, `McpRouteValidationException`, error codes, and `McpErrorResponse`.

## Feature Contract

```yaml
input: HTTPRequest<Q, P, B>
execution: McpRouteExecutionContext plus middleware
output: HTTPResponse<T> or McpErrorResponse
errors: typed route/dispatch exceptions and error codes
```

## Dependencies

- Upstream: server transport and route annotations.
- Downstream: `meshingress-route-framework` scanning, binding, validation, and invocation.

## Failure Behavior

The API exposes typed failure contracts; mapping into protocol output occurs in the framework/server layer.

## Verification

Run route-framework and server transport tests after changing a public request, response, or exception type.

## Evidence and Open Questions

Confirmed by the API records, middleware interface, and error classes. This package has no configuration resources.
