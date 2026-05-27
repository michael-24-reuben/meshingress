# Todo

## Design

- [x] Decide final module name: `lib/meshingress-tool-runtime-loader`.
- [x] Decide whether artifact resolution lives inside this module or a future `meshingress-tool-artifact-resolver` module.
- [x] Define `ToolArtifactSource`.
- [x] Define `MavenCoordinatesSource`.
- [x] Define `LocalJarSource`.
- [x] Define `PluginDirectorySource`.
- [x] Define `ResolvedToolArtifact`.
- [x] Define `ToolModuleDescriptor`.
- [x] Define `ToolModuleId`.
- [x] Define lifecycle states.
- [x] Define activation/deactivation API.
- [x] Define registry bridge API.

## Resolution

- [x] Implement direct JAR resolver as secondary/fallback path.
- [x] Implement Maven coordinate resolver as primary path.
- [x] Resolve local Maven repository artifacts.
- [ ] Resolve remote/private repository artifacts.
- [x] Resolve transitive runtime dependencies available in the local Maven repository.
- [ ] Cache downloaded artifacts.
- [ ] Add checksum metadata.

## Loading

- [x] Create isolated classloader factory.
- [x] Decide parent-first versus child-first rules.
- [x] Keep Meshingress API classes parent-owned.
- [ ] Keep tool-private dependencies child-owned where possible.
- [x] Create child Spring application context factory.
- [x] Load module auto-configuration.
- [x] Discover tool beans.

## Registry

- [x] Register tool beans with the parent tool registry.
- [x] Track which module owns each registered tool.
- [x] Prevent duplicate tool IDs unless reload policy allows replacement.
- [x] Deregister tools on deactivate.
- [ ] Reject new calls while a module is quiescing.

## Safety

- [ ] Add repository allowlist.
- [ ] Add artifact checksum verification.
- [ ] Add optional signature verification.
- [x] Add duplicate artifact policy.
- [ ] Add snapshot artifact policy.
- [ ] Add audit events for activation/deactivation.
- [ ] Add approval gate for high-risk loaded tools.

## Verification

- [ ] Create a minimal test tool module.
- [x] Build and install it into local Maven repository.
- [ ] Activate it by Maven coordinates.
- [ ] Verify tools/list sees the loaded tool.
- [ ] Verify tools/call can invoke the loaded tool.
- [ ] Deactivate the module.
- [ ] Verify tools/list no longer exposes it.
- [ ] Verify active calls drain or fail predictably.
- [ ] Verify failed activation does not leave partial registry state.

