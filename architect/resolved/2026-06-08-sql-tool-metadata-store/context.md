# Context

## Current State

Direct MCP registration metadata is currently represented by `ToolRegistrationRecord` and stored by `InMemoryToolRegistrationStore`. That path captures useful fields such as registration id, tool id, phase, source kind, status, source metadata, actor, request id, runtime module id, and registered functions, but it is not durable.

Repository-backed artifact and publication metadata is represented by model records such as `ArtifactRecord`, `ArtifactPublicationRecord`, `ArtifactAssessmentSummary`, `ArtifactScopeDeclaration`, `ArtifactFileEntry`, and `ArtifactChecksum`. That path captures richer trust metadata including artifact checksum, file entries, requested/inferred/approved/denied scopes, assessment summary, publication signature, provenance, revocation, and timestamps.

The durable SQL store should converge these surfaces without erasing the boundary between:

- direct registration, which is development-oriented unless later promoted;
- repository-backed publication installation, which is the production trust path.

## Design Motivation

The project needs a historical tool catalog rather than only a live registry snapshot. Keeping deleted tools as rows marked `deleted` supports audit, rollback analysis, conflict checks, provenance review, and future admin UI history.

## Active Scope

This architect was activated on 2026-06-14 as the next foundation objective. The first implementation slice should start with repository metadata persistence because `ArtifactService` currently owns in-memory maps for artifact records, artifact paths, assessment results, and publication records.

SQL table identity should be configured through repository properties in this same architect. The table names, schema, prefix, schema initialization flag, and datasource defaults are part of the SQL persistence contract, not a separate workflow unless the project later needs standalone schema governance.

`2026-05-28-direct-registration-hardening` focused on immediate checksum/provenance gaps in direct registration. This SQL store is the larger persistence design that should support both repository-backed publication metadata and later direct-registration durability without collapsing their trust boundaries.

## Related Follow-Up Architects

- `2026-06-14-repository-review-security-audit` depends on this store for durable review events, actor/request metadata, and lifecycle history.
- `2026-06-14-repository-scanner-sandbox-pipeline` depends on this store for raw-report references, scanner summaries, and assessment outcomes.
- `2026-06-14-publication-trust-provenance-policy` depends on this store for publication policy inputs, provenance, signatures, and key lifecycle metadata.
- `2026-06-14-runtime-publication-install-hardening` depends on this store for durable runtime publication registrations and restart reconciliation.
