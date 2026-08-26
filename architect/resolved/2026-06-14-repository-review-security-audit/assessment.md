# Assessment

The repository review/security/audit slice needed a narrow API boundary before broader scanner, provenance, or runtime-install hardening could safely build on it.

The repository module previously had the core upload, assessment, approval, and publication flow, but review visibility and later lifecycle transitions were incomplete. Reviewers needed a role-gated API surface for pending artifacts, and repository state changes needed durable lifecycle events that identify actor, request id, old state, new state, timestamp, and reason.

The completed slice keeps the first security model intentionally local and header-based. It defines repository roles for uploader, reviewer, publisher, and admin, gates repository actions through `RepositoryAccessPolicy`, and stores append-only lifecycle events for upload, assess, approve, reject, publish, revoke, delete, and restore.

Delete/restore were kept repository-local and non-destructive. Delete is a soft state transition to `DELETED`; restore uses the latest durable `DELETE` event's `from_state` as the source of truth for the restored status. Physical deletion, retention policy, scanner sandboxing, stronger publication provenance, and runtime install hardening remain separate follow-up entries.
