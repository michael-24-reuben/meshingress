# Design Plan

## Phase 0 — Code Architecture Closure

The code-level design is complete in [code-architecture.md](code-architecture.md). It fixes the module boundary, public Java API, server package ownership, typed configuration, SQL/filesystem layout, transaction protocol, route adapter, and test matrix. No implementation starts without separate authorization.

Deliverables:

- approved route and caller contract
- approved retrieval authorization model
- approved lifecycle state machine
- approved request-count semantics
- approved configuration defaults and ceilings
- approved remote-ingestion security policy
- approved `storage.lease/v1` schema
- explicit implementation authorization

## Phase 1 — Module and Boundary Design

Final boundaries:

```txt
lib/meshingress-storage-api
  └─ stable producer-facing `StorageService` and value records

app/meshingress-server
  └─ all JDBC, filesystem, lifecycle, cleanup, and HTTP implementation
```

Final separation:

| Concern | Boundary |
|---|---|
| Producer-facing contract | `meshingress-storage-api` |
| HTTP mapping, lifecycle, quota | Server application |
| Byte persistence | Server filesystem adapter |
| Metadata persistence | Server JDBC adapter |
| Tool integration | API module injected into the tool bean |
| Scheduled cleanup | Server runtime adapter invoking server services |

## Phase 2 — Lifecycle Model

Final v1 states:

```txt
STAGING -> AVAILABLE | FAILED
AVAILABLE -> EXPIRED_PENDING_DELETE | EXHAUSTED_PENDING_DELETE
EXPIRED_PENDING_DELETE | EXHAUSTED_PENDING_DELETE | FAILED -> DELETING
DELETING -> DELETED | DELETE_RETRY
DELETE_RETRY -> DELETING
```

Optional operational state:

```txt
AVAILABLE -> DELETE_RETRY
```

State rules:

- Only `AVAILABLE` entries may admit retrieval.
- Expiry prevents new retrieval immediately.
- The final permitted retrieval changes state to `EXHAUSTED_PENDING_DELETE` before bytes are streamed.
- Physical deletion waits until the active-stream count reaches zero.
- Cleanup retries failed deletion and reconciles stale state.
- `STAGING` rows and temporary files have a short independent staging TTL.

## Phase 3 — Atomic Ingestion

Recommended sequence:

1. Accept a sanitized internal producer context; v1 has no HTTP producer authorization surface.
2. Normalize requested TTL, request count, MIME, and filename against policy.
3. Run cleanup.
4. Reserve aggregate quota.
5. Create a `STAGING` metadata record.
6. Create a temporary file beneath the dedicated root on the same filesystem as the final location.
7. Stream bytes while:
   - enforcing the byte ceiling
   - computing SHA-256
   - tracking exact byte count
   - enforcing timeout and cancellation
8. Validate final MIME and content policy.
9. Flush and close the file.
10. Atomically move it into its opaque final location.
11. Transition metadata from `STAGING` to `AVAILABLE`.
12. Return the lease node.
13. On failure, remove temporary bytes, release quota, and mark or delete the failed record.

The final filename should be generated from an internal opaque identifier. The suggested user filename remains metadata only.

## Phase 4 — Retrieval Admission

Recommended atomic admission transaction:

1. Hash the presented access token.
2. Locate an `AVAILABLE` lease.
3. Reject if hard-expired.
4. Verify the file exists and its known size is consistent.
5. Atomically decrement `remainingRequests`.
6. Increment `activeStreams`.
7. If the new count is zero, transition to `EXHAUSTED_PENDING_DELETE`.
8. Commit the admission.
9. Open and stream the file.
10. In a completion callback, decrement `activeStreams`.
11. Delete when state is pending-delete and no streams remain.

### Interrupted Streams

Recommended security rule:

A retrieval is consumed once the server has admitted the stream and committed response headers. An interrupted transfer remains consumed because the server cannot prove how many bytes the client received and refunding enables abuse through repeated disconnects.

The system should separately record:

- admitted retrieval
- completed retrieval
- interrupted retrieval

This preserves audit truth while keeping request-limit enforcement deterministic.

## Phase 5 — Authorization

### Recommended Baseline

- `GET` and `HEAD`: access token acts as the capability.
- Optional caller authentication may be required by deployment policy, but should not replace the capability token.
- `DELETE` and `POST` are deferred with management/upload routes.
- Internal producers call a Java service API and do not loop through HTTP.

### Token Handling

- Generate at least 256 bits of cryptographically secure random token material.
- Encode using unpadded base64url.
- Return the raw token only in the retrieval URL.
- Persist only a keyed hash or cryptographic digest suitable for lookup.
- Never log the raw token.
- Redact the route segment in access logs and audit events.

## Phase 6 — Remote Ingestion Safety

Remote ingestion is optional and must remain disabled until policy is approved.

Recommended controls:

- allow only `https` by default
- reject URL user-info and fragments
- restrict ports, defaulting to `443`
- resolve DNS before connection
- reject loopback, link-local, private, carrier-grade NAT, multicast, unspecified, and reserved addresses
- evaluate every resolved A and AAAA address
- revalidate each redirect target
- cap redirects
- do not forward caller credentials across redirects
- apply connect, response-header, idle-read, and total-operation timeouts
- treat `Content-Length` as advisory
- enforce byte limits on the actual stream
- optionally require an operator host allowlist
- validate declared MIME against sniffed content
- compute SHA-256
- prohibit local file and non-HTTP schemes
- sanitize source details in audit output

DNS pinning and connection behavior must ensure the actual connected address is one that passed validation.

## Phase 7 — Cleanup and Reconciliation

Startup and periodic cleanup should:

- mark hard-expired entries pending-delete
- delete exhausted or revoked entries with no active streams
- remove stale temporary files
- remove orphan final files
- reconcile missing files
- release leaked quota reservations
- retry failed filesystem deletion
- expire stale `STAGING` records
- emit metrics and sanitized audit events

Pre-create cleanup may be bounded to avoid unbounded request latency. A full cleanup remains scheduled.

## Phase 8 — Verification Design

Tests to require before implementation is considered complete:

### Unit

- TTL normalization and ceilings
- request-count admission under concurrency
- expiry boundary behavior
- quota reservation and release
- token hashing and redaction
- state transitions
- interrupted-stream accounting
- filename sanitization
- MIME validation
- SSRF address classification

### Integration

- streamed ingestion larger than memory buffer
- byte-limit interruption removes temporary data
- atomic publication exposes no partial file
- startup cleanup
- periodic cleanup
- pre-create cleanup
- final retrieval defers deletion until stream close
- concurrent final requests admit only the allowed count
- database/file reconciliation
- remote redirects and DNS revalidation
- server route headers and status codes

### Failure Injection

- process termination during staging
- database failure before and after atomic move
- filesystem deletion failure
- client disconnect during final stream
- missing file after metadata commit
- quota race
- checksum mismatch

## Proposed Delivery Order

After explicit implementation authorization, the safest delivery order is:

1. finalize configuration and ownership contracts
2. create the domain API and lifecycle model
3. implement filesystem staging and atomic publication
4. implement metadata persistence and quota reservation
5. implement retrieval admission and final-stream deletion
6. implement the core v1 HTTP routes
7. implement startup, scheduled, and pre-create cleanup
8. implement metrics, auditing, and reconciliation
9. integrate one source tool through the internal Java API
10. consider deferred remote ingestion only after v1 verification

The route inventory and feature inventory are maintained in `routes.md` and `features.md`. The initial implementation boundary is maintained in `v1-scope.md`.
