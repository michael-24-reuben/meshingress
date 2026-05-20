# PRD: Optional MCP Annotation Dispatch

## Problem

Manual MCP dispatch is explicit and stable, but repetitive as method count grows. Each controller must manually:

- match method names,
- validate params,
- extract common fields,
- bind JSON to Java values,
- pass context,
- produce JSON responses.

Annotation dispatch can reduce boilerplate while keeping the current controller contract as the canonical low-level SPI.

## Goals

- Allow MCP handlers to declare methods with annotations.
- Preserve manual `McpMethodController` implementations as first-class supported controllers.
- Support parameter source identifiers such as `params`, `args`, `name`, `context`, and `method`.
- Keep Java binding separate from schema metadata.
- Allow incremental adoption per controller or method.
- Avoid magical behavior that is hard to debug.

## Non-Goals

- Do not require all MCP controllers to use annotations.
- Do not remove or deprecate `McpMethodController` initially.
- Do not couple schema declaration to Java binding implementation.
- Do not require compile-time code generation for the first version.

## Requirements

### Functional Requirements

1. A class annotated with `@McpDispatchMapping("tools")` can contain methods annotated with `@McpDispatchMethod("call")`.
2. The composed dispatch method should become `tools/call`.
3. The annotation-backed adapter must implement `McpMethodController`.
4. Method parameters must be resolved by registered `McpDispatchArgumentResolver` implementations.
5. `@McpDispatchParam(value = "args", implementation = X.class)` should resolve a source value and optionally bind it into `X`.
6. `@McpSchema(Y.class)` should attach schema metadata without controlling Java binding.
7. Raw `JsonNode`, `ObjectNode`, scalar Java values, interfaces, and concrete DTO classes should be supportable in stages.
8. Invalid params should produce existing JSON-RPC invalid-param errors.
9. Missing methods should continue to produce existing method-not-found behavior.

### Compatibility Requirements

- Existing manual controllers must continue to work.
- Existing `ToolsMcpController` should not need to be rewritten to enable the new infrastructure.
- Annotation-backed controllers should be additive Spring beans.

### Observability Requirements

- Duplicate method mappings should fail fast at application startup.
- Unsupported parameter shapes should fail fast at application startup when possible.
- Runtime binding errors should include the MCP method name and parameter source.

## Acceptance Criteria

- A sample `@McpDispatchMapping("tools")` handler can implement `tools/list` and `tools/call`.
- The annotation-backed handler can coexist with manual controllers.
- `@McpDispatchParam("args")` can extract `params.arguments`.
- `@McpDispatchParam("name")` can extract `params.name`.
- `McpCallContext` can be injected by type without annotation.
- `@McpSchema(...)` can be discovered by schema/documentation tooling independently of the implementation binding.
