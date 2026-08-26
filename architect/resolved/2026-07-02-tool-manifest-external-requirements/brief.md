# Brief: Tool Manifest External Requirements

## Problem

Meshingress tool metadata is moving from annotations on executable tool classes toward dedicated manifest classes. The immediate example is replacing direct `@McpToolProperty` and `@McpToolReadme` declarations on `PowerShellCliTool` with a separate manifest class such as `PowerShellCliManifest`.

The manifest model should be able to declare more than the current annotation surface:

- runtime properties
- secret-reference properties
- executable requirements
- external source repository requirements
- external API requirements
- scope requirements
- structured links
- readme text

This work must integrate with the current repository/runtime artifact flow instead of becoming only a tool-author convenience API.

## Current Source Assessment

The renamed module now exists as `lib/meshingress-tool-api-manifest`, but its `pom.xml` still has the old display name `meshingress-tool-native-metadata`. Its source package is still `dev.mrk.meshingress.toolmetadata`.

The current implementation is annotation-first:

- `McpToolNativeMetadataExtractor` scans quarantined `.class` files with Spring metadata readers.
- `McpToolNativeMetadata` stores only property definitions and README text.
- `McpToolNativeMetadataExporter` writes only `resources/application.yaml`, root `README.md`, and `resources/README.md`.
- `McpToolMetadata` reads runtime `resources/application.yaml` values and falls back to `@McpToolReadme`.
- `ArtifactService.assess(...)` extracts native metadata during repository assessment and exports resources before cleaning quarantine.
- `ArtifactService.artifactResourceFile(...)` currently exposes only `application.yaml` and `README.md`.
- `RepositoryArtifactFetcher` and `RuntimeToolCache` preserve the sibling `resources/` directory during runtime install.

There is also an untracked work-in-progress sample at `toolspace/powershell-cli/src/main/java/dev/mrk/toolspace/powershellcli/PowerShellCliResources.java`. It illustrates the desired manifest shape, but it is not currently a compilable contract because the referenced resource/manifest API classes are not present in the checked-in API and the public class name does not match the file name.

## Scope

Create a typed Meshingress manifest model for tool authors and repository/runtime consumers.

The first implementation should support properties, requirements, links, and readme text while preserving the existing exported resource behavior for compatibility.

## Boundaries

Do not redesign runtime dispatch, MCP JSON-RPC handling, tool execution, or the repository publication policy in this entry.

Do not execute arbitrary uploaded tool code during repository assessment solely to read manifest data. If manifest classes need to run, they should run in trusted build/runtime contexts and emit static metadata that the repository can inspect safely.

Do not use Markdown as the only source of machine-readable requirements. README remains documentation, not the manifest database.
