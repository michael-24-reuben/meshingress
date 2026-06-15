# Meshingress MCP POST API Request Reference

**Base URL:** `http://100.121.15.11:4737`  
**Primary route:** `POST /mcp`  
**Protocol shape:** JSON-RPC 2.0 over HTTP POST  
**Content-Type:** `application/json`

This document is derived from Meshingress test/client request examples. It focuses on **POST** requests only. Non-POST flows such as `GET /mcp`, `DELETE /mcp`, `OPTIONS /mcp`, `/v3/api-docs`, and
WebSocket usage are intentionally omitted.

## Shared request conventions

All normal calls use this JSON-RPC envelope:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "method/name",
  "params": { }
}
```

Use this shell variable for the examples:

```bash
BASE_URL="http://100.121.15.11:4737"
```

Admin-only methods require:

```bash
-H "Authorization: Bearer dev-admin"
```

Expected responses are shown as stable fragments. Some fields, arrays, generated metadata, or tool-specific data may vary by runtime state, installed tools, configuration, and backend availability.

---

## Public MCP protocol calls

### 1. Initialize server capabilities

Initializes the MCP session and returns server identity plus supported capabilities. The tests expect the server name to be `meshingress` unless overridden by identity properties, and tool-list change
notifications to be enabled.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2025-11-25",
      "capabilities": {},
      "clientInfo": {
        "name": "test-client",
        "version": "0.1.0"
      }
    }
  }'
```

Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2025-11-25",
    "capabilities": {
      "tools": {
        "listChanged": true
      },
      "meshingress": {
        "instanceId": "dev-node-01",
        "environment": "dev",
        "nodeRole": "edge-ingress",
        "publicBaseUrl": "http://...",
        "websocketEnabled": true,
        "websocketPath": "/mcp/ws"
      }
    },
    "serverInfo": {
      "name": "meshingress",
      "version": "0.1.0",
      "instanceId": "dev-node-01",
      "environment": "dev",
      "nodeRole": "edge-ingress",
      "publicBaseUrl": "http://..."
    }
  }
}
```

When configured with custom identity properties, expected identity can include:

```json
{
  "result": {
    "serverInfo": {
      "name": "test-meshingress",
      "instanceId": "test-node"
    }
  }
}
```

---

### 2. List available tools

Lists public, currently exposed tool functions. This endpoint is the discovery contract for dynamic clients. Each returned tool describes the function name, display metadata, argument schema, and operational annotations needed to safely build a `tools/call` request.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/list",
    "params": {}
  }'
```

Expected response shape:

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "tools": [
      {
        "name": "tool.function",
        "title": "Human-readable title",
        "description": "Human-readable tool description.",
        "inputSchema": {
          "type": "object",
          "additionalProperties": false,
          "description": "Optional argument-object description.",
          "properties": {
            "argumentName": {
              "type": "string",
              "description": "Human-readable argument description."
            }
          },
          "required": [
            "argumentName"
          ]
        },
        "annotations": {
          "readOnlyHint": true,
          "destructiveHint": false,
          "idempotentHint": true,
          "scopes": [
            "SCOPE_NAME"
          ]
        }
      }
    ]
  }
}
```

#### Tool object schema

Each entry in `result.tools[]` follows this shared shape:

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": [
    "name",
    "inputSchema"
  ],
  "properties": {
    "name": {
      "type": "string",
      "description": "Stable tool function identifier. This value is passed to tools/call as params.name."
    },
    "title": {
      "type": "string",
      "description": "Optional display title for UI clients."
    },
    "description": {
      "type": "string",
      "description": "Optional human-readable summary of the tool function."
    },
    "inputSchema": {
      "type": "object",
      "description": "JSON Schema object describing the params.arguments payload accepted by this tool."
    },
    "annotations": {
      "type": "object",
      "description": "Optional metadata describing safety hints, idempotency, and required scopes."
    }
  }
}
```

#### Input schema contract

`inputSchema` describes the object passed into `tools/call.params.arguments`.

```json
{
  "type": "object",
  "additionalProperties": false,
  "description": "Optional description of the argument object.",
  "properties": {
    "fieldName": {
      "type": "string",
      "description": "Field-specific description."
    }
  },
  "required": [
    "fieldName"
  ]
}
```

Common argument field types include:

