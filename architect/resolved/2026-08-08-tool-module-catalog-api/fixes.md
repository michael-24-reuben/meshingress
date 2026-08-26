# Implementation

- Added `ToolModuleCatalog` and public `GET /api/v1/tool-modules` endpoints for list, detail, README, and manifest-declared icon resources.
- The detail response keeps metadata, license, property declarations, and requirements inline; README and icon remain typed linked resources. Configured property values and secrets are never returned.
- Classpath manifest beans and runtime-loaded manifests now register through the same catalog. Runtime removal unregisters the corresponding entry.
- Added public GET/OPTIONS CORS support for the catalog so the separately hosted Studio can load it.
- Updated the PowerShell module manifest to publish its bundled SVG and updated the Studio explorer to use a module icon with a resilient `FolderIcon` fallback.
