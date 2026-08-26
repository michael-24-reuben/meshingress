# PRD: MCP HTTP Tool Registry

## Problem

The service needs to act as an MCP server. The immediate concern is exposing a correct and extensible `/mcp` HTTP surface that supports tool discovery, tool calling, tool validation, and controlled tool registry mutation.

The implementation should avoid designing a REST-per-tool API because MCP clients expect JSON-RPC MCP methods over a single transport endpoint.

## Goals

- Implement `/mcp` as the canonical HTTP endpoint.
- Support basic MCP session initialization.
- Support `tools/list` and `tools/call`.
- Add internal tool checking before execution.
- Add admin-only methods for dynamic tool check/register/update/delete.
- Keep standard MCP methods separate from custom admin methods.
- Make tool registry mutation auditable and permissioned.

## Non-Goals

- Do not expose every tool as its own HTTP path.
- Do not allow unauthenticated tool registration or update.
- Do not implement arbitrary remote code execution as a first-class feature.
- Do not require SSE for the MVP.
- Do not require a public REST admin API for the first MCP pass.

## Users

- MCP clients connecting to the local/server HTTP endpoint.
- Local developer tooling that wants to inspect available tools.
- Admin/dev workflows that want to validate or register tools dynamically.

## Requirements

### R1: MCP Endpoint

The server must expose:

```http
POST /mcp
```

It should also reserve:

```http
GET /mcp
DELETE /mcp
```

### R2: JSON-RPC Validation

The server must validate:

- JSON parseability.
- `jsonrpc: "2.0"`.
- request `id` when a response is expected.
- method presence.
- params shape.
- batch request shape, if batch support is enabled.

### R3: Initialization

The server must support:

```txt
initialize
notifications/initialized
ping
```

`initialize` should return server capabilities including tool support.

### R4: Tool Discovery

The server must support:

```txt
tools/list
```

The response must include enabled public tools with:

- `name`
- `title`, when available
- `description`
- `inputSchema`
- annotations, when available

### R5: Tool Invocation

The server must support:

```txt
tools/call
```

The server must:

- Find the tool by name.
- Verify it is enabled.
- Verify caller authorization.
- Validate arguments against `inputSchema`.
- Execute the mapped handler.
- Return an MCP tool result.

### R6: Tool Checking

The server should support a custom admin method:

```txt
admin/tools/check
```

It should validate a proposed tool descriptor without registering it.

Checks should include:

- valid name syntax
- unique name, unless update mode is requested
- valid JSON Schema input schema
- allowed visibility
- allowed handler key
- side-effect policy
- admin authorization

### R7: Tool Registration

The server should support a custom admin method:

```txt
admin/tools/register
```

Registration must be admin-only and must run the same checks as `admin/tools/check`.

After successful registration, the server should emit:

```txt
notifications/tools/list_changed
```

when notification transport is available.

### R8: Tool Update

The server should support a custom admin method:

```txt
admin/tools/update
```

Updates must be admin-only and auditable.

Supported update fields:

- `title`
- `description`
- `enabled`
- `visibility`
- `inputSchema`
- `outputSchema`
- `annotations`
- `handlerKey`, only if explicitly allowed

### R9: Tool Delete/Disable

The server should support a custom admin method:

```txt
admin/tools/delete
```

Default behavior should be soft-delete or disable, not physical deletion.

### R10: Auditability

Every registry mutation should record:

- actor
- action
- tool name
- previous version
- new version
- timestamp
- request id/correlation id

## Acceptance Criteria

- A client can call `initialize` through `POST /mcp` and receive server capabilities.
- A client can call `tools/list` and receive available tools.
- A client can call a valid tool through `tools/call`.
- Invalid JSON-RPC returns a JSON-RPC protocol error.
- Tool execution errors return a tool result with `isError: true` where appropriate.
- Admin-only methods reject unauthenticated or unauthorized callers.
- `admin/tools/check` validates descriptors without changing registry state.
- `admin/tools/register` adds a new dynamic tool and increments registry version.
- `admin/tools/update` changes an existing dynamic tool and increments registry version.
