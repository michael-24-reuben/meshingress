# Assessment

Rewriting the Nextcloud integration from scratch was necessary to eliminate the structural technical debt of legacy import tables and ad-hoc Base64 JSON upload paths.

Key design points:
- **Immutability After Seal**: Once a workspace transitions to `SEALED`, further file uploads or source additions are strictly rejected.
- **Per-Source State Isolation**: Retrying a failed image download does not delete or re-download already completed files in the same workspace.
- **Dedicated CLI Worker**: The worker command (`meshingress:source-import:work`) can run continuously with interval polling or via cron, managing task leases safely without risking duplicate executions.
