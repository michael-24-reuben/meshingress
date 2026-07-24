# Context

## Accepted model

`meshingress.storage.lifecycle` remains an application property with exactly two values:

```text
LOCAL_LOCAL
LOCAL_EXTERNAL
```

It describes only the placement of bytes owned by Meshingress:

| Lifecycle | Staging | Publication |
| --- | --- | --- |
| `LOCAL_LOCAL` | Meshingress local storage | Meshingress local storage |
| `LOCAL_EXTERNAL` | Meshingress local staging | configured external destination |

Each tool method carries out its transfer explicitly:

```text
LOCAL_BYTES
DELEGATED_SOURCE_URLS
```

- `LOCAL_BYTES` means the tool has, obtains, or streams bytes through Meshingress. The configured lifecycle applies. With `LOCAL_EXTERNAL`, the tool can request immediate handoff, durable queued handoff, or retained staging when the method contract permits it.
- `DELEGATED_SOURCE_URLS` means the tool supplies durable source descriptors to a delegated destination. That destination owns acquisition, staging, validation, publication, and its import-job status. Meshingress has no source bytes to hand off, so a Meshingress async handoff is invalid and must not be configurable for this transfer.

The tool chooses the transfer; the property does not choose it. Provider/target configuration remains startup infrastructure and validates whether the requested transfer is supported. Tools must not choose raw credentials or unrestricted targets.

## Concrete examples

- A qBittorrent tool owns a qBittorrent job until the download completes. Its later collection/publish operation transfers completed local files as `LOCAL_BYTES`; `LOCAL_EXTERNAL` can hand them off inline or through the Meshingress durable worker.
- Toonverse may submit page and cover HTTPS URLs as `DELEGATED_SOURCE_URLS` while uploading native JSON descriptors to the reserved destination workspace. After sealing, the destination's import status is observed separately from Meshingress's local-byte handoff worker.

## Current implementation boundary

`MeshingressStorageConfiguration` currently creates one `ToolStorageService` based on the global lifecycle. `ToolWorkspaceStorageService` selects inline versus async publication from that same global setting, and `NextcloudDelegatedStorageService` replaces the service entirely under `DELEGATED_EXTERNAL`. `ToonverseTool` branches on the service-wide `transferMode()`.

This record changes that architecture only when explicitly implemented. It does not repeal the active delegated-source or streaming-ingest records; their contracts and deployed behavior must be reconciled deliberately during implementation.
