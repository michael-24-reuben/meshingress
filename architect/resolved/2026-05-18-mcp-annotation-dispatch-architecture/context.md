# Context

## Current Controller Shape

The current tools controller is manually dispatched. It declares a supported method set containing `tools/list` and `tools/call`, implements `supports(String method)`, and uses a switch in `dispatch` to route calls.

`tools/call` currently performs these operations manually:

1. Assert `params` is an object.
2. Extract `params.name`.
3. Validate that `name` is present.
4. Extract `params.arguments`.
5. Default missing or null arguments to `{}`.
6. Assert arguments is an object.
7. Execute the tool through `ToolExecutor`.
8. Convert `ToolExecutionResult` to JSON.

The annotation-based architecture should be able to reduce this repeated extraction and validation while preserving the manual controller model.

## User Preference

The user prefers the following separation:

```java
@McpDispatchParam(value = "args", implementation = InterfaceImplementation.class)
@McpSchema(JsonSchemaClass.class)
InterfaceClass params
```

Meaning:

- `@McpDispatchParam` identifies where the argument comes from and how it binds into Java.
- `@McpSchema` identifies the external schema contract.

This is preferred over a single annotation field that tries to mean both schema and implementation.

## Rationale

The split is architecturally clean because schema metadata and runtime Java binding have different consumers:

- dispatch invocation needs source and implementation binding,
- documentation tooling needs schema metadata,
- validation tooling may need schema metadata,
- handler code needs strongly typed Java values.

A schema class may not be the same as the Java implementation class.

## Compatibility Constraint

The project currently uses Spring Boot and Jackson-style JSON node types. Annotation dispatch should integrate with Spring bean scanning and continue returning `tools.jackson.databind.JsonNode` values.

## Risks

- Reflection dispatch can hide errors until runtime unless startup validation is strict.
- Path composition can become confusing if both class and method annotations include slashes.
- Interface binding requires an explicit implementation or a resolver capable of selecting one.
- Schema validation should not silently mutate Java binding semantics.

## Open Questions

1. Should parameter source names be fixed constants or open strings?
2. Should `args` mean only `params.arguments`, or should each MCP method define its own source map?
3. Should `@McpSchema` point to a Java class, a schema provider, or a generated schema descriptor?
4. Should annotation-backed handlers be ordered before or after manual controllers when both support the same method?

Recommendation: duplicate method support should fail fast rather than rely on ordering.
