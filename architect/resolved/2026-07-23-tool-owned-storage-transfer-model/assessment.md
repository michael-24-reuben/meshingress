# Assessment

The prior global lifecycle selected one entire `ToolStorageService`. That made a Nextcloud delegated deployment incompatible with a local-byte producer in the same process, and it encoded asynchronous scheduling in the lifecycle name.

The implementation separates these axes:

- `meshingress.storage.lifecycle`: `LOCAL_LOCAL` or `LOCAL_EXTERNAL` for Meshingress-owned bytes only.
- `ToolStorageWorkspaceRequest.transferMode`: `LOCAL_BYTES` or `DELEGATED_SOURCE_URLS`, selected by the tool method.
- `ToolStorageWorkspaceRequest.localPublicationMode`: `INLINE` or `QUEUED`, valid only for local-byte workspaces.

`DELEGATED_SOURCE_URLS + localPublicationMode` is rejected. The delegated destination owns its import job and Meshingress has no source bytes to queue for handoff.

## Reopened startup regression

An explicit blank `meshingress.storage.external.delegated-target` satisfied Spring's generic property condition even though it cannot select a delegated target. The viewer factory then reached its intentional validation error during startup. A dedicated condition now requires a nonblank target name before either the factory or viewer route is registered.
