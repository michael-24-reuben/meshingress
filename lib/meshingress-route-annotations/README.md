# Annotated MCP Route Execution — `meshingress-route-annotations`

## Package Role

This package defines the annotations that declare an MCP route, its dispatch methods, parameters, schema, HTTP method, and middleware.

## User-Visible Contribution

Application code can describe a route declaratively; the framework uses those declarations to make protocol methods reachable and validate client input.

## Position in the Feature Path

```text
annotated route class
  -> route annotations
  -> McpRouteAnnotationScanner / McpDispatchMethodScanner
  -> dispatch registry and pipeline
```

## Entry Points

- `McpRoute` and `McpDispatchMapping`.
- `McpDispatchMethod` and `McpDispatchParam`.
- `McpSchema`, `McpHttpMethod`, and `McpRequestMiddleware`.

## Feature Contract

```yaml
source: Java type and method annotations
route: McpRoute
dispatch: McpDispatchMethod and McpDispatchMapping
parameters: McpDispatchParam
middleware: McpRequestMiddleware
output: scanner-readable route metadata
```

## Dependencies

- Upstream: application route classes.
- Downstream: `meshingress-route-framework`; tool annotations are referenced where route and tool metadata overlap.

## Failure Behavior

Bad declarations are rejected by scanner/validator logic in the framework rather than by this metadata-only package.

## Verification

Run the server's route-framework tests after adding or changing an annotation member.

## Evidence and Open Questions

Confirmed by the seven annotation definitions under `route/annotations`. There are no package-owned runtime resources.
