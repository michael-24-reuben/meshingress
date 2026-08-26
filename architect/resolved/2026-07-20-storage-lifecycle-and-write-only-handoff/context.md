# Context

## Current implementation

The resolved `2026-07-17-ephemeral-content-storage` record established short-lived local workspaces. The server currently writes under one local root, atomically moves a complete staging directory to the local published directory, serves files through `/storage/{sessionId}/{requestId}/files/...`, and deletes expired/exhausted local content.

The current configuration is one `meshingress.storage.*` block. `storage.properties.md` contains the proposed replacement layout. The relevant implementation currently binds a single `MeshingressProperties.Storage` record and uses a filesystem-only `WorkspaceFiles` implementation plus JDBC workspace metadata.

## Decisions already made

- No valuable deployments require compatibility aliases. The properties and Java configuration model may change completely.
- The lifecycle is one enum property: `local-local`, `local-external`, or `external-external`.
- A selected external target is configured once through `meshingress.storage.external.default-target`; target connection details live under `meshingress.storage.external.targets.<target-name>`.
- SQL metadata is shared by all lifecycle modes. It is not a per-target concern and does not contain file bytes.
- A foreign destination is owned by its provider after successful handoff. Meshingress retains an audit record but must not later read, overwrite, append to, delete, clear, clean up, or reconcile the foreign object.
- Create-only applies to every storage backend, including local storage. A collision must fail rather than overwrite.
- The file API defines the full operation set. A backend explicitly exposes only the operations it supports through capabilities and returns a stable denied/unsupported result for disallowed calls. This keeps the API uniform without silently granting foreign read or mutation access.

## Related entry

`architect/resolved/2026-07-17-ephemeral-content-storage` remains the evidence for the implemented local workspace model. This pending entry is its successor for a broader storage-backend lifecycle; it does not reopen the resolved work.

## Existing worktree boundary

The repository has unrelated in-progress server, MCP progress, audit, Aegis, and generated-client changes. This entry must be implemented in a dedicated future slice without modifying or absorbing those changes.
