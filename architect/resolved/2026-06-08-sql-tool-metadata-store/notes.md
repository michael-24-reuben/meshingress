# Notes

## 2026-06-14 Activation

The user selected this SQL metadata-store architect as the next foundation objective and asked that table identity and SQL configuration live in the property file unless a separate entry is clearly better.

Decision: keep SQL table identity and repository SQL configuration in this architect. The first implementation slice should bind configuration through `MeshingressRepositoryProperties` and `app/meshingress-repository/src/main/resources/application.properties`.

Recommended first slice before code approval:

- add a repository metadata-store boundary behind `ArtifactService`;
- make store selection property-driven;
- keep filesystem artifact binaries and raw reports on disk;
- persist canonical metadata, lifecycle state, checksums, assessment summaries, raw report references, and publication records in SQL;
- use configured schema/table identities instead of hard-coded physical table names;
- stop before implementation until the user approves the plan.
