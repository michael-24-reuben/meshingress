# Summary

The tool-family contribution model is implemented and resolved. `namespace` is now the one-name logical family, while tool artifact IDs remain independent registry identities. Runtime ownership is deterministic from persisted precedence, extensions remain additive, and an administrator controls primary/fallback behavior.

The remaining product work is adoption: individual tool modules may now declare an artifact-local icon, and a future catalog view can present the existing controlled icon endpoint. It does not require another precedence-model change.

## 2026-08-07 naming follow-on

The generated MCP function contract is now `<namespace>.<tool>.<function>`.
Module manifests own the namespace, `@McpTool` owns a module-local family, and
`@McpFunction` owns the leaf. This is implemented for classpath and dynamic
handlers, attached tool modules, registry validation, and user-facing samples.
