# HTTP Paths

## MCP Transport Paths

These are the only HTTP paths required for the MCP surface right now.

```http
POST   /mcp
GET    /mcp
DELETE /mcp
```

## `POST /mcp`

Primary MCP JSON-RPC transport endpoint.

Responsibilities:

- Accept JSON-RPC 2.0 requests, notifications, and batch messages.
- Dispatch MCP standard methods such as:
  - `initialize`
  - `notifications/initialized`
  - `ping`
  - `tools/list`
  - `tools/call`
- Dispatch server-specific admin methods such as:
  - `admin/tools/check`
  - `admin/tools/register`
  - `admin/tools/update`
  - `admin/tools/delete`
  - `admin/tools/reload`
- Validate request shape.
- Validate session state when sessions are enabled.
- Validate tool arguments against each tool's `inputSchema`.
- Return JSON-RPC success or error responses.

### Request Headers

```http
Content-Type: application/json
Accept: application/json, text/event-stream
Mcp-Session-Id: <session-id>     # required after initialize if sessions are enabled
Authorization: Bearer <token>    # recommended for admin and mutation methods
```

### Response Headers

For `initialize`, if stateful sessions are enabled:

```http
Mcp-Session-Id: <new-session-id>
```

For normal JSON responses:

```http
Content-Type: application/json
```

## `GET /mcp`

Optional server-to-client SSE stream.

MVP behavior:

```http
405 Method Not Allowed
```

Later behavior:

- Open an SSE stream for server-initiated messages.
- Emit notifications such as:
  - `notifications/tools/list_changed`
  - progress notifications
  - cancellation-related updates

## `DELETE /mcp`

Optional session termination endpoint.

MVP behavior:

- Require `Mcp-Session-Id`.
- End the session if it exists.
- Return `202 Accepted` or `204 No Content`.

Example:

```http
DELETE /mcp
Mcp-Session-Id: 01HV...
```

Response:

```http
202 Accepted
```

## Explicit Non-Goals

Do not build public MCP tool-specific HTTP paths such as:

```http
POST /tools/list
POST /tools/call
POST /tools/register
POST /tools/update
POST /tools/check
```

Those operations should be represented as JSON-RPC methods over `POST /mcp`.
