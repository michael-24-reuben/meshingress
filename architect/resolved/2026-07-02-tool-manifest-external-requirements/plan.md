# Plan

## Phase 1: Normalize the Manifest Module

- Finish the module rename from native metadata to API manifest.
- Update the `meshingress-tool-api-manifest` POM display name and description.
- Decide whether the Java package remains `dev.mrk.meshingress.toolmetadata` for compatibility or moves to a manifest-focused package with deprecated bridge types.
- Keep Maven module wiring separate from dispatch and base API modules.

## Phase 2: Add the Public Manifest Model

- Add `McpToolManifestDefinition`.
- Add `ToolProperty`.
- Add `ToolRequirement`.
- Add `ToolLink`.
- Add requirement/link kind enums or sealed models if that fits the repo style.
- Add validation in constructors/builders so blank names, invalid URLs, blank scope requirements, and invalid property names fail early.

## Phase 3: Add Manifest Discovery

Support one or both of these discovery paths:

- A marker annotation on a tool class, such as `@McpToolManifest(PowerShellCliManifest.class)`.
- A service/resource file, such as `META-INF/services/...McpToolManifestDefinition`, for build-time manifest export.

Repository assessment should not instantiate manifest classes from untrusted uploads. If an annotation points to a manifest class, the repository may read that class name from bytecode metadata, but static exported manifest data should be the source of truth for assessed artifacts.

## Phase 4: Add Static Manifest Export

Extend the current native metadata exporter flow so a trusted build/runtime step can emit machine-readable manifest data.

Candidate files:

```txt
META-INF/meshingress/tool-manifest.json
resources/application.yaml
resources/README.md
README.md
```

The JSON manifest should include:

- tool ID
- properties
- requirements
- links
- readme text or readme reference
- manifest schema version

Continue writing `resources/application.yaml` and README files for native/runtime compatibility.

## Phase 5: Repository Assessment Integration

Extend repository assessment so it reads the static manifest output from quarantine or artifact resources and stores relevant manifest evidence.

Current integration points:

- `ArtifactService.assess(...)` already invokes `McpToolNativeMetadataExtractor` and `McpToolNativeMetadataExporter`.
- `McpToolNativeMetadataExtractor` currently scans annotations from quarantined `.class` files.
- `McpToolNativeMetadataExporter` currently writes only property YAML and README resources.
- `ArtifactMetadataStore` currently persists artifact payloads, assessment payloads, publication records, and lifecycle events.

Implementation should fit manifest persistence into the current model instead of adding unrelated SQL structure. If new tables are needed for source-repository reference tracking, keep them focused and justified by cleanup/reuse behavior.

## Phase 6: Runtime Consumption

Extend `McpToolMetadata` or replace it with a manifest-aware runtime reader that can:

- read static manifest JSON
- read assigned property values from `resources/application.yaml`
- expose property metadata and readme text
- later expose requirements and links to install/runtime code

Runtime install already preserves artifact resources through `RepositoryArtifactFetcher` and `RuntimeToolCache`; keep that path intact.

## Phase 7: Source Repository Resolver

Add source repository canonicalization and managed source cache behavior after the manifest model is stable.

Minimum resolver behavior:

- normalize HTTPS and SSH GitHub URLs to `github.com/owner/repo`
- preserve optional checkout ref data
- resolve managed local path under a configured source root
- record tool-to-source references for cleanup safety

Do not make source resolution part of the first PowerShell manifest migration unless a real PowerShell requirement needs it.

## Phase 8: Pilot Migration

Migrate `PowerShellCliTool` first.

Final PowerShell manifest should declare only PowerShell-related requirements:

- `meshingress.powershell.executable`
- `meshingress.powershell.timeout-ms`
- executable requirement for `pwsh` or the configured executable
- scopes already required by the tool
- PowerShell documentation/source links if useful
- PowerShell readme text

Keep `yt-dlp` and weather API examples in tests or documentation only, not in the production PowerShell manifest.

## Phase 9: Verification

Run focused tests before broad reactor checks:

```powershell
.\mvnw.cmd -pl lib\meshingress-tool-api-manifest -am test
.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=ArtifactRepositoryNativeMetadataExportTests,ArtifactRepositoryFlowTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=RuntimeToolCacheTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl toolspace\powershell-cli -am test
```

Broader verification can follow after the pilot compiles cleanly.
