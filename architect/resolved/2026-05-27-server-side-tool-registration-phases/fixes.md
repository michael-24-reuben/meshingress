# Fixes

- Added phase/source models and request/result/provenance records under `controller/roles/registration`.
- Added `ToolRegistrationService` with per-tool locking and strategy dispatch.
- Added phase strategies:
  - `experimental` validates local JAR source, checksum requirement, override policy, replacement, and `LocalJarSource` activation.
  - `staging` validates Maven coordinates, conflict policy, override policy, replacement, and `MavenCoordinatesSource` activation.
  - `bundle` reconciles existing classpath/bundled tools without external runtime activation.
  - `native` reconciles reserved server-native namespaces only when native HTTP registration is explicitly enabled.
- Updated `RoleToolService` so phase-aware requests delegate to the new service while legacy descriptor registration continues to use the existing registry flow.
- Extended `MeshingressProperties.Tools` with `Registration` config and `StagingConflictPolicy`.
- Added `meshingress.tools.registration.*` defaults in `application.properties`.
- Added MVC tests for bundle reconciliation, missing bundle tools, and native HTTP default denial.
