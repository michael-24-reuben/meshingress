# Brief: Runtime Publication Install Hardening

## Goal

Make server-side publication installation durable, transactional, and repository-backed instead of relying on server-visible repository filesystem paths.

## Scope

- Fetch approved artifacts from the repository API instead of assuming shared filesystem visibility.
- Verify checksum and publication signature before and after cache copy.
- Persist runtime publication registrations so installed tools survive restart.
- Reconcile runtime cache and registration state on startup.
- Add transactional rollback across cache copy, runtime activation, scope validation, registry save, and tool-list notification.
- Add operational metrics and audit logs for install, rollback, deactivate, and restart reconciliation.

## Dependency

This depends on durable metadata from `2026-06-08-sql-tool-metadata-store` and stronger publication verification from `2026-06-14-publication-trust-provenance-policy`.

