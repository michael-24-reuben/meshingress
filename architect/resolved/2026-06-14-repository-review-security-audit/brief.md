# Brief: Repository Review Security and Audit

## Goal

Secure the repository API and make human review state transitions auditable.

## Scope

- Add explicit auth/role gates for upload, assess, approve, publish, revoke, and metadata access.
- Distinguish uploader, reviewer, publisher, and admin responsibilities.
- Record actor, request id, timestamp, old state, new state, and reason for every lifecycle transition.
- Add a review queue or API shape that exposes assessment details, SBOM summary, scope findings, and approve/deny controls.
- Add operational logs and basic metrics for repository state transitions.

## Dependency

This should build on `2026-06-08-sql-tool-metadata-store` so audit and review history are durable instead of only file-backed or in-memory.

