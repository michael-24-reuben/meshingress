# Todo

## MVP

- [x] Finalize `McpCacheStorage` enum values: `DEFAULT`, `NONE`, `MEMORY`, `FILE`.
- [x] Define `McpCacheStore` interface.
- [x] Define `McpCacheKey` and `McpCachedValue` records.
- [x] Define cache policy resolver from `@McpCacheResult` + global config.
- [x] Implement canonical argument filtering for `includeArguments` and `excludeArguments`.
- [x] Implement SHA-256 cache key generation.
- [x] Implement `NoOpMcpCacheStore`.
- [x] Implement `InMemoryMcpCacheStore`.
- [x] Implement `FileSystemMcpCacheStore`.
- [x] Add `meshingress.cache.*` configuration properties.
- [x] Integrate cache lookup/write into dispatch path.
- [x] Add TTL expiration behavior.
- [x] Add corrupt-file-as-miss behavior.
- [x] Add tests for cache hit/miss/expiry.
- [x] Add tests for principal/session key isolation.
- [x] Add tests for `cacheErrors` and `cacheEmptyResults`.

## Future

- [ ] Add JDBC/database-backed cache store.
- [ ] Add Redis/distributed cache store.
- [ ] Add cache metrics.
- [ ] Add namespace purge support.
- [ ] Add cache management tool guarded by cache scopes.
- [ ] Add size-based cleanup and/or LRU eviction.
- [ ] Evaluate compression and encryption-at-rest.
