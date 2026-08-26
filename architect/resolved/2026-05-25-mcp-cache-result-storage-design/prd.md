# PRD: Declarative Result Cache for MCP Tool Functions

## Goal

Implement `@McpCacheResult` as a declarative annotation for caching MCP tool function results.

## Primary Subject

```txt
lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/McpCacheResult.java
```

## Requirements

### Functional Requirements

- Detect `@McpCacheResult` on MCP function methods.
- Skip caching when `enabled=false`.
- Resolve TTL from annotation and global defaults.
- Support annotation fields:
  - `ttlMs`
  - `namespace`
  - `keyPrefix`
  - `includeArguments`
  - `excludeArguments`
  - `includeToolId`
  - `includeFunctionName`
  - `includePrincipal`
  - `includeSession`
  - `cacheErrors`
  - `cacheEmptyResults`
  - `keyMode`
  - `storage`
- Generate stable cache keys from canonical JSON input.
- Cache serialized `DispatchExecutionResult` values.
- Return cached results on cache hits.
- Treat expired or corrupt entries as cache misses.
- Use local filesystem storage for MVP.
- Keep backend pluggability through a `McpCacheStore` abstraction.

### MVP Storage Requirements

- Store cache entries under a configurable directory.
- Default to a local `.cache/meshingress/cache` directory for development.
- Write entries atomically using temp file + move.
- Use SHA-256 filenames, not raw argument values.
- Organize files by namespace and hash prefix.
- Delete expired entries opportunistically or on startup.

### Non-Goals for MVP

- Database-backed storage.
- Redis or distributed cache coordination.
- Cross-node cache invalidation.
- Admin UI or cache browser.
- LRU eviction.
- Encryption-at-rest.
- Compression.
- Cache warming.

## Acceptance Criteria

- A method annotated with `@McpCacheResult(ttlMs = 60000)` returns a cached result for repeated calls with identical cache keys.
- Expired entries are not returned.
- Error results are not cached unless `cacheErrors=true`.
- Empty results are cached by default unless `cacheEmptyResults=false`.
- Cache keys change when included arguments change.
- Cache keys do not change when excluded arguments change.
- Principal/session are included only when their annotation flags are enabled.
- Filesystem cache survives process restart.
- Corrupt cache files do not fail tool execution.
- Storage backend selection is abstracted behind a cache store interface.
