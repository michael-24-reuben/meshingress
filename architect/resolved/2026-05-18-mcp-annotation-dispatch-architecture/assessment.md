# Assessment

## Result

The annotation dispatch architecture was implemented as an additive server-local layer on top of the existing `McpMethodController` contract.

The main remodel was the dispatcher composition. `McpDispatcher` now receives all `McpMethodController` beans and validates declared supported methods at startup, rather than hard-coding the internal, tools, and roles controllers. This makes the annotation adapter visible to normal MCP routing while preserving manual controllers.

## Decisions

- Annotation dispatch lives under `app/meshingress-server`, not `lib/meshingress-tool-api`, because this pass enables server MCP method routing rather than tool-author SPI.
- `@McpDispatchParam` controls runtime source and Java binding.
- `@McpSchema` is collected as metadata only and does not affect binding.
- Class and method path segments are normalized by trimming leading and trailing slashes, while empty path parts are rejected.
- `void` dispatch handlers are rejected during scanning because MCP result semantics should stay explicit.

## Remaining Scope

This is a v1 runtime reflection implementation. It does not add compile-time annotation processing, automatic schema generation, or production migration of existing manual controllers.
