# Code Architecture

This is the implementation-ready design for the accepted v1 scope. It deliberately does not create any runtime files, routes, database objects, or dependencies.

## 1. Module and Dependency Boundary

Add one new reactor library when implementation is authorized:

```txt
lib/meshingress-storage-api              artifactId: meshingress-storage-api
  contains the stable producer-facing Java API only

app/meshingress-server
  contains the storage implementation, JDBC schema/store, filesystem store,
  Spring configuration, scheduler, HTTP controller, and route error adapter
```

The server must depend on `meshingress-storage-api`. A source-tool module that elects to publish bytes, such as `x-open-ink-library`, depends only on `meshingress-storage-api`; it must never depend on `meshingress-server`, Spring MVC, JDBC, or the filesystem implementation.

```txt
x-open-ink-library ─┐
other tool modules ─┼──> meshingress-storage-api <── meshingress-server
                    │                                  ├─ JDBC/H2/PostgreSQL
                    │                                  ├─ filesystem
                    │                                  └─ /storage controller
                    └── no dependency on server
```

Do not create a second storage implementation library in v1. The implementation is only meaningful inside the server process because it owns the database transaction manager, configured storage root, HTTP route, and scheduled cleanup. Keeping it server-owned avoids exposing Spring or persistence types as tool SPI.

The root reactor receives `lib/meshingress-storage-api`; `app/meshingress-server/pom.xml` receives the API dependency. A future tool opts in directly. Neither `app/meshingress-tool-bundle` nor an existing tool needs a dependency change until that tool actually stores content.

## 2. Stable Producer API

Package: `dev.mrk.meshingress.storage.api`.

```java
public interface StorageService {
    StorageLease store(
            InputStream content,
            StorageCreateRequest request,
            StorageCallContext context
    );
}
```

`store` reads incrementally and never materializes the entire content in heap. The caller owns and closes `content`; `StorageService` consumes it but does not close a caller-supplied stream. It either returns one fully published lease or throws `StorageException`; it never returns a partial lease.

| Type | Required fields / role |
|---|---|
| `StorageCreateRequest` | `mimeType`, `suggestedFilename`, optional `ttl`, optional `maxRequests`. No paths, URLs, raw headers, or source credentials. |
| `StorageCallContext` | `producerId`, optional `toolId`, optional `sessionId`, optional `requestId`. It is a sanitized value object, not an MCP or HTTP request object. |
| `StorageLease` | `storageId`, server-relative `url`, `mimeType`, `byteSize`, `createdAt`, `expiresAt`, `remainingRequests`, `StorageChecksum`, `suggestedFilename`; serializes naturally to the accepted `storage.lease/v1` node. |
| `StorageChecksum` | fixed `sha-256` algorithm and lowercase hexadecimal digest. |
| `StorageException` | unchecked exception carrying a closed `StorageErrorCode`; no physical path or raw token may appear in its public message. |

`StorageLease` is the tool-facing result. Tool code may embed its values in `structuredContent`, but the API module does not depend on the MCP result classes or on Jackson. The first tool integration creates `StorageCallContext` from its existing `McpCallContext` and its fixed tool operation name; that conversion stays in the tool module.

`StorageErrorCode` is closed for v1: `INVALID_REQUEST`, `CONTENT_TOO_LARGE`, `STAGING_QUOTA_EXCEEDED`, `PUBLISHED_QUOTA_EXCEEDED`, `ENTRY_LIMIT_EXCEEDED`, `STORAGE_UNAVAILABLE`, and `WRITE_FAILED`. It is an internal-producer error contract, not an HTTP representation.

## 3. Server Package Layout and Bean Ownership

All following classes live under `app/meshingress-server/src/main/java/dev/mrk/meshingress/storage/`.

```txt
storage/
  config/
    MeshingressStorageConfiguration
    StorageScheduler
  domain/
    StorageEntry, StorageState, StoragePolicy, StoragePolicyEvaluator
    StorageAdmission, StorageCleanupCandidate, StorageNameSanitizer
  store/
    StorageByteStore, FileSystemStorageByteStore, StoragePathLayout
    StorageMetadataStore, JdbcStorageMetadataStore, StorageSchemaInitializer
  service/
    DefaultStorageService, StorageRetrievalService, StorageDeletionService,
    StorageCleanupCoordinator, StorageTokenHasher
  web/
    StorageController, StorageRouteExceptionHandler
```