```json
{ "type": "string" }
```

```json
{ "type": "integer" }
```

```json
{ "type": "boolean" }
```

```json
{ "type": "array" }
```

```json
{ "type": "object" }
```

String fields may also include an enum:

```json
{
  "type": "string",
  "enum": [
    "pending",
    "active",
    "blocked",
    "resolved",
    "archived"
  ]
}
```

#### Annotation contract

`annotations` contains optional execution metadata. Clients should treat these fields as advisory unless their own security model requires stricter enforcement.

```json
{
  "readOnlyHint": true,
  "destructiveHint": false,
  "idempotentHint": true,
  "scopes": [
    "SHELL_EXECUTE",
    "FILES_WRITE"
  ]
}
```

| Field             |         Type | Meaning                                                                    |
|-------------------|-------------:|----------------------------------------------------------------------------|
| `readOnlyHint`    |      boolean | Indicates the tool is expected to read data without modifying state.       |
| `destructiveHint` |      boolean | Indicates the tool may perform destructive operations.                     |
| `idempotentHint`  |      boolean | Indicates repeated calls with the same arguments should be safe or stable. |
| `scopes`          | string array | Lists capability scopes required or associated with the tool.              |

#### Dynamic client behavior

A dynamic client should:

1. Call `tools/list`.
2. Render one callable item per `result.tools[]` entry.
3. Use `title` as the display label when present; otherwise use `name`.
4. Use `description` as helper text.
5. Generate the argument form from `inputSchema.properties`.
6. Mark fields in `inputSchema.required` as mandatory.
7. Reject unknown argument fields when `inputSchema.additionalProperties` is `false`.
8. Show confirmation prompts for sensitive scopes such as shell execution, file writes/deletes, network access, external API writes, or message publishing.
9. Submit execution through `tools/call` using the selected tool `name` and collected `arguments`.

Example `tools/call` payload generated from a discovered schema:

```json
{
  "jsonrpc": "2.0",
  "id": 21,
  "method": "tools/call",
  "params": {
    "name": "tool.function",
    "arguments": {
      "argumentName": "value"
    }
  }
}
```

#### Policy behavior

Tool visibility is policy-controlled. `tools/list` only returns tools currently exposed to the caller. Disabled, denied, private, or unavailable functions may be omitted.

A client should treat `tools/list` as authoritative. If a function is missing from `result.tools[*].name`, the client should not show it as callable.

---

## Public tool calls

### 3. Call `architect.entries.list`

Returns architect entries filtered by status. The test asserts a successful tool result and at least one resolved entry when the configured architect root has matching data.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "architect.entries.list",
      "arguments": {
        "status": "resolved"
      }
    }
  }'
```


Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "isError": false,
    "content": [
      {
        "type": "text",
        "text": "Found 23 architect entries."
      }
    ],
    "structuredContent": {
      "entries": [
        {
          "id": "2026-05-17-mcp-http-tool-registry",
          "status": "resolved",
          "path": "architect\\resolved\\2026-05-17-mcp-http-tool-registry",
          "title": "MCP HTTP Tool Registry",
          "tags": [
            "tools"
          ]
        }
      ]
    }
  }
}
```

The `entries` array is expected to contain at least one item in the test fixture. If `meshingress.dispatch.include-generated-at=false`, `result._meta.generatedAt` should be absent.

---

### 4. Call `helloworld.greet`

Runs the attached Hello World module with a name argument.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 21,
    "method": "tools/call",
    "params": {
      "name": "helloworld.greet",
      "arguments": {
        "name": "Meshingress"
      }
    }
  }'
```

Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 21,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "Hello, Meshingress!"
      }
    ],
    "structuredContent": {
      "message": "Hello, Meshingress!"
    },
    "isError": false,
    "_meta": {
      "generatedAt": "2026-06-15T14:16:04.706166900Z"
    }
  }
}
```

If the tool is deny-listed, the call returns a JSON-RPC error:

```json
{
  "jsonrpc": "2.0",
  "id": 21,
  "error": {
    "code": -32602
  }
}
```

---

## Webtoon downloader tool calls

### 8. Export Webtoon metadata

