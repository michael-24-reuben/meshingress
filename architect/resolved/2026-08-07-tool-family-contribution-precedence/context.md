# Context

## Established Direction

- A tool family has one canonical, delimiter-free namespace: `youtube`, not `cli.powershell`.
- Callable function paths remain dotted and begin with the family name: `youtube.video.metadata` and `youtube.video.download`.
- Tool artifacts have their own immutable registration identity (`toolId` or equivalent). That identity is not the public family name.
- Each artifact declares `ToolModuleMetadata` for its own contribution under that namespace. An additional artifact is a contribution to a family, not a source-code subclass and not a metadata merge that can rewrite the selected primary contribution's identity.
- The registry, not artifact-authored manifest data, owns whether a contribution is active, its precedence, and whether it may become a fallback when a primary contribution is unavailable.

## Current Contract Gap

`McpToolManifestDefinition` exposes a standalone `toolId()` plus top-level links. `McpToolNativeMetadata` serializes that string but does not bind it to an MCP descriptor or a persisted registration identity. The manifest API currently has no structured public tool metadata, icon resource, contribution relationship, or persisted conflict-resolution contract.

`McpToolNativeMetadata` presently contains an exploratory CSV-style `execution` comment. A comma-separated ID list must not become the persisted model because it cannot provide foreign-key integrity, ordered-row auditing, duplicate prevention, or independent enable/disable lifecycle.

## Required Behavior

For a family execution order of `tool001` before `tool005`:

- both contributions may add unique functions to `youtube`;
- `tool001` owns `youtube.video.metadata` if both declare it;
- the conflicting `tool005` function is skipped with a structured `FUNCTION_CONFLICT_IGNORED` event that identifies both contributions;
- `tool005` remains partially active when it still owns unique functions such as `youtube.video.download`;
- if `tool001` is disabled or removed, `tool005` may claim formerly conflicting paths only when an administrator-approved fallback policy permits it;
- arrival order must not affect the result.
