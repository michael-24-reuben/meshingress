# Todo

## Planning

- [x] Record `/storage` as a generic first-class server route.
- [x] Separate ephemeral storage from `/artifact` durability and result caching.
- [x] Record filesystem-byte/database-metadata separation.
- [x] Draft the `storage.lease/v1` contract.
- [x] Draft lifecycle and atomic-ingestion models.
- [x] Draft request-accounting and final-stream deletion behavior.
- [x] Draft remote-ingestion safety requirements.
- [x] Define the internal-only producer contract for the first implementation.
- [x] Define opaque-token retrieval without authenticated route calling.
- [x] Defer ownership and deletion authority with authenticated management routes.
- [x] Set configuration defaults and hard ceilings as bounded implementation-time properties.
- [x] Approve request-count accounting and final-stream deletion behavior.
- [x] Defer range-request behavior.
- [x] Defer remote ingestion and its SSRF policy.
- [x] Approve uniform public unavailable status behavior.
- [x] Select server-relative public URLs.
- [x] Finalize the `storage.lease/v1` architecture contract.
- [x] Record that implementation remains separately unauthorized.

## Code Architecture Still Required

- [x] Define the Maven module boundary, artifact name, and dependency direction for storage.
- [x] Define the Java package layout and public/internal interfaces, including `StorageService`, lease types, policy evaluation, and byte-store contracts.
- [x] Define database records, schema/migration ownership, indexes, and the filesystem object/staging layout.
- [x] Define lifecycle state transitions, transaction boundaries, cleanup reconciliation, and concurrent final-request handling.
- [x] Define Spring configuration, bean ownership, controller/service adapters, error mapping, and the `meshingress.storage.*` typed binding.
- [x] Define the first implementation's test matrix: atomic publication, quota races, expiry, request exhaustion, cleanup, and retrieval streaming.
- [x] Resolve this architect only after the code-level architecture is complete and the user accepts it.

## V1 Implementation

- [x] Create `meshingress-storage-api` producer module.
- [x] Add `GET` and `HEAD` `/storage/{accessToken}` route mappings.
- [x] Add idempotent JDBC schema initialization and metadata store.
- [x] Add filesystem staging, checksum, atomic publication, and deletion store.
- [x] Add startup and scheduled bounded cleanup.
- [x] Add typed `meshingress.storage.*` configuration.
- [x] Add server tests for publication, request accounting, cleanup, and HTTP behavior.
- [x] Preserve source-tool integration and Toonverse behavior as deferred follow-up work; the server API is ready for a future opt-in tool.

## Session/Request Workspace Revision

- [x] Move the public producer contract into `meshingress-tool-api` as a tool-facing storage workspace API.
- [x] Replace opaque storage IDs and token routes with session/request workspace metadata and named file records.
- [x] Store files at `tools/{sessionId}/{requestId}/files/` and generate `files/manifest.json` from authoritative metadata.
- [x] Replace storage routes with `GET` and `HEAD /storage/{sessionId}/{requestId}/files/{relativePath}`.
- [x] Preserve TTL, aggregate quotas, request limits, active-stream safety, and recursive cleanup at workspace scope.
- [x] Add focused unit and MVC tests for workspace paths, manifest publication, retrieval, and cleanup.

## Route and Feature Planning

- [x] Separate capability retrieval from authenticated management routes.
- [x] Define the recommended core v1 route surface.
- [x] Define the internal Java streaming API boundary.
- [x] Define deferred remote-ingestion, resumable-upload, and asynchronous-job routes.
- [x] Define deferred multiple-lease routes.
- [x] Define operational route placement under `/api/v1/storage`.
- [x] Select the server configuration location and typed binding pattern.
- [x] Define ownership, quota, integrity, MIME, cleanup, and observability features.
- [x] Define the recommended v1 included and deferred scope.
- [x] Resolve the first implementation to internal ingestion plus public opaque-token retrieval only.
- [x] Defer the expanded authenticated management and remote-ingestion route surface.