Runs the Webtoon downloader in metadata-export mode. The configured server clamps excessive concurrency values to configured maxima. In the test configuration, `concurrentChapters: 99` is clamped to
`2`, and `concurrentPages: 99` is clamped to `7`.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 502,
    "method": "tools/call",
    "params": {
      "name": "webtoon.export_metadata",
      "arguments": {
        "url": "https://www.webtoons.com/en/action/omniscient-reader/list?title_no=2154",
        "outputSubdirectory": "M:\MEDIA_VAULT\40_Manhwa_Manga_Comics\Manhwa",
        "exportFormat": "json",
        "start": 1,
        "end": 2,
        "concurrentChapters": 99,
        "concurrentPages": 99
      }
    }
  }'
```

Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 502,
  "result": {
    "isError": false,
    "structuredContent": {
      "ok": true,
      "command": [
        "--export-metadata",
        "--concurrent-chapters",
        "2",
        "--concurrent-pages",
        "7"
      ],
      "outputFiles": [
        "metadata.json"
      ]
    }
  }
}
```

---

### 9. Reject Webtoon path traversal / unsafe output directory

Rejects output subdirectories that escape the configured Webtoon output root.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 503,
    "method": "tools/call",
    "params": {
      "name": "webtoon.download_series",
      "arguments": {
        "url": "https://www.webtoons.com/en/fantasy/example/list?title_no=1234",
        "outputSubdirectory": "../outside"
      }
    }
  }'
```

Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 503,
  "result": {
    "isError": true,
    "structuredContent": {
      "message": "must be inside the configured output root"
    }
  }
}
```

---

## Test-only annotated dispatch example

### 10. Call `test/echo`

[//]: # (Find reason for outuput `{"jsonrpc":"2.0","id":30,"error":{"code":-32601,"message":"Method not found"}}`. Check if the test handler is registered and the method name matches the handler's `@McpToolMapping`.)

This request documents the annotation-based dispatch framework. It is only available when the test handler is registered.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 30,
    "method": "test/echo",
    "params": {
      "name": "annotation"
    }
  }'
```

Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 30,
  "result": {
    "name": "annotation",
    "method": "test/echo"
  }
}
```

A JSON-RPC notification omits `id`. The test expects HTTP 204 with an empty body:

```bash
curl -i -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "test/echo",
    "params": {
      "name": "notification"
    }
  }'
```

Expected HTTP result:

```text
HTTP/1.1 204 No Content
```

---

## Error and protocol behavior

### 11. Invalid JSON-RPC version

The server returns protocol errors in a JSON-RPC response body while still using HTTP 200 in the tests.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "1.0",
    "id": 4,
    "method": "ping"
  }'
```

Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "error": {
    "code": -32600,
    "message": "jsonrpc must be \"2.0\""
  }
}
```

---

### 12. Invalid JSON body

Malformed JSON returns a JSON-RPC parse error.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -d '{'
```

Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": null,
  "error": {
    "code": -32700,
    "message": "Parse error"
  }
}
```

---

### 13. Public `tools/register` is not exposed

The public `tools/register` method is intentionally unavailable. Registration is handled through admin role methods.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 40,
    "method": "tools/register",
    "params": {
      "artifactId": "sample"
    }
  }'
```

Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 40,
  "error": {
    "code": -32601,
    "message": "Method not found"
  }
}
```

---

## Admin role/tool registry operations

The following methods are role-gated and require:

```bash
-H "Authorization: Bearer dev-admin"
```

Without admin authorization, `roles/tools/list` is expected to return a forbidden JSON-RPC error.

### 14. List role/tool registrations

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dev-admin" \
  -d '{
    "jsonrpc": "2.0",
    "id": 35,
    "method": "roles/tools/list",
    "params": {
      "includeDisabled": true,
      "includePrivate": true
    }
  }'
