# MCP Tool Module Lifecycle — `meshingress-tool-api-manifest`

## Package Role

This package reads, represents, and exports static tool metadata for packaged tool modules.

## User-Visible Contribution

Repository review and runtime installation can determine a tool's identifier, properties, requirements, and links before activating the module.

## Position in the Feature Path

```text
tool-manifest.json or class metadata
  -> McpToolNativeMetadataExtractor
  -> repository review / runtime loader
  -> install or activation decision
```

## Entry Points

- `McpToolManifestDefinition` and `McpToolNativeMetadata`.
- `McpToolNativeMetadataExtractor` and `McpToolNativeMetadataExporter`.
- Artifact-directory resolvers and `ToolProperty`, `ToolRequirement`, and `ToolLink`.

## Feature Contract

```yaml
toolMetadata:
  toolId: string
  properties: list
  requirements: list
  links: list
source: static manifest or extracted class metadata
```

## Dependencies

- Upstream: tool annotations and packaged tool artifacts.
- Downstream: `ArtifactService` uses the extractor during review; `meshingress-tool-runtime-loader` consumes manifest requirements for provisioning.

## Failure Behavior

Malformed or missing metadata prevents accurate review/provisioning and is handled by the caller's repository or loader policy.

## Verification

```powershell
.\mvnw.cmd -pl lib/meshingress-tool-api-manifest -am test
```

## Evidence and Open Questions

Confirmed by the extractor/exporter and `McpToolMetadataTests`. Manifest search locations are determined by the calling resolver, not by a package-owned resource.
