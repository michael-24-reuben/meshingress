# Plan

## 1. Keep Annotation as Policy Surface

Treat `McpCacheResult.java` as declarative policy only. It should not know how filesystem, memory, database, or distributed cache stores work.

## 2. Add Cache SPI

Introduce framework-level types similar to:

```java
public interface McpCacheStore {
    Optional<McpCachedValue> get(McpCacheKey key);
    void put(McpCacheKey key, McpCachedValue value);
    void delete(McpCacheKey key);
}
```

Suggested supporting records/classes:

```txt
McpCacheKey
McpCachedValue
McpCachePolicy
McpCacheKeyGenerator
McpCacheManager
McpCacheStoreResolver
```

## 3. Implement MVP Stores

MVP stores:

```txt
NoOpMcpCacheStore
InMemoryMcpCacheStore
FileSystemMcpCacheStore
```

Future stores:

```txt
JdbcMcpCacheStore
RedisMcpCacheStore
DistributedMcpCacheStore
```

## 4. Add Configuration

Add a `cache` group to `MeshingressProperties`:

```txt
meshingress.cache.enabled=true
meshingress.cache.default-storage=file
meshingress.cache.directory=.cache/meshingress/cache
meshingress.cache.default-ttl=5m
meshingress.cache.max-ttl=1h
meshingress.cache.max-entry-size=1MB
meshingress.cache.max-total-size=256MB
meshingress.cache.create-directories=true
meshingress.cache.cleanup-on-startup=true
```

## 5. Dispatch Integration

Wrap function invocation in cache logic:

```txt
resolve tool/function
resolve cache annotation + config
build cache key
read cache
  hit -> return cached result
  miss -> invoke function
check cacheability
write cache
return result
```

Caching should live in the framework dispatch path, not inside individual tools.

## 6. Filesystem Entry Format

Use hashed filenames:

```txt
.cache/meshingress/cache/tools/<namespace>/<first-2-hash-chars>/<sha256>.json
```

Store metadata and serialized result JSON:

```json
{
  "schemaVersion": "mcp-cache-v1",
  "createdAt": "2026-05-25T00:00:00Z",
  "expiresAt": "2026-05-25T00:05:00Z",
  "namespace": "example",
  "key": "sha256...",
  "result": {
    "content": [],
    "structuredContent": {},
    "_meta": {}
  }
}
```

## 7. Future Development Path

After MVP is stable:

1. Add JDBC schema and `JdbcMcpCacheStore`.
2. Add cache metrics.
3. Add namespace purge APIs.
4. Add optional admin/cache-management tool guarded by `CACHE_READ`/`CACHE_DELETE` scopes.
5. Add Redis/distributed store if multi-node deployments require shared cache.