```

Expected response fragment after registering `voicebox.speak` as a bundle tool:

```json
{
  "jsonrpc": "2.0",
  "id": 35,
  "result": {
    "registrations": [
      {
        "toolId": "voicebox.speak",
        "phase": "bundle"
      }
    ]
  }
}
```

Unauthorized expected fragment:

```json
{
  "error": {
    "code": -32003
  }
}
```

---

### 15. Check a proposed tool descriptor

Validates a descriptor for a dynamic alias without installing it.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dev-admin" \
  -d '{
    "jsonrpc": "2.0",
    "id": 6,
    "method": "roles/tools/check",
    "params": {
      "tool": {
        "name": "architect.entries.copy",
        "title": "List Architect Entries Alias",
        "description": "List architect entries through a dynamic alias.",
        "enabled": true,
        "visibility": "public",
        "handlerKey": "architect.entries.list",
        "inputSchema": {
          "type": "object",
          "properties": {
            "status": {
              "type": "string"
            }
          },
          "additionalProperties": false
        }
      }
    }
  }'
```

Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 6,
  "result": {
    "valid": true,
    "errors": [ ],
    "warnings": [ ],
    "normalized": {
      "name": "architect.entries.copy",
      "visibility": "public",
      "enabled": true,
      "functions": [
        {
          "name": "architect.entries.copy",
          "title": "List Architect Entries Alias",
          "description": "List architect entries through a dynamic alias.",
          "inputSchema": {
            "type": "object",
            "properties": {
              "status": {
                "type": "string"
              }
            },
            "additionalProperties": false
          }
        }
      ]
    }
  }
}
```

---

### 16. Create an alias for an existing tool handler

Registers a public alias that points at an existing handler key.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dev-admin" \
  -d '{
    "jsonrpc": "2.0",
    "id": 7,
    "method": "roles/tools/alias",
    "params": {
      "tool": {
        "name": "architect.entries.copy",
        "title": "List Architect Entries Alias",
        "description": "List architect entries through a dynamic alias.",
        "enabled": true,
        "visibility": "public",
        "handlerKey": "architect.entries.list",
        "inputSchema": {
          "type": "object",
          "properties": {
            "status": {
              "type": "string"
            }
          },
          "additionalProperties": false
        }
      }
    }
  }'
```

Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 7,
  "result": {
    "aliased": true,
    "version": 1
  }
}
```

---

### 17. Update a registered alias/tool descriptor

Patches mutable tool metadata.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dev-admin" \
  -d '{
    "jsonrpc": "2.0",
    "id": 8,
    "method": "roles/tools/update",
    "params": {
      "name": "architect.entries.copy",
      "patch": {
        "description": "List architect entries through a dynamic alias.",
        "enabled": true
      }
    }
  }'
```

Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 8,
  "result": {
    "updated": true,
    "previousVersion": 1,
    "version": 2
  }
}
```

---

### 18. Disable/delete a registered alias/tool

Disables an alias or handles a phase-registration deletion lifecycle depending on how the tool was registered.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dev-admin" \
  -d '{
    "jsonrpc": "2.0",
    "id": 9,
    "method": "roles/tools/delete",
    "params": {
      "name": "architect.entries.copy",
      "mode": "disable"
    }
  }'
```

Expected response fragment for alias disable:

```json
{
  "jsonrpc": "2.0",
  "id": 9,
  "result": {
    "disabled": true,
    "version": 3
  }
}
```

Expected response fragment for bundle phase registration deletion:

```json
{
  "result": {
    "deleted": true,
    "restartRequired": true,
    "registrations": [
      {
        "status": "reconciled-deleted"
      }
    ]
  }
}
```

---

### 19. Reload runtime tools

Reports whether runtime reload is supported. The current tested MVP response is unsupported.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dev-admin" \
  -d '{
    "jsonrpc": "2.0",
    "id": 38,
    "method": "roles/tools/reload"
  }'
```

Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 38,
  "result": {
    "reloaded": false,
    "supported": false
  }
}
```

---

## Phase-aware tool registration

`roles/tools/register` is the admin-only registration entry point. The OpenAPI tests describe its params as **phase-aware registration params**.

### 20. Reject descriptor-only registration payload

The old descriptor-style payload is invalid for `roles/tools/register`.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dev-admin" \
  -d '{
    "jsonrpc": "2.0",
    "id": 33,
    "method": "roles/tools/register",
    "params": {
      "tool": {
        "name": "architect.entries.copy",
        "title": "List Architect Entries Alias",
        "description": "List architect entries through a dynamic alias.",
        "enabled": true,
        "visibility": "public",
        "handlerKey": "architect.entries.list"
      }
    }
  }'
```

Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 33,
  "error": {
    "code": -32602,
    "message": "roles/tools/register requires phase-aware registration params."
  }
}
```

---

### 21. Register experimental local JAR

