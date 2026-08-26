# Decision Register

Statuses:

- `ACCEPTED` — approved design constraint
- `PROPOSED` — recommended default awaiting approval
- `OPEN` — no preferred answer finalized
- `DEFERRED` — intentionally excluded from the first implementation

| ID | Decision | Status | Current Position |
|---|---|---|---|
| STG-001 | `/storage` is a first-class route | ACCEPTED | Route is alongside `/mcp` and `/artifact`; not source-specific and not an MCP function family. |
| STG-002 | `/storage` is ephemeral | ACCEPTED | Durable versioned content remains under `/artifact`. |
| STG-003 | Bytes and metadata are separated | ACCEPTED | Bytes on dedicated filesystem root; lease metadata and lifecycle state in database. |
| STG-004 | Lease result schema | ACCEPTED IN PRINCIPLE | Generic `storage.lease/v1` node with URL, MIME, size, expiry, and remaining requests. Exact optional fields remain reviewable. |
| STG-005 | Opaque access credentials | ACCEPTED | Never expose source URLs or filesystem paths. |
| STG-006 | Cleanup triggers | ACCEPTED | Startup, periodic, and pre-create. |
| STG-007 | Atomic streamed ingestion | ACCEPTED | Temporary file, byte enforcement, then atomic publication. |
| STG-008 | Producer access surface | ACCEPTED | First implementation exposes only the internal `StorageService`; HTTP upload creation is deferred. |
| STG-009 | Retrieval authorization | ACCEPTED | An opaque capability token is the first implementation's only retrieval gate; additive authentication is deferred. |
| STG-010 | `HEAD` usage accounting | ACCEPTED | `HEAD` does not consume a retrieval. |
| STG-011 | Interrupted `GET` accounting | ACCEPTED | Consume after admission/response commitment; interruption remains consumed. |
| STG-012 | Final-stream deletion | ACCEPTED | Mark pending-delete atomically and delete after active stream count reaches zero. |
| STG-013 | Retrieval unavailable status | ACCEPTED | Uniform `404` for unknown, expired, exhausted, revoked, or deleted tokens. |
| STG-014 | Range requests | DEFERRED | Disable in the first implementation; design separately before enabling. |
| STG-015 | Token persistence | ACCEPTED | Store only token hash; raw token appears only in the returned URL. |
| STG-016 | Checksum | ACCEPTED | Compute and store SHA-256 for every published entry. |
| STG-017 | Remote ingestion | DEFERRED | Excluded from the first implementation; its SSRF and network policy need a dedicated future design. |
| STG-018 | Configuration defaults | ACCEPTED | Define bounded defaults and hard ceilings under `meshingress.storage.*` at implementation time; numeric values are not architectural blockers. |
| STG-019 | Public URL construction | ACCEPTED | Return server-relative capability URLs beginning `/storage/`; deployment URL rewriting remains outside this capability. |
| STG-020 | Ownership model | DEFERRED | No authenticated ownership or management model is included in the first implementation. |

## Expanded Route and Feature Decisions

| ID | Decision | Status | Current Position |
|---|---|---|---|
| STG-021 | Separate capability and management paths | DEFERRED | Keep the proposed route separation for a future authenticated-management slice. |
| STG-022 | Core upload route | DEFERRED | HTTP upload is outside the first implementation; tools use the internal API. |
| STG-023 | Internal tool integration | ACCEPTED | Source tools call an internal `StorageService`; they do not perform loopback HTTP. |
| STG-024 | Metadata lookup route | DEFERRED | Authenticated metadata lookup is outside the first implementation. |
| STG-025 | Usage route | DEFERRED | Authenticated usage reporting is outside the first implementation. |
| STG-026 | Remote URL route | DEFERRED | Reserve `POST /storage/entries/remote`; keep disabled until security policy approval. |
| STG-027 | Listing and patch management | DEFERRED | Add paginated listing and limited mutation only after v1 ownership rules are stable. |
| STG-028 | Multiple leases | DEFERRED | Keep one-entry/one-lease in v1; separate stored objects and leases later if needed. |
| STG-029 | Resumable uploads | DEFERRED | Add only if ordinary streamed upload proves insufficient. |
| STG-030 | Asynchronous ingestion jobs | DEFERRED | Add only for remote downloads that may outlive MCP invocation timeouts. |
| STG-031 | Operational route namespace | DEFERRED | Add authenticated operational routes only with the management slice. |
| STG-032 | Attachment disposition | ACCEPTED | Default all first-version retrievals to attachment; introduce safe-inline classification later. |
| STG-033 | Ownership attribution | DEFERRED | Record optional tool/session context when available, but do not introduce an ownership model. |
| STG-034 | Quota dimensions | ACCEPTED | Enforce entry, published, staging, and concurrency quotas; owner quotas are deferred. |
| STG-035 | V1 remote ingestion | ACCEPTED | Exclude remote ingestion from the first implementation. |
| STG-036 | Storage configuration location | ACCEPTED | Keep `meshingress.storage.*` in the server `application.properties`, bound through a nested `MeshingressProperties.Storage` record. Do not create or import a separate `storage.properties`; the existing imported `artifact.properties` is a repository-specific configuration bundle, not the general configuration pattern. |
