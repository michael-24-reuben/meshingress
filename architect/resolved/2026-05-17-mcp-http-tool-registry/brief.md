# Brief

## Request

Design the initial MCP HTTP interface for the current Spring Boot service, focusing only on `/mcp` for now.

The user wants support for:

- MCP tool calling over HTTP.
- Tool checking and validation.
- Tool registration.
- Tool updates.
- A clear statement of HTTP paths to build.

## Current Direction

The service should expose one canonical MCP transport endpoint:

```http
POST   /mcp
GET    /mcp
DELETE /mcp
```

Tool operations should be expressed as JSON-RPC MCP methods over `POST /mcp`, not as separate REST endpoints per tool.

## Naming Note

`meshingress` may not be the right final service name if the product is specifically an MCP server for structured engineering memory. Better candidates include:

- `architect-mcp`
- `task-sentinel-mcp`
- `sentinel-architect`
- `project-memory-mcp`

Recommended working name: `architect-mcp`.

## Scope

In scope:

- `/mcp` HTTP contract.
- JSON-RPC request/response conventions.
- MCP lifecycle methods.
- Tool discovery.
- Tool invocation.
- Tool checking.
- Admin-only dynamic tool registration/update/delete methods.
- Internal registry model.
- Build phases.

Out of scope for the first implementation:

- Full resources API.
- Full prompts API.
- Public REST CRUD routes for architect entries.
- UI.
- Remote multi-tenant marketplace-style tool installation.
