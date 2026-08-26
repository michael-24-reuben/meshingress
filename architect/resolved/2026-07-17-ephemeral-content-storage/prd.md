# Product Requirements

## Problem

MCP tool results can describe content, but large physical content should not be embedded directly in JSON-RPC responses. Meshingress needs a generic handoff mechanism that materializes bytes temporarily and returns a bounded retrieval lease.

The capability must be generic enough for any source tool while remaining separate from durable artifact publication.

## Goals

1. Expose temporary physical content through a predictable server route.
2. Return a stable `storage.lease/v1` node through MCP `structuredContent`.
3. Bound storage consumption by per-entry size, aggregate quota, TTL, and retrieval count.
4. Avoid exposing source URLs, local paths, or internal filenames.
5. Stream ingestion without buffering the complete object in memory.
6. Publish entries atomically only after validation and complete ingestion.
7. Delete expired, exhausted, failed, and explicitly removed entries.
8. Make lifecycle and access decisions auditable.

## Non-Goals

- Durable or versioned package storage
- General-purpose user filesystem
- Public directory browsing
- Permanent CDN behavior
- Source-specific routes
- An MCP function family named `storage.*`
- Reusing result-cache entries as downloadable files
- Implementing the capability during this planning record

## Functional Requirements

### Ingestion

- Accept a byte stream supplied by an internal producer or an authorized HTTP caller.
- Optionally support controlled remote HTTP ingestion.
- Enforce the entry byte ceiling while streaming.
- Write into a temporary file located on the same filesystem as the final storage root.
- Compute at least SHA-256 while ingesting.
- Verify declared and observed metadata according to policy.
- Atomically move the completed file into its published location.
- Never publish an incomplete file.
- Run cleanup before admitting new content.
- Reject creation when quota cannot be safely reserved.

### Retrieval

- Resolve an opaque access token without revealing internal identifiers.
- Verify lifecycle state, hard expiry, and request allowance before opening the file.
- Return the stored MIME type and byte size.
- Do not increment usage for `HEAD`.
- Prevent concurrent retrievals from exceeding the maximum request count.
- Safely defer deletion while an admitted final stream is still open.
- Return a uniform unavailable response for unknown, expired, exhausted, or deleted tokens unless policy explicitly requires otherwise.

### Deletion

- Permit authenticated early deletion by `storageId`.
- Revoke the access token before removing bytes.
- Make deletion idempotent at the service boundary.
- Record lifecycle reason and audit identity when available.

### Cleanup

Cleanup runs:

1. during application startup
2. periodically
3. before accepting a new entry

Cleanup must reconcile both metadata and filesystem state:

- expired entries
- exhausted entries
- abandoned staging files
- records whose file is missing
- files whose metadata is missing
- pending-delete entries with no active streams
- failed ingestion residues

## Lease Contract Requirements

A successful producer returns a `storage.lease/v1` `structuredContent` node containing, at minimum:

- opaque retrieval URL
- MIME type
- byte size
- expiry time
- remaining retrieval count

The contract may also include:

- opaque storage ID for authenticated deletion
- checksum
- suggested filename
- creation time

It must not include:

- filesystem path
- temporary path
- source URL
- secret headers
- internal database key
- raw access-token hash

## Limit Requirements

The final configuration surface must distinguish:

- defaults
- operator-configured ceilings
- non-overridable hard ceilings

Limits requiring decisions:

- default TTL
- maximum TTL
- default request count
- maximum request count
- per-entry byte limit
- aggregate byte quota
- staging quota
- maximum concurrent retrievals
- remote-ingestion timeout
- redirect count
- allowed protocols
- allowed ports
- MIME allow/deny rules
- remote-host allow/deny rules

## Observability Requirements

At minimum, record metrics for:

- create attempts
- create success and failure
- staged bytes
- published bytes
- current stored bytes
- quota rejections
- retrieval admissions
- retrieval completions
- interrupted retrievals
- expirations
- request exhaustion
- explicit deletions
- cleanup deletions
- orphan reconciliation
- SSRF and validation rejections

Audit records must redact access tokens, source credentials, and sensitive query strings.