`MeshingressStorageConfiguration` is the sole bean assembly point and is conditional on `meshingress.storage.enabled=true`. It creates the filesystem store, JDBC metadata store, policy evaluator, token hasher, public `StorageService`, retrieval/deletion/cleanup services, scheduler, and controller. The implementation classes are constructor-injected; no static service locator is permitted.

`StorageService` is the only producer bean exposed outside the server package. `StorageByteStore` and `StorageMetadataStore` are server implementation SPIs, allowing deterministic unit tests to substitute fakes without teaching tool modules about persistence.

`StorageScheduler` uses `@Scheduled(fixedDelayString = "${meshingress.storage.cleanup-interval}")`; `@EnableScheduling` belongs in `MeshingressStorageConfiguration`, not in an unrelated application class. Its only action is the bounded cleanup coordinator. Startup cleanup is an `ApplicationRunner` bean in the same configuration and runs before the server is advertised as ready.

## 4. Typed Configuration

Extend `MeshingressProperties` with `@Valid @NotNull Storage storage` and a null-safe `Storage.defaults()` constructor path. Update every direct test construction of the root record for the added parameter; callers using property binding remain source-compatible.

The nested `Storage` record owns these implementation properties:

```txt
meshingress.storage.enabled
meshingress.storage.root
meshingress.storage.max-entry-size
meshingress.storage.max-published-bytes
meshingress.storage.max-staging-bytes
meshingress.storage.max-entries
meshingress.storage.max-concurrent-retrievals
meshingress.storage.default-ttl
meshingress.storage.max-ttl
meshingress.storage.default-max-requests
meshingress.storage.max-requests
meshingress.storage.staging-ttl
meshingress.storage.cleanup-interval
meshingress.storage.cleanup-batch-size
meshingress.storage.sql.schema
meshingress.storage.sql.table-prefix
meshingress.storage.sql.table.entries
meshingress.storage.sql.table.usage
meshingress.storage.sql.table.events
meshingress.storage.sql.initialize-schema
```

Initial defaults: root `.cache/meshingress/storage`; entry ceiling `256MB`; published ceiling `1GB`; staging ceiling `256MB`; `1,024` entries; `32` concurrent streams; default/max TTL `30m`/`24h`; default/max requests `1`/`50`; staging TTL `15m`; cleanup every `5m`; batch size `100`; SQL schema `meshingress`, prefix `storage_`, and schema initialization enabled. `StoragePolicyEvaluator` clamps requested TTL/request count to the configured maxima and rejects nonpositive values. These are deployment-tunable limits, not hard-coded policy hidden in services.

Keep these keys in `app/meshingress-server/src/main/resources/application.properties`. Do not create or import `storage.properties`: it would split a generic server capability away from the existing typed `meshingress.*` configuration surface.

## 5. Persistence and Filesystem Layout

Use the existing JDBC/H2/PostgreSQL support but do not reuse repository tables or `SqlArtifactMetadataStore`. Storage lifecycle and quota contention are independent of durable artifact publication.

`StorageSchemaInitializer` follows the existing repository convention: validates schema/table identifiers, performs idempotent `create schema if not exists` and `create table if not exists`, then narrowly additive `alter table ... add column if not exists` only for future compatible additions. It does not use JPA or Flyway in v1.

### Tables

`storage_entries` is one entry/one lease:

| Column | Notes |
|---|---|
| `storage_id varchar(64) primary key` | opaque `stg_` management identifier, never a path. |
| `access_token_sha256 char(64) not null unique` | SHA-256 of raw 256-bit capability; raw token is never persisted. |
| `state varchar(48) not null` | lifecycle enum below. |
| `object_key varchar(256) not null unique` | relative object key only. |
| `mime_type varchar(255) not null`, `suggested_filename varchar(255)` | normalized metadata. |
| `byte_size bigint`, `checksum_sha256 char(64)` | populated only on publication. |
| `remaining_requests integer not null`, `active_streams integer not null` | counters updated only through guarded SQL methods. |
| `created_at`, `published_at`, `expires_at`, `updated_at` | `timestamp with time zone`; `published_at`/size/checksum are null during staging. |
| `producer_id`, `tool_id`, `session_id`, `request_id` | sanitized provenance; no header blobs or credentials. |
| `delete_attempts integer not null`, `last_error_code varchar(64)` | retry/reconciliation diagnostics only. |

