# `/mcp` API Contract

## Standard MCP Methods

### `initialize`

Request:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2025-11-25",
    "capabilities": {},
    "clientInfo": {
      "name": "dev-client",
      "version": "0.1.0"
    }
  }
}
```

Response:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2025-11-25",
    "capabilities": {
      "tools": {
        "listChanged": true
      }
    },
    "serverInfo": {
      "name": "architect-mcp",
      "version": "0.1.0"
    }
  }
}
```

### `notifications/initialized`

Request notification:

```json
{
  "jsonrpc": "2.0",
  "method": "notifications/initialized"
}
```

No response.

### `ping`

Request:

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "ping"
}
```

Response:

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {}
}
```

### `tools/list`

Request:

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/list",
  "params": {}
}
```

Response:

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "tools": [
      {
        "name": "architect.entries.list",
        "title": "List Architect Entries",
        "description": "List structured engineering memory entries by status, tag, or text query.",
        "inputSchema": {
          "type": "object",
          "properties": {
            "status": {
              "type": "string",
              "enum": ["pending", "active", "blocked", "resolved", "archived"]
            },
            "tag": {
              "type": "string"
            },
            "query": {
              "type": "string"
            }
          },
          "additionalProperties": false
        },
        "annotations": {
          "readOnlyHint": true,
          "destructiveHint": false,
          "idempotentHint": true
        }
      }
    ]
  }
}
```

### `tools/call`

Request:

```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "method": "tools/call",
  "params": {
    "name": "architect.entries.list",
    "arguments": {
      "status": "active"
    }
  }
}
```

Response:

```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "Found 1 active architect entry."
      }
    ],
    "structuredContent": {
      "entries": [
        {
          "id": "2026-05-17-mcp-http-tool-registry",
          "title": "MCP HTTP Tool Registry",
          "status": "active"
        }
      ]
    },
    "isError": false
  }
}
```

## Custom Admin Methods

These are not standard MCP methods. They are server-specific JSON-RPC methods over `POST /mcp`.

All `admin/tools/*` methods require admin authorization.

### `admin/tools/check`

Validates a tool descriptor without persisting it.

Request:

```json
{
  "jsonrpc": "2.0",
  "id": 10,
  "method": "admin/tools/check",
  "params": {
    "tool": {
      "name": "architect.entries.get",
      "title": "Get Architect Entry",
      "description": "Read one architect entry by id.",
      "enabled": true,
      "visibility": "public",
      "handlerKey": "architect.entries.get",
      "inputSchema": {
        "type": "object",
        "properties": {
          "entryId": {
            "type": "string"
          }
        },
        "required": ["entryId"],
        "additionalProperties": false
      }
    }
  }
}
```

Response:

```json
{
  "jsonrpc": "2.0",
  "id": 10,
  "result": {
    "valid": true,
    "warnings": [],
    "normalized": {
      "name": "architect.entries.get",
      "visibility": "public",
      "enabled": true,
      "inputSchemaDraft": "2020-12"
    }
  }
}
```

### `admin/tools/register`

Registers a new dynamic tool.

Request:

```json
{
  "jsonrpc": "2.0",
  "id": 11,
  "method": "admin/tools/register",
  "params": {
    "tool": {
      "name": "architect.entries.get",
      "title": "Get Architect Entry",
      "description": "Read one architect entry by id.",
      "enabled": true,
      "visibility": "public",
      "handlerKey": "architect.entries.get",
      "inputSchema": {
        "type": "object",
        "properties": {
          "entryId": {
            "type": "string"
          }
        },
        "required": ["entryId"],
        "additionalProperties": false
      }
    }
  }
}
```

Response:

```json
{
  "jsonrpc": "2.0",
  "id": 11,
  "result": {
    "registered": true,
    "toolName": "architect.entries.get",
    "version": 1,
    "registryVersion": 2
  }
}
```

### `admin/tools/update`

Updates a registered dynamic tool.

Request:

```json
{
  "jsonrpc": "2.0",
  "id": 12,
  "method": "admin/tools/update",
  "params": {
    "name": "architect.entries.get",
    "patch": {
      "description": "Read one structured engineering memory entry by id.",
      "enabled": true
    }
  }
}
```

Response:

```json
{
  "jsonrpc": "2.0",
  "id": 12,
  "result": {
    "updated": true,
    "toolName": "architect.entries.get",
    "previousVersion": 1,
    "version": 2,
    "registryVersion": 3
  }
}
```

### `admin/tools/delete`

Disables or soft-deletes a tool.

Request:

```json
{
  "jsonrpc": "2.0",
  "id": 13,
  "method": "admin/tools/delete",
  "params": {
    "name": "architect.entries.get",
    "mode": "disable"
  }
}
```

Response:

```json
{
  "jsonrpc": "2.0",
  "id": 13,
  "result": {
    "deleted": false,
    "disabled": true,
    "toolName": "architect.entries.get",
    "previousVersion": 2,
    "version": 3,
    "registryVersion": 4
  }
}
```

### `admin/tools/list`

Lists public, private, disabled, and admin-only tools for administrators.

Request:

```json
{
  "jsonrpc": "2.0",
  "id": 14,
  "method": "admin/tools/list",
  "params": {
    "includeDisabled": true,
    "includePrivate": true
  }
}
```

Response:

```json
{
  "jsonrpc": "2.0",
  "id": 14,
  "result": {
    "registryVersion": 4,
    "tools": []
  }
}
```

### `admin/tools/reload`

Reloads registry state from persistent storage or static bean registrations.

Request:

```json
{
  "jsonrpc": "2.0",
  "id": 15,
  "method": "admin/tools/reload"
}
```

Response:

```json
{
  "jsonrpc": "2.0",
  "id": 15,
  "result": {
    "reloaded": true,
    "registryVersion": 5
  }
}
```
