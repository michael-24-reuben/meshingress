# Fixes

## Files Changed

- `pom.xml`
- `app/meshingress-server/pom.xml`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/ToolRegistry.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/InMemoryToolRegistry.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/runtime/RuntimeToolRegistryBridge.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/runtime/ServerToolModuleHandlerFactory.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/runtime/ToolRuntimeLoaderConfiguration.java`
- `lib/meshingress-tool-runtime-loader/**`

## Behavioral Changes

- Added `lib/meshingress-tool-runtime-loader` to the Maven reactor.
- Added artifact source contracts for Maven coordinates, direct JARs, and plugin directories.
- Added local Maven repository resolution for installed artifacts and local transitive runtime dependencies.
- Added direct local JAR resolution as a fallback source.
- Added runtime lifecycle contracts and status records.
- Added a default runtime loader that resolves an artifact, creates an isolated classloader, starts a child Spring context, discovers handlers, registers them, and can deactivate/unload the module.
- Added a server-side registry bridge so runtime-loaded handlers use the same registry path as startup-discovered handlers.
- Added owner tracking to `InMemoryToolRegistry` so runtime-loaded functions can be deregistered cleanly.
- Added server handler discovery for both direct `McpToolHandler` beans and annotated `@McpTool` beans inside a loaded child context.

## Compatibility Notes

- Existing startup-discovered tools still use the same constructor registration path.
- The runtime registry methods are additive on `ToolRegistry`.
- Existing static tool modules remain valid because runtime activation reads the same Spring Boot `AutoConfiguration.imports` convention.
