# Plan

1. Inspect the live tool registry, registration-store implementations, descriptor/function registration paths, and current persistence migrations to select compatible storage changes.
2. Define `ToolModuleMetadata`, `ToolIcon`, validation rules, native JSON schema evolution, and a backwards-compatible read/migration strategy for existing `tool-manifest.json` files.
3. Replace standalone manifest `toolId()` and top-level `links()` with structured metadata; update manifest executors, exporters, extractors, examples, and focused module tests.
4. Add persisted family contribution and precedence records through the registration-store abstraction, including administrator-only mutation and validation of namespace/tool references.
5. Implement deterministic family/function resolution with partial activation and structured duplicate-function events.
6. Add administrative API visibility for family order, contribution state, conflicts, and fallback eligibility.
7. Export validated icon resources and add a controlled Studio-facing retrieval contract with fallback behavior.
8. Test primary, contributor, pre-primary extension, duplicate function, disable/remove primary, fallback promotion, invalid namespace, icon validation, restart persistence, and legacy-manifest cases.
9. Resolve manifest namespace plus `@McpTool` plus `@McpFunction` identity composition; migrate attached modules and validate generated IDs through the public registry.
