# Fixes

- Added schema-two `ToolModuleMetadata`, attribution, links, tags, and artifact-local icon declarations, with schema-one manifest reads retained.
- Added persisted contribution activation modes, precedence, runtime association, and current conflict records to the file-backed registration store.
- Rebuilt dynamic namespace registration from persisted ascending precedence. Duplicate functions are skipped while unique extension functions remain active; pending primary-dependent modules do not claim conflicts.
- Added role-gated `roles/tools/contributions/list` and `roles/tools/contributions/update`; updates validate uniqueness/primary ownership and refresh loaded runtime state.
- Promoted only validated declared icons from the assessed artifact and exposed the coordinate-bound repository `/icon` endpoint. Uniconed tools retain Studio's existing fallback.
- Added manifest-aware annotation scanning: a module manifest provides the namespace, `@McpTool` provides a tool family, and `@McpFunction` provides the callable leaf.
- Rejected dots inside namespace, tool-family, and function values; registry descriptors now require `namespace.tool`, and public functions require `namespace.tool.function`.
- Migrated attached module manifests, annotations, storage identities, Studio samples, API examples, and dynamic descriptor fixtures to the generated three-part names.
