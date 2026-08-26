# Recommended V1 Scope

## Included Routes

```txt
GET    /storage/{accessToken}
HEAD   /storage/{accessToken}
```

## Included Internal Capability

```txt
StorageService.store(InputStream, StorageCreateRequest, StorageCallContext)
```

## Included Features

- one stored entry equals one lease
- dedicated filesystem root
- database-backed metadata and lifecycle state
- streamed ingestion
- per-entry byte ceiling
- aggregate published quota
- separate staging quota
- quota reservation
- temporary-file staging
- atomic publication
- SHA-256 calculation
- exact byte count
- hard TTL
- maximum successful retrieval count
- opaque capability token
- token hash at rest
- `HEAD` without request consumption
- interrupted `GET` remains consumed
- pending deletion after final admitted stream
- safe deletion after active streams close
- startup, periodic, and pre-create cleanup
- filesystem/database reconciliation
- attachment-only delivery by default
- MIME and filename sanitization
- uniform public `404` for unavailable leases
- metrics and sanitized audit events
- predictable `storage.lease/v1` structured content

## Explicitly Deferred

- remote URL ingestion
- asynchronous ingestion jobs
- resumable client uploads
- multiple leases per stored object
- lease rotation
- token renewal
- range requests
- inline delivery for active content
- conditional requests
- CDN integration
- cross-node/shared-storage claims
- public cleanup routes
- HTTP upload and authenticated management routes

## V1 Acceptance Criteria

1. A caller can stream bytes into storage without full memory buffering.
2. Incomplete uploads never become retrievable.
3. Stored bytes and database metadata remain reconcilable after process failure.
4. Entry and aggregate limits are enforced under concurrency.
5. Retrieval never exposes source URLs or local paths.
6. Expired, exhausted, revoked, deleted, and unknown tokens are unavailable.
7. Concurrent final retrievals cannot exceed `maxRequests`.
8. The final admitted stream completes safely before physical deletion.
9. Startup, scheduled, and pre-create cleanup are idempotent.
10. MCP tools can return a stable `storage.lease/v1` node.
11. Raw tokens are absent from logs, traces, metrics, and persisted metadata.
12. No remote network ingestion is possible while the feature is disabled.

## Implementation Authorization Boundary

This scope is a proposed implementation baseline only.

This pending architecture does not authorize module creation, controller creation, route mapping, database migration, scheduler creation, property changes, or source-tool integration. Explicit implementation authorization remains required.
