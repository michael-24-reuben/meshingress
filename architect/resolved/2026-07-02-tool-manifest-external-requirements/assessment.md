# Assessment

## Result

Resolved. The annotation-backed `McpToolReadme` and `McpToolProperty` implementation path has been replaced by a class-based manifest API plus static manifest resources that repository assessment and runtime install can consume without executing uploaded tool code.

## Findings

The old implementation mixed executable tool code with packaging metadata:

- `McpToolProperty`, `McpToolProperties`, and `McpToolReadme` required metadata to live as annotations on tool classes.
- `McpToolNativeMetadataExtractor` depended on annotation scanning of quarantined class files.
- Runtime metadata fallback still looked for `@McpToolReadme`.
- Repository resource export only persisted `application.yaml` and README text, so requirements and links had no machine-readable artifact payload.

The new design separates the declaration model from execution:

- Tool authors implement `McpToolManifestDefinition`.
- Manifest data is represented by `ToolProperty`, `ToolRequirement`, and `ToolLink`.
- Build/runtime trusted code can register a manifest object with `McpToolMetadata`.
- Repository assessment reads only static `tool-manifest.json` files from quarantine.
- Export still writes compatibility resources: `resources/application.yaml`, root `README.md`, and `resources/README.md`.

## Residual Risk

Source repository requirements are typed and canonicalized, but managed source repository reuse, local checkout lifecycle, and cleanup safety are not implemented in this slice. That should remain a separate focused architect because it affects storage ownership and deletion policy.
