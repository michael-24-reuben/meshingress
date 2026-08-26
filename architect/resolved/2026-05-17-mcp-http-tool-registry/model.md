# Internal Model

## Tool Descriptor

```json
{
  "name": "architect.entries.create",
  "title": "Create Architect Entry",
  "description": "Create a new structured engineering memory entry.",
  "version": 1,
  "enabled": true,
  "visibility": "public",
  "handlerKey": "architect.entries.create",
  "inputSchema": {
    "type": "object",
    "properties": {
      "title": {
        "type": "string"
      },
      "status": {
        "type": "string",
        "enum": ["pending", "active"]
      },
      "tags": {
        "type": "array",
        "items": {
          "type": "string"
        }
      }
    },
    "required": ["title"],
    "additionalProperties": false
  },
  "outputSchema": {
    "type": "object",
    "properties": {
      "entryId": {
        "type": "string"
      },
      "path": {
        "type": "string"
      }
    },
    "required": ["entryId"]
  },
  "annotations": {
    "readOnlyHint": false,
    "destructiveHint": false,
    "idempotentHint": false
  }
}
```

## Java Interfaces

```java
public interface ToolRegistry {
    List<McpToolDescriptor> listPublicEnabledTools();
    List<McpToolDescriptor> listAdminTools(ToolListFilter filter);
    Optional<McpToolDescriptor> findEnabledTool(String name);
    ToolCheckResult check(ToolDefinition definition, ToolCheckMode mode);
    ToolRegistrationResult register(ToolDefinition definition, Actor actor);
    ToolUpdateResult update(String name, ToolPatch patch, Actor actor);
    ToolDeleteResult delete(String name, ToolDeleteMode mode, Actor actor);
    long registryVersion();
}
```

```java
public interface ToolExecutor {
    McpToolResult execute(String toolName, JsonNode arguments, McpCallContext context);
}
```

```java
public interface ToolHandler {
    String handlerKey();
    McpToolResult call(JsonNode arguments, McpCallContext context);
}
```

## Suggested Dispatch Flow

```txt
McpController
  -> McpDispatcher
    -> JsonRpcValidator
    -> McpSessionService
    -> McpMethodRouter
      -> InitializeHandler
      -> PingHandler
      -> ToolsListHandler
      -> ToolsCallHandler
      -> AdminToolsCheckHandler
      -> AdminToolsRegisterHandler
      -> AdminToolsUpdateHandler
      -> AdminToolsDeleteHandler
    -> ToolRegistry
    -> ToolExecutor
```

## `tools/call` Execution Flow

```txt
POST /mcp
  parse JSON
  validate JSON-RPC envelope
  validate session if required
  route method tools/call
  extract params.name and params.arguments
  find tool descriptor
  verify tool is enabled
  verify caller is authorized
  validate arguments against inputSchema
  find handler by handlerKey
  execute handler with timeout
  validate output if outputSchema exists
  write audit event
  return MCP tool result
```

## Error Handling

Protocol-level errors should use JSON-RPC errors.

Examples:

- parse error
- invalid request
- method not found
- invalid params
- internal error

Tool execution/business errors should usually return a successful JSON-RPC response with an MCP tool result containing:

```json
{
  "isError": true,
  "content": [
    {
      "type": "text",
      "text": "Human-readable error message."
    }
  ]
}
```
