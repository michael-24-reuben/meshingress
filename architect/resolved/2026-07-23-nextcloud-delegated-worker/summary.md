# Summary

Resolved on `development`. A completely re-architected Nextcloud delegated-source integration has been implemented under `integrations/nextcloud/meshingress`, cleanly replacing the legacy v1 implementation which remains preserved as `meshingress-v1`.

The new app enforces an explicit state machine:
`RESERVE -> OPEN -> SEALED -> QUEUED -> RUNNING -> COMPLETED | FAILED`

Key accomplishments:
- Sealed handoff boundary ensures Nextcloud worker owns execution only after the workspace is fully staged with native descriptors and delegated source URLs.
- Granular persistence schema (`meshingress_workspaces` and `meshingress_workspace_sources`) isolates per-source download states and errors.
- Worker lease expiration and auto-recovery for interrupted jobs.
- Safe resume capability for client status monitors without duplicate submissions.
