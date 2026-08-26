# Fixes

## Manifest API

- Added `McpToolManifestDefinition`.
- Added typed manifest records: `ToolProperty`, `ToolRequirement`, and `ToolLink`.
- Added `McpToolManifestJson` for stable static JSON read/write.
- Extended `McpToolNativeMetadata` to carry schema version, tool id, properties, requirements, links, and README text.
- Removed annotation implementations `McpToolProperty`, `McpToolProperties`, and `McpToolReadme`.

## Repository Integration

- Reworked native metadata extraction to read static manifest JSON from quarantine instead of scanning annotation metadata.
- Exported manifest data to both `resources/tool-manifest.json` and `META-INF/meshingress/tool-manifest.json`.
- Preserved existing `resources/application.yaml`, root `README.md`, and `resources/README.md` exports.
- Allowed repository clients to download `resources/tool-manifest.json`.
- Merged manifest-declared `scope` requirements into artifact inferred scopes during assessment.

## Runtime Integration

- Updated runtime resource download to fetch `tool-manifest.json` alongside `application.yaml` and `README.md`.
- Updated `McpToolMetadata` to read static manifest output and to support trusted runtime manifest registration.

## PowerShell Pilot

- Added `PowerShellCliManifest`.
- Removed `McpToolProperty` and `McpToolReadme` annotations from `PowerShellCliTool`.
- Registered the manifest through `PowerShellCliToolAutoConfiguration`.
- Added a static PowerShell manifest resource under `META-INF/meshingress/tool-manifest.json`.

## Tests

- Updated manifest module tests for static manifest files and runtime registration.
- Updated repository flow/export tests for manifest resource export, download, and manifest-declared scope requirements.
- Updated runtime cache tests for manifest resource download.
