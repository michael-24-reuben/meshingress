# Brief: McpCacheResult Storage Design

## Main Subject Objective

Primary file:

```txt
lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/McpCacheResult.java
```

The objective is to implement the framework behavior behind `@McpCacheResult`, allowing annotated MCP tool functions to cache `DispatchExecutionResult` output according to declarative cache policy.

## Current Direction

Use local filesystem cache storage as the MVP backend.

Recommended MVP path pattern:

```txt
.cache/meshingress/cache/tools/<namespace>/<hash-prefix>/<sha256>.json
```

The implementation should keep storage pluggable from the start so that database, Redis/distributed cache, and other backends can be added later without changing the annotation contract.

## Problem Statement

`McpCacheResult.java` currently declares cache policy fields, but the runtime does not yet implement cache storage, cache key generation, TTL handling, result serialization, or dispatch interception.

The framework needs a consistent caching layer that:

- reads cache policy from `@McpCacheResult`
- creates stable canonical cache keys
- stores and restores serialized `DispatchExecutionResult` payloads
- supports local development with no external infrastructure
- does not permanently lock the framework into filesystem-only storage

## MVP Decision

Filesystem storage is the MVP because it is inspectable, durable across restarts, easy to test locally, and requires no database or external service.

## Deferred Options

The following are future development items, not MVP requirements:

- JDBC/database-backed cache store
- Redis/distributed cache store
- cluster-safe shared cache semantics
- cache metrics and observability dashboards
- admin cache browsing and manual invalidation tools
- advanced eviction by size, LRU, or namespace quotas
- compression/encryption-at-rest for cache entries
