# Product Requirements: Storage Lifecycle and Write-Only Handoff

## Goal

Allow tools to materialize downloadable content using a configurable lifecycle while preserving the existing local experience and allowing final delivery to foreign provider-owned storage.

## Lifecycle policy

| Lifecycle | Local staging | Local publication/retrieval | Foreign handoff | Valid |
|---|---|---|---|---|
| `local-local` | yes | yes | no | yes |
| `local-external` | yes | no after handoff | final create-only write | yes |
| `external-external` | no | no | provider upload session then final create-only write | yes |
| `external-local` | no | would require foreign read | n/a | no |

The server must validate the selected lifecycle and target capability at startup:

- `local-local` needs a local backend and does not require an external target.
- `local-external` needs a local backend plus a target that can create a final object.
- `external-external` needs a target that supports a provider-managed upload/staging session and finalization without a later foreign read.

## Configuration contract

The proposed structure in `app/meshingress-server/src/main/resources/storage.properties.md` is the target reference. It retains the existing policy values but scopes local physical settings under `meshingress.storage.local`, external policy/targets under `meshingress.storage.external`, and shared control-plane settings under `meshingress.storage.metadata.sql`.

Important policy rules:

- `meshingress.storage.max-entry-size` applies while Meshingress receives bytes in every lifecycle. Its current behavior is a maximum total size for one workspace, not a per-file limit; the new documentation and API must either preserve that behavior explicitly or deliberately rename/redefine it.
- `meshingress.storage.local.max-entries` limits active locally managed workspaces. Durable audit records for externalized content must not consume this local physical-storage quota.
- Local publish TTL, request, and concurrent-retrieval settings apply only to `local-local`.
- Local cleanup applies only to local staging and locally published content. It must never act on external files.
- Shared SQL records lifecycle metadata, usage accounting, and auditable handoff events. It retains no file bytes and is not configured per external target.

## Universal create-only contract

Every backend must implement create-only final-object behavior.

- A local backend uses an exclusive create and rejects existing final names.
- A WebDAV backend must use a server-enforced conditional create. A plain `PUT` that may overwrite does not satisfy this requirement.
- A provider that cannot prove create-only behavior for the selected target must be rejected for a create-only lifecycle, rather than silently degrading to overwrite behavior.
- Object keys must be server-generated or otherwise collision-resistant. The system must not rely on a preceding foreign `contains` call to avoid races.

`write-only` permits the writes required to create, stream, and finalize an uncommitted provider upload session. It prohibits remote object reads and all post-publication modifications, deletion, listing, cleanup, or reconciliation.

## Full file-operation API

The API must define the complete operation vocabulary even though individual backends expose different capabilities.

```java
public interface StorageFileApi {
    StorageCapabilities capabilities();

    StorageResult<CreateFileResult> createFile(CreateFileRequest request);
    StorageResult<WriteResult> writeFile(WriteHandle handle, InputStream content);
    StorageResult<AppendResult> appendFile(WriteHandle handle, InputStream content);
    StorageResult<CompleteFileResult> completeFile(WriteHandle handle);
    StorageResult<AbortFileResult> abortFile(WriteHandle handle);

    StorageResult<FilePresence> containsFile(StorageFileKey key);
    StorageResult<StorageFileMetadata> getFileMetadata(StorageFileKey key);
    StorageResult<Long> getFileSize(StorageFileKey key);
    StorageResult<InputStream> readFile(StorageFileKey key);
    StorageResult<DeleteFileResult> deleteFile(StorageFileKey key);
}
```

The exact Java names and request/response records may change during implementation, but the semantic operation set is required.

`StorageCapabilities` declares which operations are permitted for a backend and object state. `StorageResult` carries a stable result category, including success, not-found, conflict, unsupported, denied, and failed. Unsupported or denied operations must be explicit; they must not be silently emulated through another provider operation.

`containsFile`, metadata, size, and read are observation operations. A write-only handoff target defines these API methods but declares them unavailable; Meshingress must not invoke them. Locally retained handoff audit data is queried through metadata/audit services, not by probing the foreign object.

`appendFile`, `completeFile`, and `abortFile` apply only to an uncommitted write handle. A completed foreign object cannot be appended, overwritten, deleted, or read by Meshingress.

## Audit and receipts

For every external handoff, retain a durable local event containing at least:

- lifecycle mode and target name
- server-generated object key or opaque provider object ID
- size, MIME type, checksum, and creation/finalization timestamps
- tool, session, and request identity
- create/finalize result and provider receipt/reference

Do not persist credentials, bearer tokens, pre-signed read URLs, or foreign filesystem paths in the audit event.

## Non-goals

- Giving Meshingress read or mutation authority over a foreign handoff destination.
- Provider retention enforcement, foreign cleanup, or remote reconciliation.
- Treating a network share as a foreign handoff; an accessible mounted filesystem remains local storage for this feature.
- Changing durable artifact repository storage.

## Acceptance criteria

- Configuration rejects invalid lifecycle/target combinations before accepting tool writes.
- A local collision and a provider collision both produce a conflict/failure without overwriting a completed object.
- External handoff success writes a durable local audit event and does not schedule foreign cleanup.
- A write-only backend exposes no permitted read, contains, metadata, size, delete, or post-completion append operation.
- Local-local behavior remains covered for staging, atomic publication, retrieval limits, expiry, and cleanup.