Indexes: unique token hash and object key; `(state, expires_at)` for expiry cleanup; `(state, updated_at)` for stale staging/delete retries; `(expires_at)` for bounded scanning.

`storage_usage` contains exactly one `scope_key='global'` row with `published_bytes`, `staging_bytes`, `entry_count`, and `active_streams`. It is initialized transactionally and locked/conditionally updated for global quota and retrieval-concurrency decisions. `storage_events` is append-only, with identity `event_id`, `storage_id`, event type, previous/new state, byte/request deltas, sanitized producer/request ids, and timestamp. It never stores a raw URL, raw token, or filesystem path.

Physical files are never named from caller data and absolute paths are never stored in SQL:

```txt
${meshingress.storage.root}/
  .staging/{storageId}.part
  objects/{first-two-id-chars}/{next-two-id-chars}/{storageId}.blob
```

`StoragePathLayout` derives both paths from a validated internal ID, normalizes the result, and verifies containment under the configured absolute root. `FileSystemStorageByteStore` creates the staging file and final directory on the same volume, writes with `CREATE_NEW`, flushes and closes before `ATOMIC_MOVE`, and treats a missing atomic-move capability as a hard publication failure rather than silently exposing a copy-in-progress. Suggested filenames are metadata only.

## 6. State Machine, Transactions, and Failure Recovery

```txt
STAGING --publish--> AVAILABLE
STAGING --failure/stale--> FAILED
AVAILABLE --expiry--> EXPIRED_PENDING_DELETE
AVAILABLE --last admitted GET--> EXHAUSTED_PENDING_DELETE
EXPIRED_PENDING_DELETE | EXHAUSTED_PENDING_DELETE | FAILED
  --eligible cleanup--> DELETING --delete succeeds--> DELETED
DELETING --delete fails--> DELETE_RETRY --retry--> DELETING
```

Only `AVAILABLE` admits a new retrieval. `DELETING`, `DELETE_RETRY`, `FAILED`, and `DELETED` never do. V1 has no revocation transition because authenticated management is deliberately deferred.

### Store transaction sequence

1. `StoragePolicyEvaluator` normalizes producer input before a reservation.
2. `JdbcStorageMetadataStore.reserveStaging(...)` runs in one transaction: lock/conditionally update the global usage row, reserve the configured maximum entry bytes in `staging_bytes`, increment `entry_count`, and insert `STAGING` metadata with generated ID, token hash, and object key.
3. `FileSystemStorageByteStore.writeStaging(...)` streams into the `.part` file, enforces the exact entry ceiling, and returns the exact byte count plus SHA-256.
4. `publish(...)` moves the complete file atomically to its final object key, then in a transaction changes the entry to `AVAILABLE`, moves actual bytes from staging to published usage, releases any unused staging reservation, sets expiry/counters/checksum, and writes a publication event.
5. Any write failure removes the staging file, marks the entry `FAILED`, and releases its reservation. A crash after the atomic move but before SQL publication leaves an orphan/stale `STAGING` record; reconciliation removes it safely.

No transaction holds a database lock while bytes are copied. No file becomes addressable before the `AVAILABLE` transition commits.

### Retrieval admission

`StorageRetrievalService.openForGet(token)` hashes the supplied token, then `JdbcStorageMetadataStore.admitGet(...)` performs a short transaction:

1. lock the entry by token hash (`select ... for update`);
2. reject unless state is `AVAILABLE`, `expires_at > now`, and `remaining_requests > 0`;
3. conditionally reserve one global active stream, increment entry `active_streams`, decrement remaining requests, and change to `EXHAUSTED_PENDING_DELETE` if the new count is zero;
4. append an admitted event and commit.

The controller opens the file only after admission. If it cannot open it, `releaseFailedOpen(storageId)` decrements active-stream counters, marks the record for deletion/reconciliation, and responds with the same unavailable result before body commitment. A successfully admitted response is consumed even if the client disconnects; the `StreamingResponseBody` closes the stream and calls `completeStream(storageId, completed)` in `finally`. That transaction decrements active counters and schedules deletion if the state is pending-delete and the count reaches zero.

`HEAD` calls `inspectForHead(token)`: it reads an unexpired `AVAILABLE` entry without changing counters and verifies the object exists before returning headers. It intentionally does not keep an active stream because it has no body.

