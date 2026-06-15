# Assessment

## Result

The first SQL metadata-store slice is implemented for `app/meshingress-repository`.

The repository previously kept canonical artifact records, artifact paths, assessment results, and publication records in `ArtifactService` process-local maps while also writing `record.json`, `latest-review.json`, and `publication.json` metadata snapshots. That made repository metadata non-durable and created a competing JSON metadata-store shape.

## Implemented Scope

- Added property-driven SQL metadata-store configuration.
- Added a repository metadata-store interface.
- Added a SQL implementation using Spring JDBC and H2 defaults.
- Persisted canonical repository metadata for artifacts, artifact files, assessments, publications, and lifecycle events.
- Removed canonical JSON metadata snapshot writes from the active repository flow.
- Preserved filesystem storage for artifact binaries, quarantine extraction, raw `assessment.json`, and raw `cyclonedx-sbom.json`.

## Remaining Scope

Direct server registration metadata still uses `InMemoryToolRegistrationStore`. That work was intentionally deferred because the verified first slice focused on repository artifact/publication metadata and the server registration path has different lifecycle and trust semantics.
