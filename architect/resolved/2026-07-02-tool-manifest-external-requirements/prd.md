# PRD: Tool Manifest External Requirements

## Goal

Define a class-based Meshingress tool manifest contract that lets a tool declare properties, requirements, links, and readme text outside the executable tool class while still producing static artifact metadata that repository assessment and runtime install can consume.

## User Value

Tool authors should be able to keep execution logic in the tool class and declaration logic in a dedicated manifest class.

Repository and runtime consumers should be able to inspect, resolve, audit, and clean up requirements without parsing README text or loading untrusted uploaded tool code.

## Requirements

### R1: Typed Manifest Contract

Add a public API contract such as `McpToolManifestDefinition` under the manifest API module. The contract should expose:

- `toolId()`
- `properties()`
- `requirements()`
- `links()`
- `readmeMarkdown()`

The exact package should match the final module shape, but it should live with `meshingress-tool-api-manifest`, not server internals.

### R2: Tool Property Model

Add a fluent or immutable `ToolProperty` model that can represent:

- string values
- long/integer values
- boolean values if needed
- secret references
- default values
- required/optional flags
- human-readable descriptions

Secret values must not be embedded in manifests. Manifest properties should reference secret names or secret providers, such as `env:WEATHER_API_KEY`.

### R3: Tool Requirement Model

Add a structured `ToolRequirement` model that supports at least:

- executable requirements
- source repository requirements
- external API requirements
- scope requirements

Scope requirements should reuse the existing `McpToolScope` surface rather than duplicating scope risk metadata.

### R4: Structured Links

Add `ToolLink` or equivalent support for source, documentation, API docs, downloads, terms, license, and related links.

Links must be structured data, not only README hyperlinks.

### R5: Static Export

The repository must be able to inspect manifest output without executing arbitrary uploaded code. The manifest implementation should therefore support a static export artifact such as:

```txt
META-INF/meshingress/tool-manifest.json
resources/application.yaml
resources/README.md
README.md
```

The exact file name can change during implementation, but the static manifest must contain machine-readable properties, requirements, links, and readme text.

### R6: Compatibility With Existing Resources

The current external resource behavior must keep working:

- property defaults are exported to `resources/application.yaml`
- README text is exported to `README.md`
- README text is also exported to `resources/README.md`
- runtime install preserves the sibling `resources/` directory

Class-based manifests should extend this behavior instead of removing it in the first slice.

### R7: Source Repository Requirements

Source repositories should be first-class requirements. They must use canonical repository identity based on normalized URL host/path.

Equivalent URLs such as these should resolve to the same identity:

```txt
https://github.com/yt-dlp/yt-dlp
https://github.com/yt-dlp/yt-dlp.git
git@github.com:yt-dlp/yt-dlp.git
```

Preferred canonical identity:

```txt
github.com/yt-dlp/yt-dlp
```

Preferred managed local path:

```txt
${sourceRoot}/github.com/yt-dlp/yt-dlp
```

The label is display-only and must not define the local path.

### R8: Shared Source Reuse

Repository/runtime persistence should eventually track source repository usage so Meshingress can:

- detect already resolved sources
- reuse managed source directories across tools
- know which tools reference a source
- avoid deleting shared source code still required by another tool
- clean up unused managed sources safely

The first implementation may defer resolver execution, but the manifest schema should not make shared source tracking impossible.

### R9: Checkout Awareness

The source repository model should leave room for checkout refs, tags, branches, or commit hashes. MVP may resolve one checkout per canonical repository, but the data model should not block later checkout-specific resolution.

### R10: Migration Path

Existing `@McpToolProperty` and `@McpToolReadme` declarations should remain temporarily supported while manifest classes are introduced and migrated.

## Non-Goals

- Do not redesign SQL storage wholesale in this entry.
- Do not implement external source cloning before the manifest contract is stable.
- Do not require all bundled tools to migrate in the first slice.
- Do not change MCP dispatch or tool execution behavior.
- Do not make `java.util.ResourceBundle` the manifest abstraction. It may be used internally for localized text only.

## Acceptance Criteria

- A dedicated manifest class can declare properties, requirements, links, and readme text.
- The repository can read static manifest output without loading uploaded tool classes.
- Existing exported `resources/application.yaml` and README behavior remains intact.
- Source repositories are represented as structured requirements with canonical URL identity.
- API credential needs are represented as secret-reference properties plus external API requirements.
- Scope requirements use the current `McpToolScope` model.
- The PowerShell CLI tool can be migrated as the first pilot without unrelated weather or `yt-dlp` requirements in its final manifest.
- Tests cover annotation compatibility, class-manifest export, repository assessment export, and runtime resource installation.
