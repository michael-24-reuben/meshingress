# Annotated MCP Route Execution — `meshingress-route-framework`

## Package Role

This package scans annotated routes and turns an incoming MCP dispatch request into validated argument binding, middleware execution, method invocation, return adaptation, observations, and protocol-ready errors.

## User-Visible Contribution

An MCP method reaches the matching annotated application method with validated JSON inputs, infrastructure parameters such as `McpCallContext` and `McpProgressReporter`, and consistent error behavior.

## Position in the Feature Path

```text
MCP request
  -> McpDispatchMethodScanner / registry
  -> argument resolvers and middleware
  -> handler-method invocation
  -> return adapter / error mapper
  -> server transport
```

## Entry Points

- `McpRouteAnnotationScanner`, `McpRouteExecutionPipeline`, and `McpRouteInvoker`.
- Dispatch scanner, registry, handler-method invoker, argument resolvers, and schema registry under `framework.dispatch`.
- `StandardMcpRouteErrorMapper` and execution observers.

## Local Execution Flow

The framework discovers route methods, validates their declarations, resolves parameters from the request or server infrastructure, executes middleware, invokes the method, and adapts the return value. `McpProgressReporterArgumentResolver` injects a fresh reporter without making it a client schema field.

## Dependencies

- Upstream: `meshingress-route-api`, route annotations, server transport/configuration.
- Downstream: application route methods and, for shared call contracts, `meshingress-tool-api`.

## Failure Behavior

Binding, validation, middleware, and invocation failures pass through the standard route error mapper; observers receive lifecycle events.

## Verification

```powershell
.\mvnw.cmd -pl app/meshingress-server -am test "-Dtest=McpDispatchRouteFrameworkTests"
```

## Evidence and Open Questions

Confirmed by scanner, registry, pipeline, resolver, schema, observer, and error-mapper sources. Runtime route registration is supplied by the server configuration.
