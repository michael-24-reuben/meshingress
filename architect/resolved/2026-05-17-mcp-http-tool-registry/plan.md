# Plan

## Phase 1: Static MCP Tool Server

Implement the minimum useful MCP server.

### HTTP

```http
POST /mcp
GET  /mcp    # return 405 for now
DELETE /mcp # optional no-op/session termination
```

### MCP Methods

```txt
initialize
notifications/initialized
ping
tools/list
tools/call
```

### Components

```txt
McpController
McpDispatcher
JsonRpcValidator
McpSessionService
McpMethodRouter
ToolRegistry
ToolExecutor
```

### Tool Registry

Start with in-process static registration from Spring beans.

```txt
@Component tools register themselves at startup.
tools/list reads from ToolRegistry.
tools/call dispatches to ToolExecutor.
```

## Phase 2: Validation and Policy

Add stronger checks before dynamic mutation.

- Validate `tools/call` arguments against `inputSchema`.
- Validate output against `outputSchema` when present.
- Add tool-level authorization.
- Add timeout per tool.
- Add audit log for tool calls.
- Add tool annotations for read-only/destructive/idempotent hints.

## Phase 3: Admin Tool Registry Methods

Add custom JSON-RPC methods over `POST /mcp`.

```txt
admin/tools/check
admin/tools/register
admin/tools/update
admin/tools/delete
admin/tools/list
admin/tools/reload
```

Do not expose these as REST paths.

## Phase 4: Persistent Dynamic Registry

Persist dynamic tool descriptors.

Suggested tables:

```txt
mcp_tool_definition
mcp_tool_version
mcp_tool_audit_event
```

Registry entries should include:

- name
- version
- enabled
- visibility
- title
- description
- input schema
- output schema
- annotations
- handler key
- created/updated timestamps

## Phase 5: SSE and Notifications

Implement `GET /mcp` as an SSE endpoint.

Use it for:

- `notifications/tools/list_changed`
- progress notifications
- cancellation-related updates

## Phase 6: Resources

After tools are stable, expose architect files as MCP resources.

Example resource URIs:

```txt
architect://entries
architect://entries/{entryId}
architect://entries/{entryId}/brief
architect://entries/{entryId}/context
architect://entries/{entryId}/todo
architect://entries/{entryId}/plan
architect://entries/{entryId}/summary
```
