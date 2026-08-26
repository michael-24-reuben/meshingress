# Feature Design

This file consolidates the functional features proposed for the storage capability.

## 1. Lease Lifecycle

Recommended states:

```txt
STAGING
AVAILABLE
EXPIRED_PENDING_DELETE
EXHAUSTED_PENDING_DELETE
REVOKED_PENDING_DELETE
DELETE_RETRY
DELETED
FAILED
```

Rules:

- only `AVAILABLE` entries admit retrieval
- hard expiry is enforced during retrieval admission, not only by cleanup
- revocation prevents new retrieval immediately
- the final permitted retrieval marks the entry pending-delete before streaming begins
- physical deletion waits for active streams to close
- cleanup retries failed filesystem deletion
- stale `STAGING` entries have an independent staging timeout

## 2. Predictable MCP Lease Node

Recommended structured-content node:

```json
{
  "type": "storage.lease/v1",
  "storageId": "stg_opaque-management-id",
  "url": "https://meshingress.example/storage/opaque-access-token",
  "mimeType": "application/pdf",
  "byteSize": 482193,
  "createdAt": "2026-07-17T22:41:58-04:00",
  "expiresAt": "2026-07-17T23:11:58-04:00",
  "remainingRequests": 3,
  "checksum": {
    "algorithm": "sha-256",
    "value": "digest"
  },
  "etag": "sha256-digest",
  "suggestedFilename": "article.pdf",
  "disposition": "attachment"
}
```

Required fields:

- `type`
- `url`
- `mimeType`
- `byteSize`
- `expiresAt`
- `remainingRequests`

Recommended optional fields:

- `storageId`
- `createdAt`
- `checksum`
- `etag`
- `suggestedFilename`
- `disposition`

Forbidden fields include source URL, filesystem path, staging path, raw database key, access-token hash, source headers, cookies, and credentials.

## 3. Retrieval Semantics

Recommended rules:

- `HEAD` does not decrement `remainingRequests`
- `GET` is consumed after atomic admission and response commitment
- interrupted downloads remain consumed
- unavailable capabilities return uniform `404`
- `Cache-Control: no-store`
- `X-Content-Type-Options: nosniff`
- `Content-Disposition: attachment` by default
- inline delivery is allowed only for explicitly safe MIME classes
- range requests are disabled in v1

Deferred range strategies:

1. count every accepted range request
2. create a short-lived download-session token
3. bind subsequent ranges to one logical retrieval session

## 4. Quota Model

Support separate quotas for:

- per-entry bytes
- total published bytes
- total staging bytes
- total entry count
- entries per principal
- bytes per principal
- entries per source tool
- concurrent ingestions
- concurrent retrievals
- daily ingested bytes
- remote-ingestion bandwidth

Creation reserves quota before accepting an unbounded stream. Failed ingestion releases the reservation.

## 5. Ownership and Attribution

Recommended metadata:

```json
{
  "ownerType": "principal",
  "ownerId": "user-123",
  "createdByTool": "toonverse.fetch-chapters",
  "mcpSessionId": "session-456",
  "requestId": "request-789"
}
```

Ownership controls metadata visibility, early deletion, quota accounting, audit attribution, lease creation, and administrative override. Capability retrieval may remain independent of owner authentication.

## 6. Integrity

Include:

- SHA-256 calculated while streaming
- exact stored byte count
- optional expected checksum
- optional expected byte count
- final checksum verification before publication
- ETag derived from stored checksum
- reconciliation checks for missing or mismatched files

Conditional requests are deferred. Before enabling them, decide whether `304 Not Modified` consumes a retrieval.

## 7. MIME and Filename Policy

Features:

- validate MIME syntax
- compare declared and detected types
- support MIME allow and deny rules
- sanitize suggested filenames
- remove path components
- reject control characters
- enforce maximum filename length
- classify content as `safe-inline`, `attachment-only`, `blocked`, or `unknown`
- default active or unknown content to attachment
- set `X-Content-Type-Options: nosniff`

## 8. Remote Ingestion

Remote ingestion remains disabled by default.

Required controls before enablement:

- HTTPS-only default
- allowed-port policy
- optional host allowlist
- redirect ceiling and target revalidation
- DNS resolution validation
- IPv4 and IPv6 private-address rejection
- loopback, link-local, multicast, reserved, unspecified, and metadata-address rejection
- DNS rebinding protection
- connection, header, idle-read, and total-operation timeouts
- streamed byte enforcement independent of `Content-Length`
- no credential forwarding across origins
- source URL and query redaction
- MIME validation
- checksum calculation

Suggested configuration posture:

```properties
meshingress.storage.remote.enabled=false
```

## 9. Cleanup and Reconciliation

Cleanup handles:

- expired, exhausted, and revoked entries
- abandoned staging files
- stale `STAGING` metadata
- orphan files without database metadata
- metadata rows whose file is missing
- leaked quota reservations
- failed deletion retries
- crash residues

Triggers:

1. startup
2. periodic schedule
3. before entry creation
4. manual authenticated operator action

Pre-create cleanup may be bounded to protect request latency.

## 10. Observability

Recommended metrics:

```txt
storage.entries.available
storage.bytes.published
storage.bytes.staging
storage.quota.rejections
storage.ingestion.success
storage.ingestion.failure
storage.retrieval.admitted
storage.retrieval.completed
storage.retrieval.interrupted
storage.entries.expired
storage.entries.exhausted
storage.entries.revoked
storage.cleanup.deleted
storage.cleanup.failed
storage.reconciliation.orphans
```

Recommended audit events:

```txt
STORAGE_CREATE_REQUESTED
STORAGE_CREATED
STORAGE_RETRIEVAL_ADMITTED
STORAGE_RETRIEVAL_COMPLETED
STORAGE_RETRIEVAL_INTERRUPTED
STORAGE_EXPIRED
STORAGE_EXHAUSTED
STORAGE_REVOKED
STORAGE_DELETED
STORAGE_CLEANUP_FAILED
STORAGE_REMOTE_REJECTED
```

Never log raw access tokens, full capability URLs, source credentials, cookies, local paths, or unredacted sensitive query parameters.

## 11. Token Management

Recommended token characteristics:

- at least 256 bits of cryptographically secure random material
- unpadded base64url encoding
- raw token returned only in the capability URL
- only a cryptographic lookup digest persisted
- token fingerprint permitted in audit records
- immediate revocation support
- future token rotation through explicit lease-management routes

## 12. Multi-Node Readiness

Even if v1 is single-node, preserve these future requirements:

- shared or node-affined filesystem semantics
- database-backed atomic request admission
- ownership of cleanup work
- no reliance on in-memory request counters
- safe handling when one node retrieves while another schedules deletion

Do not claim multi-node support until these behaviors are implemented and verified.
