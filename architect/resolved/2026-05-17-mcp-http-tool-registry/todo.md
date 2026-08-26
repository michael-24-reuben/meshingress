# Todo

## MCP Endpoint

- [x] Add `McpController` at `/mcp`.
- [x] Implement `POST /mcp` for JSON-RPC messages.
- [x] Make `GET /mcp` return `405 Method Not Allowed` for MVP.
- [x] Implement `DELETE /mcp` for session termination or return a safe no-op response.

## JSON-RPC Core

- [x] Add JSON-RPC request DTOs or `JsonNode` parser layer.
- [x] Validate `jsonrpc: "2.0"`.
- [x] Validate request `id` rules.
- [x] Validate `method` presence.
- [x] Add standard JSON-RPC error responses.
- [x] Decide whether batch requests are supported in MVP.

## MCP Lifecycle

- [x] Implement `initialize`.
- [x] Return server info and capabilities.
- [x] Decide whether to enable stateful sessions immediately.
- [x] Implement `notifications/initialized`.
- [x] Implement `ping`.

## Tool Discovery

- [x] Create `ToolRegistry` abstraction.
- [x] Create internal `McpToolDescriptor` model.
- [x] Register static tools from Spring beans.
- [x] Implement `tools/list`.
- [x] Hide disabled and admin-only tools from normal `tools/list`.

## Tool Calling

- [x] Implement `tools/call`.
- [x] Validate requested tool exists.
- [x] Validate tool is enabled.
- [x] Validate arguments against `inputSchema`.
- [x] Dispatch to handler by `handlerKey`.
- [x] Return MCP tool result with `content`, optional `structuredContent`, and `isError`.

## Tool Checking

- [x] Implement `admin/tools/check`.
- [x] Validate tool name syntax.
- [x] Validate uniqueness rules.
- [x] Validate input schema.
- [x] Validate output schema if present.
- [x] Validate handler key exists or is allowed.
- [x] Return warnings and normalized descriptor.

## Tool Registration and Update

- [x] Implement `admin/tools/register`.
- [x] Implement `admin/tools/update`.
- [x] Implement `admin/tools/delete` as disable/soft-delete first.
- [x] Add admin-only authorization policy.
- [x] Add registry versioning.
- [x] Add audit events for registry mutation.
- [ ] Emit `notifications/tools/list_changed` when SSE support exists.

## Persistence

- [ ] Design `mcp_tool_definition` table.
- [ ] Design `mcp_tool_version` table.
- [ ] Design `mcp_tool_audit_event` table.
- [ ] Add repository/service layer.
- [ ] Add migration strategy if using Flyway/Liquibase later.

## Security

- [ ] Require authentication for `/mcp` in non-local environments.
- [x] Require admin role/scope for `admin/tools/*`.
- [x] Prevent dynamic registration of arbitrary code handlers.
- [ ] Add rate limits or request size limits.
- [ ] Add output sanitization policy for tool results.
