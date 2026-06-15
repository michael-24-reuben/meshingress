# Plan

## Approval Gate

This architect is active for design and implementation planning. Do not begin code changes for the SQL store until the user approves the first implementation slice.

## First Implementation Slice

Start with `app/meshingress-repository` metadata persistence. The current pressure point is `ArtifactService`, which keeps `ArtifactRecord`, assessment results, artifact paths, and `ArtifactPublicationRecord` instances in process-local maps while also writing JSON snapshots to the repository filesystem.

The first slice should introduce a repository metadata-store boundary and a SQL-backed implementation without changing scanner behavior, publication trust rules, or runtime installation semantics.

## SQL Configuration Contract

Keep SQL configuration in `app/meshingress-repository/src/main/resources/application.properties` and bind it through `MeshingressRepositoryProperties`.

Proposed properties:

```properties
meshingress.repository.metadata-store=sql
meshingress.repository.sql.schema=meshingress
meshingress.repository.sql.table-prefix=repository_
meshingress.repository.sql.table.artifacts=repository_artifacts
meshingress.repository.sql.table.artifact-files=repository_artifact_files
meshingress.repository.sql.table.assessments=repository_assessments
meshingress.repository.sql.table.publications=repository_publications
meshingress.repository.sql.table.lifecycle-events=repository_lifecycle_events
meshingress.repository.sql.initialize-schema=true
```

Use standard Spring datasource properties for the actual connection:

```properties
spring.datasource.url=jdbc:h2:file:./repository/sql/meshingress-repository
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```

The table identity belongs in this same SQL architect because table naming, schema, prefix, and migration/bootstrap behavior are part of the persistence contract. A separate architect is only needed if this grows into multi-database migration tooling or cross-service schema governance.

## Phase 1: Model Inventory

- Inventory current direct registration records, repository artifact records, publication records, assessment summaries, function descriptors, and lifecycle events.
- Identify which fields are authoritative, derived, or diagnostic.
- Preserve the trust boundary between direct registration and repository-backed publication installation.
- For the first implementation slice, prioritize `ArtifactRecord`, artifact file entries, assessment summary/results, artifact path lookup, publication records, and lifecycle events in the repository app.

## Phase 2: SQL Schema Draft

- Draft SQL tables for tools, registrations, sources, checksums, functions, artifacts, files, scope policies, assessments, publications, and lifecycle events.
- Include soft-delete lifecycle state rather than physical delete as the default.
- Include extensible metadata columns such as JSON fields and `etc` planning placeholders where the model is expected to grow.
- Use the configured schema/table properties for physical table names; do not hard-code table names into SQL adapters beyond property defaults.

## Phase 3: Persistence Adapter

- Add a SQL-backed implementation behind existing registration/repository service boundaries.
- Keep in-memory behavior available for tests or local development until the SQL path is stable.
- Add migration or bootstrap logic for existing file-backed repository metadata if needed.
- Prefer a narrow `ArtifactMetadataStore` interface for the first slice so `ArtifactService` stops owning `ConcurrentHashMap` metadata directly.
- Keep raw artifact binaries, extracted quarantine files, SBOM JSON, and raw assessment reports on the repository filesystem; SQL stores canonical metadata and durable references to those files.

## Phase 4: API and Query Behavior

- Update list/read APIs to distinguish active records from historical records.
- Add filters for active, deleted, replaced, revoked, phase, tool type, checksum status, trust status, and source kind.
- Ensure delete operations mark lifecycle state and append an audit event.
- Keep existing upload, metadata, assess, approve, publish, assessment, and publication API behavior stable in the first slice.

## Phase 5: Verification

- Add repository and server tests proving metadata survives process/service reconstruction.
- Add tests proving deleted tools remain queryable as historical rows but are excluded from active tool listing.
- Add checksum and publication metadata assertions for both direct registration and repository-backed installation paths.
- Add repository tests with SQL enabled that upload, assess, approve, publish, rebuild the service/store, and read metadata/publication back from SQL.
