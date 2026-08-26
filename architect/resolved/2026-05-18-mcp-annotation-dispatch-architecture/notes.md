# Notes

## Initial Direction

Keep `McpMethodController` as the stable SPI. Implement annotation dispatch as an adapter that also implements `McpMethodController`.

## Preferred Example

```java
@McpDispatchMapping("tools")
final class ToolsMcpHandler {

    @McpDispatchMethod("call")
    ObjectNode toolsCall(
        @McpDispatchParam(value = "name") String name,
        @McpDispatchParam(value = "args", implementation = ToolCallArgumentsImpl.class)
        @McpSchema(ToolCallArgumentsSchema.class)
        ToolCallArguments args,
        McpCallContext context
    ) {
        // ...
    }
}
```

## Resolver Principle

Each method parameter should be resolved by exactly one resolver. Ambiguous resolver matches should fail during startup validation.

## Schema Principle

`@McpSchema` should be queryable by tooling even if the handler is never invoked. That implies schema metadata should be captured during scanning, not during invocation.