Deletion is a two-stage claim: `claimDelete(storageId)` transactionally changes an eligible pending state with `active_streams=0` to `DELETING`; the filesystem deletion follows outside the transaction; `completeDelete` marks `DELETED` and releases published quota, or `failDelete` changes to `DELETE_RETRY`. This makes concurrent cleanup, final-stream completion, and retry idempotent.

## 7. HTTP Adapter and Error Contract

`StorageController` is a server bean at `/storage` with exactly two v1 mappings:

```txt
GET  /storage/{accessToken}
HEAD /storage/{accessToken}
```

The `GET` mapping rejects a supplied `Range` header with `416` before admission. It obtains a `StorageOpenHandle`, sets `Content-Type`, exact `Content-Length`, `Content-Disposition: attachment`, `Cache-Control: no-store`, and `X-Content-Type-Options: nosniff`, then streams with `StreamingResponseBody`. The handle completion callback is always invoked. The `HEAD` mapping returns the same representation headers with no body and does not consume a request.

`StorageRouteExceptionHandler` maps every public capability-unavailable condition—unknown hash, expired, exhausted, missing object, pending deletion, bad token shape, and disabled storage—to the identical empty `404` response. It maps an unsupported range to `416` and an unexpected pre-commit storage failure to a generic `500` response while logging only the storage ID and failure code. A streaming failure after response commitment cannot change the status; it is recorded as an interrupted event and still consumes the admission.

There is no creation route, management route, authenticated caller policy, CORS exception, or security filter change in this slice. The existing security configuration permits the capability route; its token is the only retrieval gate by accepted decision. Future management routes require their own authorization design.

## 8. Cleanup, Reconciliation, and Observability

`StorageCleanupCoordinator.runBounded(now, batchSize)` runs at startup, before a new reservation, and periodically. It works in batches and is safe to rerun:

1. transition expired available entries to `EXPIRED_PENDING_DELETE`;
2. mark stale `STAGING` entries `FAILED` and release reservations;
3. claim/delete entries in pending, failed, or retry state when no stream is active;
4. find metadata rows whose object is missing and retire them through the same deletion path;
5. remove stale `.part` files not referenced by a live staging row;
6. remove final object files not referenced by a non-deleted metadata row, after a configurable grace period equal to staging TTL.

The pre-create run is one bounded batch only; it may not scan the entire root while a producer waits. A full scheduled pass repeats batches until no candidates remain, with each batch released between transactions.

Metrics: gauges for published/staging bytes, entry count, active streams, and state counts; counters for publish success/failure, admission outcomes, expired/exhausted cleanup, delete retries, and reconciliation repairs. Audit events use storage ID or a short hash fingerprint only. They must not log raw token, complete retrieval URL, absolute path, source URL, authorization header, cookie, or input stream content.

## 9. Implementation Test Matrix

| Layer | Required proof |
|---|---|
| API/domain unit | request normalization, MIME/filename sanitization, token hashing, lease URL shape, error-code stability. |
| filesystem unit | containment, same-root staging, byte ceiling, SHA-256, atomic move behavior, cleanup of partial writes. |
| JDBC integration (H2) | schema idempotence, quota reservation/release, publish transition, expiry transition, atomic final-request admission, active-stream cap, deletion claim/retry. |
| service integration | no partial visibility, failed publication cleanup, crash-window reconciliation, missing-object handling, final GET defers deletion until close, interrupted GET remains consumed. |
| MVC integration | exact GET/HEAD headers, `HEAD` non-consumption, uniform 404 states, range 416, attachment disposition, no raw token/path in public error body. |
| scheduler/reconciliation | startup, periodic, and pre-create bounded cleanup are idempotent; orphan staging/final files and stale metadata converge safely. |
| source-tool contract (later opt-in) | tool receives `StorageLease`, embeds stable `storage.lease/v1`, and never makes a loopback `/storage` request. |

Add PostgreSQL integration coverage before declaring a multi-database deployment supported. V1 otherwise claims the already-configured H2 path only. Fault-injection tests must cover database failure before/after move, delete failure, source-stream read failure, and concurrent requests at the final quota boundary.

## 10. Deferred Follow-Up Boundary

Remote ingestion, HTTP upload, authenticated management, ownership, revocation, multiple leases, ranges, resumable transfers, cross-node storage, CDN behavior, and inline content remain outside these classes and tables. A future design may add them without changing the v1 producer API or leaking implementation types into tool modules.
