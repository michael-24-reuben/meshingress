# Plan

1. Reduce `MeshingressProperties.Storage.Lifecycle` and its documented property values to `LOCAL_LOCAL` and `LOCAL_EXTERNAL`; define a compatible configuration migration for legacy values.
2. Introduce a tool-requested transfer contract for `LOCAL_BYTES` and `DELEGATED_SOURCE_URLS`. Store the resolved choice with the workspace rather than deriving it from one singleton storage service.
3. Keep local-byte execution policy separate: only a `LOCAL_BYTES` workspace under `LOCAL_EXTERNAL` may use inline handoff, queued handoff, or explicit retained staging.
4. Replace the single lifecycle-selected storage bean with a validated router/registry that can serve local workspaces and delegated-provider workspaces in the same server process.
5. Update Toonverse to select delegated source transfer explicitly and to branch from the workspace's resolved transfer contract rather than `ToolStorageService.transferMode()`.
6. Add a qBittorrent tool only after its completed-file collection path can request local-byte staging and a permitted external handoff policy without changing the global lifecycle.
7. Add regression coverage for property binding, transfer validation, inline/queued local-byte publication, delegated reservation/seal/status, and rejection of delegated-plus-Meshingress-async combinations.
8. Reconcile and update the related delegated-source and streaming-ingest architect records with the implemented outcome.
