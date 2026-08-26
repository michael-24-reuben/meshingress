# Todo

## Design

- [x] Confirm final annotation names.
- [x] Confirm whether `@McpSchema` targets only parameters or also methods/classes.
- [x] Decide whether composed method paths trim slashes, reject slashes, or normalize both.
- [x] Decide v1 behavior for `void` handler return values.

## Implementation

- [x] Add `@McpDispatchMapping`.
- [x] Add `@McpDispatchMethod`.
- [x] Add `@McpDispatchParam(value, implementation, required)`.
- [x] Add `@McpSchema(value)`.
- [x] Add `McpDispatchArgumentResolver` interface.
- [x] Add context resolver.
- [x] Add method-name resolver.
- [x] Add raw params resolver.
- [x] Add named dispatch-param resolver.
- [x] Add typed JSON binder.
- [x] Add schema metadata extraction.
- [x] Add annotation scanner.
- [x] Add dispatch registry.
- [x] Add handler method invoker.
- [x] Add return value adapter.
- [x] Add annotation-backed `McpMethodController` implementation.

## Tests

- [x] Duplicate method mapping fails at startup.
- [x] Missing required param fails with invalid params.
- [x] Non-object `args` fails when target requires object.
- [x] `McpCallContext` resolves by type.
- [x] `params`, `args`, `name`, and `method` resolve correctly.
- [x] `@McpSchema` metadata is visible without affecting Java binding.
- [x] Manual and annotation-backed controllers can coexist.
