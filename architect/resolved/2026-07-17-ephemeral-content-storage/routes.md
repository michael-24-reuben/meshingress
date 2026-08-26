# Route Design

This file defines the proposed HTTP surface for the temporary-content storage capability. It is planning material, not an implemented controller contract.

## Route Naming Rule

Capability retrieval and authenticated management use separate route shapes:

```txt
/storage/{accessToken}
/storage/entries/{storageId}
```

This prevents ambiguity between a bearer retrieval token and an authenticated management identifier.

## First Implementation Routes

| Method | Route | Authorization | Purpose |
|---|---|---|---|
| `GET` | `/storage/{accessToken}` | Opaque capability token | Retrieve stored bytes. |
| `HEAD` | `/storage/{accessToken}` | Same capability as `GET` | Retrieve representation headers without transferring bytes or consuming a retrieval. |

Ingestion is internal through `StorageService`; HTTP upload and authenticated management routes are deferred.

## Internal Java API

Source tools should not loop through HTTP when they already run inside Meshingress.

Proposed service shape:

```java
StorageLease store(
        InputStream content,
        StorageCreateRequest request,
        StorageCallContext context
);
```

Required characteristics:

- streams without reading all bytes into memory
- applies the same policy as HTTP ingestion
- returns the same `storage.lease/v1` domain result
- records producer, tool invocation, session, and request context
- never returns the physical path

## Optional Creation Routes

### Remote URL Ingestion

```http
POST /storage/entries/remote
```

This route remains disabled until the remote-ingestion threat model and policy are approved.

Example request:

```json
{
  "sourceUrl": "https://example.com/article.pdf",
  "ttlSeconds": 1800,
  "maxRequests": 3,
  "expectedMimeType": "application/pdf",
  "expectedSha256": "optional-expected-digest"
}
```

Remote ingestion must use a distinct route because it requires network scope, SSRF controls, DNS validation, redirect handling, and source-host policy that ordinary upload ingestion does not require.

A single polymorphic `POST /storage/entries` route was considered but is not preferred for v1 because local upload and remote ingestion have materially different authorization and security requirements.

## Authenticated Management Routes

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/storage/entries` | Paginated listing with owner, state, expiry, MIME, and source-tool filters. |
| `GET` | `/storage/entries/{storageId}` | Retrieve full lifecycle metadata. |
| `PATCH` | `/storage/entries/{storageId}` | Apply limited safe mutations such as shortening expiry or reducing remaining requests. |
| `POST` | `/storage/entries/{storageId}/revoke` | Immediately deny new retrievals without requiring immediate metadata deletion. |
| `DELETE` | `/storage/entries/{storageId}` | Revoke and physically delete bytes when safe. |
| `GET` | `/storage/usage` | Return published bytes, staging bytes, entry counts, and quota limits. |

Recommended list filters:

```http
GET /storage/entries?state=available
GET /storage/entries?ownerId=user-123
GET /storage/entries?createdBefore=2026-07-18T00:00:00Z
GET /storage/entries?expiresBefore=2026-07-18T01:00:00Z
GET /storage/entries?mimeType=application/pdf
GET /storage/entries?sourceTool=toonverse.fetch-chapters
```

Management responses must never expose raw access tokens, access-token hashes, local paths, source credentials, or unredacted source URLs.

## Multiple Lease Routes

Deferred extension:

| Method | Route | Purpose |
|---|---|---|
| `POST` | `/storage/entries/{storageId}/leases` | Mint another independently bounded lease for existing bytes. |
| `GET` | `/storage/entries/{storageId}/leases` | List lease metadata. |
| `DELETE` | `/storage/entries/{storageId}/leases/{leaseId}` | Revoke one lease without deleting the underlying object. |
| `POST` | `/storage/entries/{storageId}/leases/{leaseId}/rotate` | Replace a potentially exposed access token. |

This requires separating the physical stored object from independently managed leases. The one-entry/one-lease model remains the recommended v1 design.

## Resumable Upload Routes

Deferred extension for large or unstable client uploads:

| Method | Route | Purpose |
|---|---|---|
| `POST` | `/storage/uploads` | Begin an upload session. |
| `PUT` | `/storage/uploads/{uploadId}` | Upload or append a validated byte range. |
| `HEAD` | `/storage/uploads/{uploadId}` | Return the accepted upload offset. |
| `POST` | `/storage/uploads/{uploadId}/complete` | Validate and atomically publish the completed object. |
| `DELETE` | `/storage/uploads/{uploadId}` | Abort the upload and remove staging data. |

Do not add this protocol unless ordinary streamed upload proves insufficient.

## Remote Ingestion Job Routes

Deferred extension for downloads that may outlive an MCP request:

| Method | Route | Purpose |
|---|---|---|
| `POST` | `/storage/ingestions` | Start asynchronous remote ingestion. |
| `GET` | `/storage/ingestions/{ingestionId}` | Read progress, failure state, or resulting lease. |
| `DELETE` | `/storage/ingestions/{ingestionId}` | Cancel active ingestion. |

Suggested job states:

```txt
QUEUED
RESOLVING
CONNECTING
DOWNLOADING
VALIDATING
PUBLISHING
COMPLETED
FAILED
CANCELLED
```

## Operational Routes

Operational actions should live under the authenticated administrative API namespace rather than the public capability path:

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/storage/status` | Storage enablement, root health, metadata-store health, and capacity state. |
| `GET` | `/api/v1/storage/usage` | Operator-wide quota and lifecycle statistics. |
| `POST` | `/api/v1/storage/cleanup` | Manually trigger bounded cleanup. |
| `POST` | `/api/v1/storage/reconcile` | Reconcile filesystem objects and database metadata. |
| `GET` | `/api/v1/storage/policy` | Read effective non-secret limits and policy. |

Cleanup and reconciliation remain internal operational actions. They are not public `/storage` routes.

## Response Semantics

| Condition | Recommended response |
|---|---|
| Successful synchronous creation | `201 Created` |
| Successful retrieval or header inspection | `200 OK` |
| Unknown, expired, exhausted, revoked, or deleted capability | Uniform `404 Not Found` |
| Authenticated deletion succeeds or target is already absent | `204 No Content` |
| Entry limit exceeded | `413 Payload Too Large` |
| Aggregate quota unavailable | `507 Insufficient Storage` |
| Unsupported range request | `416 Range Not Satisfiable` |
