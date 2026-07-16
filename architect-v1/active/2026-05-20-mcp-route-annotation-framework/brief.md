# MCP Route Annotation Framework

## Objective

Design and implement a Spring Boot MCP route execution model where server paths can be declared with reusable route metadata:

```java
@McpRoute(
        id = "mcp.transport.post.v1",
        method = POST,
        path = "/mcp"
)
@PostMapping("/mcp")
public ResponseEntity<JsonNode> post(...) {
    ...
}
```

The route annotation modules should describe actual HTTP server paths. JSON-RPC method dispatch remains inside the MCP transport body only where the MCP protocol requires it.

## Current Server Boundary

The current live server route is:

```txt
POST /mcp
GET /mcp
DELETE /mcp
```

`POST /mcp` remains the MCP JSON-RPC transport. MCP tool calls such as `tools/list` and `tools/call` stay JSON-RPC methods handled inside that transport.

## Core Decisions

- Use `@McpRoute` to describe actual server paths, not JSON-RPC method strings.
- Keep JSON-RPC envelope handling inside the `/mcp` transport.
- Keep MCP tool calling as JSON-RPC because MCP clients expect `tools/list` and `tools/call` over the transport body.
- Use class-based middleware references instead of string middleware names.
- Each middleware class should represent exactly one middleware unit.
- Middleware should implement a shared interface with a consistent calling method.
- Keep `@McpConfigureMapping` for common route execution settings only.
- Use server-authoritative route IDs from `@McpRoute`; do not trust client-provided route IDs.
- Add structured logs and route registry validation so annotation behavior remains inspectable.

## Scope Boundary

This framework should handle route execution concerns:

- route identity
- request envelope
- middleware execution
- security middleware
- secret references
- audit/debug trace
- timeout metadata
- standard error behavior
- route registry validation
- observability hooks

It should not absorb domain/business logic, external API implementation logic, MCP tool policy logic, or MCP tool metadata policy logic.
