# Fixes

## Files Changed

- `lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/cache/McpCacheStorage.java`
- `lib/meshingress-config/src/main/java/dev/mrk/meshingress/config/MeshingressProperties.java`
- `app/meshingress-server/src/main/resources/application.properties`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/annotation/AnnotatedMcpToolHandler.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/annotation/AnnotatedMcpToolHandlerProvider.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/cache/*`
- `app/meshingress-server/src/test/java/dev/mrk/meshingress/mcp/tools/cache/McpCacheManagerTests.java`
- `pom.xml`
- `app/meshingress-server/pom.xml`
- `toolspace/powershell-cli-tool/pom.xml`

## Behavioral Changes

- `@McpCacheResult` now controls annotated MCP tool result caching.
- Filesystem cache entries use hashed filenames and JSON entry metadata.
- Expired and corrupt filesystem entries are treated as misses.
- Error results are skipped unless `cacheErrors=true`.
- Empty results are skipped when `cacheEmptyResults=false`.
- Included/excluded arguments, principal, and session flags participate in cache key generation.
- Tool modules remain normal dependency jars during reactor packaging; the server module alone produces the executable Boot jar.
