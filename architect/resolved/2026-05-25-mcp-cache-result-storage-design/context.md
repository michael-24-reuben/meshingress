# Context

## Main Subject

```txt
lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/McpCacheResult.java
```

The annotation is intended to let tool authors opt into result caching at the function method level.

Current fields include:

```txt
enabled
ttlMs
namespace
keyPrefix
includeArguments
excludeArguments
includeToolId
includeFunctionName
includePrincipal
includeSession
cacheErrors
cacheEmptyResults
keyMode
storage
```

## Design Direction

The annotation should remain small and declarative. Runtime behavior should be implemented in the framework using a storage abstraction.

## MVP Storage Decision

Local filesystem storage is the preferred MVP backend.

Rationale:

- requires no external services
- works for local development
- survives process restarts
- easy to inspect during debugging
- sufficient for single-node Meshingress deployments
- keeps implementation pressure low while cache semantics stabilize

## Storage Path Direction

Use a `.cache` directory, but avoid placing arbitrary keys directly into paths.

Recommended layout:

```txt
.cache/meshingress/cache/tools/<namespace>/<hash-prefix>/<sha256>.json
```

## Security Notes

- Do not store secrets in cache keys.
- Do not put raw argument values in filenames.
- Include principal/session in keys when cache entries could otherwise leak user-specific results.
- Framework-managed caching should not require every cached tool to declare explicit cache scopes.
- Cache inspection/deletion tools should require cache scopes.

## Future Development

Database, Redis, and distributed cache options should be treated as future backend implementations of the same `McpCacheStore` contract.

The MVP should avoid design choices that prevent later database or distributed storage.
