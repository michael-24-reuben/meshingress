# Todo

## Model and Contract

- [x] Add `ToolRegistrationPhase` enum with `EXPERIMENTAL`, `STAGING`, `BUNDLE`, `NATIVE`.
- [x] Add `ToolSourceKind` enum with `LOCAL_JAR`, `MAVEN_COORDINATES`, `CLASSPATH_BUNDLE`, `SERVER_NATIVE`.
- [x] Add request DTO for `roles/tools/register`.
- [x] Add response DTO for registration result.
- [x] Add HTTP context/provenance DTO.

## Controller

- [x] Update the role tool registration path through `RoleToolService`.
- [x] Add or route `POST /roles/tools/register`.
- [x] Parse `phase` from JSON-RPC params as the HTTP-equivalent context for `/mcp`.
- [x] Delegate to a registration service instead of calling runtime loader directly.

## Service Layer

- [x] Add `ToolRegistrationService`.
- [x] Add `ToolRegistrationStrategy` interface.
- [x] Implement `ExperimentalToolRegistrationStrategy`.
- [x] Implement `StagingToolRegistrationStrategy`.
- [x] Implement `BundleToolRegistrationStrategy`.
- [x] Implement `NativeToolRegistrationStrategy`.
- [x] Add per-tool locking around phase registration.

## Experimental Phase

- [x] Locate previous active experimental attachment for same tool ID.
- [x] Deactivate/unregister previous experimental attachment before new activation.
- [x] Close dynamic classloader/runtime handle if applicable.
- [x] Activate `LocalJarSource`.
- [x] Persist provenance with `phase=experimental`.

## Staging Phase

- [x] Validate Maven coordinates.
- [x] Activate `MavenCoordinatesSource`.
- [x] Decide and implement conflict policy for same tool ID.
- [x] Persist provenance with `phase=staging`.

## Bundle Phase

- [x] Review `lib/meshingress-tool-runtime-loader/src/main/java/dev/mrk/meshingress/runtime/bundle/ClasspathToolBundle.java`.
- [x] Add lookup/reconciliation support for bundled classpath tools if missing.
- [x] Ensure no runtime external code install occurs for `phase=bundle`.
- [x] Return deterministic error if bundle tool is not present.

## Native Phase

- [x] Define reserved native namespaces.
- [x] Reject native registration for non-native tool IDs.
- [x] Reject external override of native tools.
- [x] Prefer internal bootstrap path for native registrations.

## Policy and Config

- [x] Extend `MeshingressProperties.Tools` with nested `Registration` properties.
- [x] Add `StagingConflictPolicy` enum.
- [x] Bind `meshingress.tools.registration.*` from application configuration.
- [x] Add config flags for experimental/staging overrides.
- [x] Enforce admin control-plane access for dynamic phases.
- [x] Enforce admin control-plane access for bundle reconciliation.
- [x] Treat native phase as internal-only unless `allow-native-http=true`.

## Audit and Verification

- [x] Persist active registration records with source metadata and request provenance.
- [x] Mark replaced experimental and staging records in the registration store.
- [x] Persist source metadata and HTTP request context.
- [x] Add MVC tests for legacy registration, bundle reconciliation, bundle-not-present behavior, native default denial, and existing MCP behavior.