Registers a local JAR as an experimental runtime tool. Requires local-JAR registration to be enabled and the JAR path to be under the configured local JAR root.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dev-admin" \
  -d '{
    "jsonrpc": "2.0",
    "id": 100,
    "method": "roles/tools/register",
    "params": {
      "phase": "experimental",
      "toolId": "helloworld.text",
      "replace": true,
      "localJar": {
        "path": "sample-module-0.0.1-SNAPSHOT-all.jar",
        "checksumSha256": "<expected-sha256>"
      }
    }
  }'
```

Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 100,
  "result": {
    "registered": true,
    "phase": "experimental",
    "registeredFunctions": [
      "helloworld.text"
    ]
  }
}
```

Checksum mismatch expected fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 110,
  "error": {
    "code": -32602,
    "data": {
      "errorCode": "TOOL_REGISTRATION_CHECKSUM_MISMATCH"
    }
  }
}
```

---

### 22. Register staging Maven coordinates

Registers a staged tool from Maven coordinates.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dev-admin" \
  -d '{
    "jsonrpc": "2.0",
    "id": 200,
    "method": "roles/tools/register",
    "params": {
      "phase": "staging",
      "toolId": "helloworld.text",
      "replace": true,
      "maven": {
        "groupId": "dev.mrk.toolspace",
        "artifactId": "sample-module",
        "version": "0.0.1-SNAPSHOT"
      }
    }
  }'
```

Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 200,
  "result": {
    "registered": true,
    "phase": "staging",
    "registeredFunctions": [
      "helloworld.text"
    ]
  }
}
```

---

### 23. Register bundled/classpath tool

Reconciles a bundled tool that already exists on the server classpath.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dev-admin" \
  -d '{
    "jsonrpc": "2.0",
    "id": 30,
    "method": "roles/tools/register",
    "params": {
      "phase": "bundle",
      "toolId": "voicebox.speak",
      "bundle": {
        "bundleId": "meshingress-tool-bundle"
      }
    }
  }'
```

Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 30,
  "result": {
    "registered": true,
    "phase": "bundle",
    "sourceKind": "CLASSPATH_BUNDLE",
    "status": "reconciled",
    "registeredFunctions": [
      "voicebox.speak"
    ]
  }
}
```

If the bundle tool is not present on the classpath:

```json
{
  "jsonrpc": "2.0",
  "id": 31,
  "error": {
    "code": -32602,
    "data": {
      "errorCode": "BUNDLE_TOOL_NOT_PRESENT"
    }
  }
}
```

---

### 24. Register bundle using local JAR + Maven coordinates

The sample tests also exercise a bundle registration with `bundle`, `localJar`, and `maven` parameters together.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dev-admin" \
  -d '{
    "jsonrpc": "2.0",
    "id": 300,
    "method": "roles/tools/register",
    "params": {
      "phase": "bundle",
      "toolId": "helloworld.text",
      "bundle": {
        "bundleId": "meshingress-tool-bundle"
      },
      "localJar": {
        "path": "sample-module-0.0.1-SNAPSHOT-all.jar",
        "checksumSha256": "<expected-sha256>"
      },
      "maven": {
        "groupId": "dev.mrk.toolspace",
        "artifactId": "sample-module",
        "version": "0.0.1-SNAPSHOT"
      }
    }
  }'
```

Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 300,
  "result": {
    "registered": true,
    "phase": "bundle",
    "registeredFunctions": [
      "helloworld.text"
    ]
  }
}
```

---

### 25. Register native server-core tool

Registers a native/server-core tool. The sample test enables native HTTP registration; the controller test also verifies that native registration is forbidden by default.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dev-admin" \
  -d '{
    "jsonrpc": "2.0",
    "id": 400,
    "method": "roles/tools/register",
    "params": {
      "phase": "native",
      "toolId": "meshingress.runtime.info",
      "nativeTool": {
        "namespace": "meshingress.runtime"
      }
    }
  }'
```

Expected response fragment when native HTTP registration is enabled:

```json
{
  "jsonrpc": "2.0",
  "id": 400,
  "result": {
    "registered": true,
    "phase": "native",
    "registeredFunctions": [
      "meshingress.runtime.info"
    ]
  }
}
```

Expected response fragment when native HTTP registration is disabled:

