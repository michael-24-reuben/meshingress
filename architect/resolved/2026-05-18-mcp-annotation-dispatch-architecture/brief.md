# Brief

## Goal

Design an optional annotation-based dispatch layer for MCP methods without replacing the existing explicit `McpMethodController` contract.

The current manual controller model uses:

```java
boolean supports(String method);
JsonNode dispatch(String method, JsonNode params, McpCallContext context);
```

The proposed architecture adds annotation-based handler mapping such as:

```java
@McpDispatchMapping("tools")
class ToolsMcpHandler {

    @McpDispatchMethod("call")
    ObjectNode toolsCall(
        @McpDispatchParam(value = "args", implementation = ToolCallArgumentsImpl.class)
        @McpSchema(ToolCallArgumentsSchema.class)
        ToolCallArguments args,
        McpCallContext context
    ) {
        // ...
    }
}
```

## Design Preference

Schema metadata and Java binding should be separate:

```java
@McpDispatchParam(value = "args", implementation = SomeImplementation.class)
@McpSchema(SomeJsonSchemaClass.class)
SomeInterface value
```

This keeps these concerns independent:

- `@McpDispatchParam`: where the value comes from and how it binds into Java.
- `@McpSchema`: what schema should be advertised, enforced, or exposed to callers.

## Scope

In scope:

- Annotation definitions.
- Handler scanning.
- Method registry construction.
- Parameter source resolution.
- Optional typed JSON binding.
- Optional JSON schema metadata extraction.
- Adapter into the existing `McpMethodController` SPI.

Out of scope for the first implementation:

- Replacing manual `dispatch` controllers.
- Full compile-time annotation processing.
- Automatic schema generation from arbitrary Java types.
- Broad validation framework integration beyond basic schema metadata plumbing.

## Existing Context

The existing `ToolsMcpController` manually supports `tools/list` and `tools/call`, validates `params`, extracts `params.name` and `params.arguments`, executes the tool, and returns JSON. The annotation layer should be able to express the same behavior while keeping the manual implementation valid.
