# Context

## Draft Review

The draft direction is sound: a class-based manifest is clearer than stacking resource/property annotations on the executable tool class. It also gives Meshingress a natural home for data that is not execution behavior: properties, requirements, links, readme text, external source repositories, and API credential references.

The main integration concern is safe discovery. Repository assessment currently processes uploaded artifacts in quarantine and reads annotation metadata from `.class` files without loading those classes. A class-based manifest usually requires instantiating code. That is acceptable in trusted build/runtime contexts, but not as the only repository ingestion path for untrusted uploaded JARs.

The architect therefore requires a static export artifact, such as `META-INF/meshingress/tool-manifest.json`, so the repository can inspect manifest data safely.

## Live Source Evidence

Current module state:

- `pom.xml` lists `lib/meshingress-tool-api-manifest`.
- `lib/meshingress-tool-api-manifest/pom.xml` has artifactId `meshingress-tool-api-manifest`, but the display name still says `meshingress-tool-native-metadata`.
- Existing classes still live under `dev.mrk.meshingress.toolmetadata`.
- `toolspace/powershell-cli/pom.xml` depends on `meshingress-tool-api-manifest`.
- `app/meshingress-repository/pom.xml` and `app/meshingress-server/pom.xml` depend on `meshingress-tool-api-manifest`.

Current metadata implementation:

- `McpToolProperty` and `McpToolReadme` are runtime-retained type annotations.
- `McpToolNativeMetadataExtractor` scans quarantined `.class` files with Spring `MetadataReader`.
- `McpToolNativeMetadata` currently stores only properties and readme text.
- `McpToolNativeMetadataExporter` writes `resources/application.yaml`, `README.md`, and `resources/README.md`.
- `McpToolMetadata` reads assigned runtime values from `resources/application.yaml` and readme text from resource files before falling back to annotations.

Repository/runtime flow:

- `ArtifactService.assess(...)` extracts metadata from quarantine, exports resources to the artifact directory, then cleans quarantine.
- `ArtifactService.artifactResourceFile(...)` currently exposes only `application.yaml` and `README.md` under `/resources/{resourceName}`.
- `RepositoryArtifactFetcher` fetches `application.yaml` and `README.md` resource files.
- `RuntimeToolCache` installs the JAR and sibling `resources/` directory.

Related architecture:

- `architect/pending/2026-05-23-runtime-tool-creation-and-registry` already calls for runtime-installed tool manifests with ID, version, scopes, secrets, compatibility, checksum, and signature metadata.
- `architect/pending/2026-05-27-cli-anything-tool-import-flow` already calls for generated tool manifests containing source repository URL, source commit, generator metadata, entrypoints, scopes, schemas, and verification metadata.
- `architect/active/2026-05-27-meshingress-repository-artifact-implementation` is the broad repository parent/backlog and includes artifact manifest/resource concerns.

## Assessment Notes

The draft should keep `@McpToolManifest(PowerShellCliManifest.class)` as a useful authoring link, but not depend on that annotation as the repository's only manifest input.

The untracked `PowerShellCliResources.java` sample is useful design material but should not be treated as ready source. It imports resource classes that do not exist yet in the checked-in API and contains unrelated `yt-dlp` and weather API example requirements inside a PowerShell manifest. Those examples belong in tests/docs, not the production PowerShell pilot.

The current scope enum is in `lib/meshingress-tool-annotations` under `dev.mrk.meshingress.scopes.McpToolScope`. If `meshingress-tool-api-manifest` directly exposes scope requirements using that enum, module dependencies must be checked to avoid accidental cycles.

## Design Decisions To Preserve

- Use manifest classes for authoring clarity.
- Export static manifest data for repository safety.
- Preserve current resource file layout during migration.
- Treat source repository requirements separately from executable requirements.
- Canonicalize source repository identity from URL host/path, not labels.
- Keep secret values out of manifest files.
- Use README for human documentation only, not as the structured requirement source.

## Risks

- Loading manifest classes during repository assessment would create an unsafe execution path for uploaded artifacts.
- Moving packages abruptly could break current toolspace and repository imports.
- Adding source repository persistence too early could sprawl into the broader repository storage design.
- The current fixed `/resources/{resourceName}` endpoint will need a deliberate extension if the static manifest JSON should be downloadable.
