# Implementation Notes

## 2026-08-07 implementation update

- Manifest schema two now serializes `metadata` with a one-segment namespace, authored title/description/authors/license/tags/links, and optional local `ToolIcon`. Schema-one manifests remain readable; a legacy dotted `toolId` migrates to its first segment and must be reviewed for a more precise namespace.
- Runtime cache installation reserves a persisted contribution before activation. The first contribution in a namespace becomes `PRIMARY`; a later unconfigured contribution becomes `REQUIRES_PRIMARY` and cannot promote itself.
- `RuntimeToolRegistryBridge` rebuilds dynamic family registrations by persisted ascending precedence, not activation order. Lower duplicate functions are skipped while unique functions remain active; structured warnings name the namespace, function, owner tool ID, and rejected tool ID.
- Assessment now promotes only the manifest-declared local icon into reviewed `resources/`, after path, type, and size validation. `GET /api/v1/artifact/{groupId}/{artifactId}/{version}/icon` serves that controlled artifact resource with its declared MIME type. Studio can use this coordinate-bound resource and retains its current built-in fallback icon whenever a tool has none; no arbitrary URL is accepted.
- Admin-only MCP methods `roles/tools/contributions/list` and `roles/tools/contributions/update` expose and change persisted precedence and activation modes. A change immediately re-applies the loaded namespace. The list includes the current persisted duplicate-function conflict set.
- The file-backed registration document is now schema three to retain conflicts. Rebuilds replace the current namespace conflict set and do not report conflicts for pending `REQUIRES_PRIMARY` contributions.

## 2026-08-07 namespace, tool, and function resolution

- `McpToolMetadata` resolves the registered or packaged module manifest for an annotated class. Both classpath and dynamically loaded annotated handlers pass its namespace into `McpToolAnnotationScanner`.
- A manifest supplies the module namespace once; individual handler classes supply only `@McpTool` families. This lets multiple handlers in one module share the same namespace without duplicating manifest identity.
- The scanner emits `namespace.tool` descriptors and `namespace.tool.function` public callable names. The registry validates the same two-level descriptor and three-level function shape, including role-gated dynamic descriptors.
- Migrated attached modules include Hello World, PowerShell, YouTube, Transform, Open Ink Library/Toonverse, yt-dlp, and Faster Whisper. Existing contribution precedence continues to resolve ownership independently of these generated names.
