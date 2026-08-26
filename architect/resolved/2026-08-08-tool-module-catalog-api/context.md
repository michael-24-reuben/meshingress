# Context

`ToolModuleMetadata` already defines one module's namespace, presentation metadata, optional artifact-local icon, properties, requirements, and README. The repository artifact API can serve reviewed artifact resources by coordinate, but the active server has no equivalent public view for bundled/open-source modules.

The Studio explorer groups MCP functions by namespace. Its namespace icon can only render when the active server publishes a safe URL; an artifact-relative `logo.svg` is neither available from a Studio origin nor unique for classpath modules.

Module identity must be separate from namespace because extensions may contribute to the same namespace. The catalog therefore needs a server-owned opaque module identifier and a namespace-to-module relation rather than a `/tools/{namespace}` source of truth.