```json
{
  "jsonrpc": "2.0",
  "id": 32,
  "error": {
    "code": -32003,
    "data": {
      "errorCode": "TOOL_REGISTRATION_PHASE_DISABLED"
    }
  }
}
```

---

## Publication-record installation

`roles/tools/installPublication` installs a repository publication record after validating signature, trust state, checksums, revocation status, scopes, and local scope policy. The publication object
is large and should normally be generated by the repository/publication subsystem.

### 26. Install an approved publication record

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dev-admin" \
  -d '{
    "jsonrpc": "2.0",
    "id": 500,
    "method": "roles/tools/installPublication",
    "params": {
      "toolId": "helloworld.text",
      "publication": {
        "coordinate": {
          "groupId": "dev.mrk.tools",
          "artifactId": "sample-module",
          "version": "0.0.1-SNAPSHOT",
          "classifier": null,
          "extension": "jar"
        },
        "type": "GENERATED_TOOL_MODULE",
        "trustStatus": "APPROVED_LIMITED",
        "artifactUri": "meshingress-repository://artifact/dev.mrk.tools/sample-module/0.0.1-SNAPSHOT/sample-module-0.0.1-SNAPSHOT-all.jar",
        "artifactChecksum": {
          "algorithm": "sha256",
          "value": "<repository-artifact-sha256>"
        },
        "scopePolicy": {
          "requestedScopes": ["USER_WRITE"],
          "approvedScopes": ["USER_WRITE"],
          "installableScopes": ["USER_WRITE"],
          "deniedScopes": []
        },
        "scanSummary": {
          "status": "clean",
          "tools": ["cyclonedx-sbom", "bytecode-scope-scanner"],
          "findingCount": 0,
          "metadata": {
            "test": true
          }
        },
        "provenance": null,
        "revoked": false,
        "publishedAt": "2026-05-29T00:00:00-04:00",
        "signatureAlgorithm": "HmacSHA256",
        "signature": "<publication-signature>"
      }
    }
  }'
```

Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 500,
  "result": {
    "installed": true,
    "sourceKind": "PUBLICATION_RECORD",
    "registeredFunctions": [
      "helloworld.text"
    ]
  }
}
```

Then validate the installed function:

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 501,
    "method": "tools/call",
    "params": {
      "name": "helloworld.text",
      "arguments": {}
    }
  }'
```

Expected response fragment:

```json
{
  "result": {
    "isError": false
  }
}
```

---

### 27. Publication install rejection cases

The tests cover these rejection branches:

| Condition                                            | Expected error fragment                                                         |
|------------------------------------------------------|---------------------------------------------------------------------------------|
| Invalid signature                                    | `"Publication record signature is invalid."`                                    |
| Unsigned publication                                 | `"Publication record is unsigned."`                                             |
| Repository checksum mismatch                         | `"repository artifact checksum mismatch"`                                       |
| Revoked publication                                  | `"Publication record is revoked."`                                              |
| Non-installable trust status, for example `REJECTED` | `"Publication record is not in an installable trust state."`                    |
| Missing approved function scope                      | `"Tool function helloworld.text scope not approved in publication: USER_WRITE"` |
| Publication approves a locally disabled scope        | `"Publication record approves disabled scope: SHELL_EXECUTE"`                   |

Generic curl shape:

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dev-admin" \
  -d '{
    "jsonrpc": "2.0",
    "id": 510,
    "method": "roles/tools/installPublication",
    "params": {
      "toolId": "helloworld.text",
      "publication": {
        "...": "publication record under test"
      }
    }
  }'
```

Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 510,
  "error": {
    "code": -32003,
    "message": "Publication record signature is invalid."
  }
}
```

---

### 28. Install approved PowerShell publication

Installs the PowerShell CLI tool module from an approved publication record. This flow requires dangerous scopes to be locally enabled in configuration, including shell execution and file deletion.

```bash
curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dev-admin" \
  -d '{
    "jsonrpc": "2.0",
    "id": 700,
    "method": "roles/tools/installPublication",
    "params": {
      "toolId": "cli.powershell",
      "publication": {
        "coordinate": {
          "groupId": "dev.mrk.toolspace",
          "artifactId": "powershell-cli",
          "version": "0.0.1-SNAPSHOT",
          "classifier": null,
          "extension": "jar"
        },
        "type": "GENERATED_TOOL_MODULE",
        "trustStatus": "APPROVED_LIMITED",
        "artifactUri": "meshingress-repository://artifact/dev.mrk.toolspace/powershell-cli/0.0.1-SNAPSHOT/powershell-cli-0.0.1-SNAPSHOT.jar",
        "artifactChecksum": {
          "algorithm": "sha256",
          "value": "<repository-artifact-sha256>"
        },
        "scopePolicy": {
          "requestedScopes": ["SHELL_EXECUTE", "FILES_WRITE"],
          "approvedScopes": ["FILES_DELETE", "FILES_WRITE", "SHELL_EXECUTE"],
          "installableScopes": ["FILES_DELETE", "FILES_WRITE", "SHELL_EXECUTE"],
          "deniedScopes": []
        },
        "scanSummary": {
          "status": "findings",
          "tools": ["cyclonedx-sbom", "bytecode-scope-scanner"],
          "findingCount": 10,
          "metadata": {
            "reviewed": true
          }
        },
        "provenance": null,
        "revoked": false,
        "publishedAt": "2026-06-14T00:00:00-04:00",
        "signatureAlgorithm": "HmacSHA256",
        "signature": "<publication-signature>"
      }
    }
  }'
