# Todo

- [x] Activate the SQL metadata-store architect as the next foundation objective.
- [x] Record that SQL table identity and repository SQL config belong in this same architect rather than a separate entry.
- [x] Get user approval for the first implementation slice before changing code.
- [x] Inventory first-slice repository fields from artifact records, publication records, assessment summaries, file entries, artifact paths, and lifecycle events.
- [x] Decide SQL implementation target for the first slice: Spring JDBC with a narrow repository metadata-store abstraction.
- [x] Add repository property defaults for SQL store selection, datasource defaults, schema, table prefix, and explicit table identities.
- [x] Draft and bootstrap first-slice SQL tables for artifacts, artifact files, assessments, publications, and lifecycle events.
- [x] Keep raw artifact binaries, quarantine files, assessment JSON, and SBOM JSON on the filesystem while moving canonical metadata to SQL.
- [x] Add append-only lifecycle events for upload, assess, approve, and publish in the repository slice.
- [x] Introduce a first-slice repository metadata-store boundary for artifacts, assessments, artifact paths, publications, and lifecycle events.
- [x] Add tests proving SHA-256, artifact metadata, file entries, trust status, assessment metadata, and publication metadata are stored and queryable from a fresh SQL store instance.

## Deferred Follow-Up

- [ ] Migrate `InMemoryToolRegistrationStore` to a SQL-backed `ToolRegistrationStore` in a later server/runtime registration slice.
- [ ] Add deleted/replaced/revoked query behavior for direct registration and runtime install history after repository SQL metadata is stable.
- [ ] Expand normalized SQL coverage for tool catalog/source/function entities when the runtime registration work starts.
