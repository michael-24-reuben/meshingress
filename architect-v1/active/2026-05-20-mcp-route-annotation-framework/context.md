# Context

## Background

The design started from a desired Spring Boot controller style where handlers could receive a typed request envelope:

```java
post(HTTPRequest<Query, Params, Body> request)
```

The live server currently has a single MCP transport route family under `/mcp`, with JSON-RPC method dispatch inside the request body.

## Current Direction

The preferred model is:

- route annotations describe actual server paths
- `/mcp` remains the MCP protocol transport
- `tools/list` and `tools/call` remain MCP JSON-RPC methods
- class-based middleware references
- one middleware class per middleware concern
- shared middleware interface
- JavaDoc and JetBrains `@Pattern` for string parameter guidance
- structured logs and metrics to reduce runtime opacity
- startup validation to fail fast

## Existing Project Notes

The project uses a multi-module Maven layout with separate tool API, tool annotation, route API, route annotation, route framework, tool framework, toolspace, and server modules.

The server module can consume the route framework to validate annotated HTTP server paths. JSON-RPC method controllers remain a protocol implementation detail under `POST /mcp`.

## Important Design Constraints

- Java annotations cannot reference local methods directly.
- `@McpConfigureMapping` should remain relatively shallow and should not become a nested policy DSL.
- Route ID must be authoritative from server annotation metadata, not client input.
- Route annotation `path` values must be actual HTTP server paths.
- MCP tool protocol methods should not be modeled as server paths.

## Module Structure Decision

Use exactly this three-module `lib/` split for the route system:

```txt
lib/meshingress-route-api
lib/meshingress-route-annotations
lib/meshingress-route-framework
```

Do not create a centralized catch-all module such as `lib/meshingress-route`, `lib/meshingress-route-core`, or a server-owned route module for this slice.

Stable contracts should live in `meshingress-route-api`. Declarative annotations should live in `meshingress-route-annotations`. Runtime execution machinery should live in `meshingress-route-framework`.
