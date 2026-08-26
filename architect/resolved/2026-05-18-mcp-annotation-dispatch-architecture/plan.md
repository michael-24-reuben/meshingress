# Plan

## Architecture Shape

```txt
JSON-RPC MCP request
  -> method string + params + McpCallContext
  -> McpMethodControllerComposite
       -> manual McpMethodController beans
       -> AnnotationMcpMethodController
            -> McpDispatchRegistry
            -> HandlerMethodInvoker
            -> McpDispatchArgumentResolver chain
```

## Package Layout

Suggested package structure:

```txt
dev.mrk.meshingress.controller.dispatch
├─ annotation
│  ├─ McpDispatchMapping.java
│  ├─ McpDispatchMethod.java
│  ├─ McpDispatchParam.java
│  └─ McpSchema.java
│
├─ invoker
│  ├─ AnnotationMcpMethodController.java
│  ├─ McpDispatchRegistry.java
│  ├─ McpDispatchHandlerMethod.java
│  ├─ McpDispatchMethodScanner.java
│  ├─ McpHandlerMethodInvoker.java
│  └─ McpReturnValueAdapter.java
│
├─ resolver
│  ├─ McpDispatchArgumentResolver.java
│  ├─ McpCallContextArgumentResolver.java
│  ├─ McpMethodNameArgumentResolver.java
│  ├─ RawParamsArgumentResolver.java
│  ├─ McpDispatchParamArgumentResolver.java
│  └─ TypedJsonArgumentBinder.java
│
└─ schema
   ├─ McpSchemaDescriptor.java
   ├─ McpSchemaRegistry.java
   └─ McpSchemaExtractor.java
```

## Annotation Definitions

### `@McpDispatchMapping`

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpDispatchMapping {
    String value();
}
```

### `@McpDispatchMethod`

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpDispatchMethod {
    String value();
}
```

### `@McpDispatchParam`

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpDispatchParam {
    String value();
    Class<?> implementation() default Void.class;
    boolean required() default true;
}
```

### `@McpSchema`

```java
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface McpSchema {
    Class<?> value();
}
```

## Parameter Sources

Initial supported sources:

| Source | Resolves To |
|---|---|
| `params` | full JSON-RPC params node |
| `args` | `params.arguments`, defaulting to `{}` when allowed |
| `name` | `params.name` |
| `context` | `McpCallContext` |
| `method` | raw MCP method string |

## Binding Rules

1. If parameter type is `McpCallContext`, inject context by type.
2. If parameter has `@McpDispatchParam`, resolve the named source.
3. If `implementation != Void.class`, bind JSON into that implementation class.
4. If parameter type is assignable from the implementation class, pass the implementation instance.
5. If no implementation is declared:
   - pass `JsonNode` directly for `JsonNode` parameters,
   - pass `ObjectNode` only when the node is an object,
   - coerce strings/numbers/booleans for scalar parameters,
   - bind concrete POJOs directly if the parameter type is concrete.
6. `@McpSchema` is recorded as metadata only; it does not decide Java binding.

## Startup Scanning

1. Find Spring beans annotated with `@McpDispatchMapping`.
2. For each method annotated with `@McpDispatchMethod`, compose the full method name.
3. Validate uniqueness of method names.
4. Analyze parameters and select resolvers.
5. Register `McpDispatchHandlerMethod` entries in `McpDispatchRegistry`.

## Invocation Flow

```txt
AnnotationMcpMethodController.dispatch(method, params, context)
  -> registry.get(method)
  -> invoker.resolveArguments(handler, method, params, context)
  -> Method.invoke(bean, args)
  -> returnValueAdapter.toJsonNode(result)
```

## Return Value Rules

Supported initially:

- `JsonNode`
- `ObjectNode`
- `ArrayNode`
- `Map<String, Object>` converted by `ObjectMapper`
- POJO converted by `ObjectMapper`
- `void` converted to empty JSON object or rejected; choose explicitly before implementation

Recommendation: reject `void` in v1 to avoid ambiguous MCP response semantics.

## Migration Strategy

1. Introduce annotations and resolver infrastructure.
2. Add tests using a sample annotation-backed handler.
3. Keep `ToolsMcpController` manual initially.
4. Optionally create `AnnotatedToolsMcpController` as a test-only or experimental implementation.
5. Switch production controllers only after behavior parity tests are in place.
