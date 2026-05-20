# HelloWorldTool Annotation Remap

This package remaps the legacy `McpToolHandler` implementation into an annotation-oriented design.

## Main remap

Legacy shape:

```java
public class HelloWorldTool implements McpToolHandler {
    McpToolDescriptor descriptor();
    ToolExecutionResult call(ObjectNode arguments, McpCallContext context);
}
```

Annotation shape:

```java
@McpTool(...)
@McpSchema(HelloWorldGreetArgsSchema.class)
@McpToolScopes(...)
public class HelloWorldTool {

    @McpDispatchMethod("tools/call")
    public ToolExecutionResult call(
        @McpDispatchParam(value = "args", implementation = DefaultHelloWorldGreetArgs.class)
        @McpSchema(HelloWorldGreetArgsSchema.class)
        HelloWorldGreetArgs arguments,
        McpCallContext context
    ) {
        ...
    }
}
```

## Created support classes

- `McpDispatchMapping`
- `McpDispatchMethod`
- `McpDispatchParam`
- `McpSchema`
- `McpJsonSchemaProvider`
- `McpTool`
- `McpToolScopes`
- `McpToolDescriptorFactory`
- `HelloWorldGreetArgs`
- `DefaultHelloWorldGreetArgs`
- `HelloWorldGreetArgsSchema`

## Binding rule

`@McpDispatchParam(value = "args", implementation = DefaultHelloWorldGreetArgs.class)` means:

```txt
JSON-RPC params.arguments -> DefaultHelloWorldGreetArgs -> HelloWorldGreetArgs parameter
```

`@McpSchema(HelloWorldGreetArgsSchema.class)` means:

```txt
Expose this JSON Schema to callers/tool descriptors.
```

The schema and Java binding are intentionally separate.