```

Expected response fragment:

```json
{
  "jsonrpc": "2.0",
  "id": 700,
  "result": {
    "installed": true,
    "sourceKind": "PUBLICATION_RECORD",
    "registeredFunctions": [
      "cli.powershell.execute"
    ]
  }
}
```

---

## Request/response model notes

### JSON-RPC success

```json
{
  "jsonrpc": "2.0",
  "id": "<same id as request>",
  "result": { }
}
```

### JSON-RPC error

```json
{
  "jsonrpc": "2.0",
  "id": "<same id as request, or null for parse-level failures>",
  "error": {
    "code": -32602,
    "message": "Invalid params",
    "data": {
      "errorCode": "OPTIONAL_APPLICATION_ERROR_CODE"
    }
  }
}
```

### Tool execution result

Tool calls use JSON-RPC success even when the tool itself reports a domain/runtime failure:

```json
{
  "result": {
    "isError": true,
    "structuredContent": {
      "ok": false,
      "message": "Tool-specific failure"
    }
  }
}
```

Protocol and registry failures use JSON-RPC `error` instead.

---

## Error code fragments observed

| Scenario                                               | Expected code / data                                           |
|--------------------------------------------------------|----------------------------------------------------------------|
| Invalid JSON-RPC envelope                              | `error.code = -32600`                                          |
| Malformed JSON                                         | `error.code = -32700`                                          |
| Method not found, such as public `tools/register`      | `error.code = -32601`                                          |
| Invalid params, such as registration checksum mismatch | `error.code = -32602`                                          |
| Forbidden/admin/policy rejection                       | `error.code = -32003`                                          |
| Registration checksum mismatch                         | `error.data.errorCode = "TOOL_REGISTRATION_CHECKSUM_MISMATCH"` |
| Missing bundled classpath tool                         | `error.data.errorCode = "BUNDLE_TOOL_NOT_PRESENT"`             |
| Disabled registration phase                            | `error.data.errorCode = "TOOL_REGISTRATION_PHASE_DISABLED"`    |

---

## Operational cautions

- `tools/list` is the source of truth for tool names and argument schemas.
- `tools/call.params.arguments` is tool-specific and should be generated from each tool descriptor returned by `tools/list`.
- Admin role methods should not be exposed to untrusted clients.
- Publication install is security-sensitive: signature, checksum, trust status, revocation, scope approval, and local scope policy must all pass.
- Dangerous scopes such as `SHELL_EXECUTE`, `FILES_DELETE`, and `FILES_WRITE` should remain disabled unless the installed tool is explicitly trusted and reviewed.
- Runtime registration behavior depends on server configuration:
    - `meshingress.tools.registration.enabled`
    - `meshingress.tools.registration.allow-experimental`
    - `meshingress.tools.registration.allow-staging`
    - `meshingress.tools.registration.allow-bundle`
    - `meshingress.tools.registration.allow-native-http`
    - `meshingress.tools.registration.local-jar-root`
    - `meshingress.tools.registration.bundle-pom-path`
