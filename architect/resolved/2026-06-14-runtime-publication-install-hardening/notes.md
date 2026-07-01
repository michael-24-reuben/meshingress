# Notes

## 2026-06-18 Activation

- Activated after `2026-06-14-publication-trust-provenance-policy` established Ed25519 verification, key IDs, and revoked-key rejection.
- The safest first implementation slice is failure-injection coverage around the existing install mutation sequence, followed by compensating rollback.
- Repository API transport and restart durability remain in scope but should follow a verified transaction boundary.

## 2026-06-18 Registration Save Rollback Slice

- Mutation inventory for current `ArtifactInstaller.install`: verify signature, require installable publication, copy artifact into runtime cache, activate runtime module, resolve registered functions from the registry, require approved scopes, build a registration record, save the active registration, then return registry version.
- Existing compensation only covered approved-scope validation after activation. A `registrationStore.saveActive` failure could leave an activated runtime module without an active durable registration record.
- Added `ArtifactInstallerRollbackTests.installDeactivatesRuntimeModuleWhenRegistrationSaveFails` with fake collaborators to prove a post-activation save failure deactivates the runtime module and preserves the original save exception.
- Updated `ArtifactInstaller` so all post-activation install steps run inside the same rollback envelope. If any runtime exception occurs before success, `runtimeLoader.deactivate(handle.moduleId())` is attempted and any deactivation failure is attached as a suppressed exception.
- Verified with focused installer rollback test and existing `McpPublicationInstallTests`; both passed.

## 2026-06-19 Runtime Cache Cleanup Rollback Slice

- Added `RuntimeToolCache.remove(Path)` to delete a cached runtime jar only when the resolved path stays under the configured runtime cache root, then prune empty coordinate directories.
- Extended `ArtifactInstaller` so any install failure after `runtimeToolCache.install(...)` attempts runtime-cache removal. If activation already returned a handle, runtime deactivation still runs first; cleanup failures are attached as suppressed exceptions on the original install failure.
- Extended `ArtifactInstallerRollbackTests` to cover cache removal when activation fails before a module handle exists and when registration save fails after activation.
- Verification required a local install of the existing untracked `toolspace/video-loop` dependency because `app/meshingress-tool-bundle` references it but the root reactor does not include it. The first attempt also showed the module's unrelated test-classpath and boot-repackage gaps, so the local jar was installed with test compilation and repackage skipped.
- Verified with `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=ArtifactInstallerRollbackTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: 2 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- Verified with `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=McpPublicationInstallTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: 12 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- Ran `git diff --check -- app/meshingress-server/src/main/java/dev/mrk/meshingress/server/install/ArtifactInstaller.java app/meshingress-server/src/main/java/dev/mrk/meshingress/server/install/RuntimeToolCache.java app/meshingress-server/src/test/java/dev/mrk/meshingress/server/install/ArtifactInstallerRollbackTests.java`; no whitespace errors.

## 2026-06-20 Cleanup Failure Suppression Coverage

- Added focused failure-injection coverage for cleanup failure reporting without changing production installer code.
- `installSuppressesRuntimeCacheRemovalFailureWhenActivationFails` proves an activation failure remains authoritative when runtime-cache removal also fails; the cache-removal exception is attached as a suppressed exception and no deactivation is attempted because no module handle exists.
- `installSuppressesDeactivationAndRuntimeCacheRemovalFailuresWhenRegistrationSaveFails` proves a registration-save failure remains authoritative when both runtime deactivation and runtime-cache removal fail; both cleanup exceptions are attached as suppressed exceptions in the compensation order.
- The first focused Maven run failed at test compilation because the subclassed cache fake could not access private recorded fields on `RecordingRuntimeToolCache`; changed those fake fields to package-visible inside the test class only.
- Verified with `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=ArtifactInstallerRollbackTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: 4 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- Verified with `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=McpPublicationInstallTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: 12 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- Ran `git diff --check -- app/meshingress-server/src/test/java/dev/mrk/meshingress/server/install/ArtifactInstallerRollbackTests.java`; no whitespace errors.
- Runtime install hardening remains active. Durable registration persistence, restart reconciliation, repository API fetch, and deactivation/revocation behavior are still unfinished.

## 2026-06-20 Post-Registration Rollback Slice

- Added `ArtifactInstallerRollbackTests.installMarksSavedRegistrationRolledBackWhenFinalRegistryVersionFails` to cover the failure boundary after `registrationStore.saveActive(...)` succeeds but the final registry version read fails.
- The first focused run failed as expected: the saved registration record stayed active because `ArtifactInstaller` had no compensation for failures after registration persistence succeeded.
- Updated `ArtifactInstaller` to keep the record returned by `saveActive(...)` and, on any later runtime exception, call `registrationStore.markStatus(registrationId, "install-rolled-back")` before runtime deactivation and runtime-cache removal. If the status update fails, that persistence failure is attached as a suppressed exception on the original install failure.
- Verified with `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=ArtifactInstallerRollbackTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: 5 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- Verified with `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=McpPublicationInstallTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: 12 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- Ran `git diff --check -- app/meshingress-server/src/main/java/dev/mrk/meshingress/server/install/ArtifactInstaller.java app/meshingress-server/src/test/java/dev/mrk/meshingress/server/install/ArtifactInstallerRollbackTests.java`; no whitespace errors. Git warned that LF in `ArtifactInstaller.java` will be replaced by CRLF when Git next touches it.
- Runtime install hardening remains active. Durable publication registration storage, startup reconciliation, repository API fetch, direct publication deactivation/revocation behavior, and restart/missing-cache tests remain unfinished.

## 2026-06-21 Durable Registration Store Slice

- Replaced the production `ToolRegistrationStore` bean with `FileToolRegistrationStore`, while leaving `InMemoryToolRegistrationStore` available for focused unit tests that instantiate it directly.
- Added `meshingress.repository.runtime-registration-store-path`, defaulting to `runtime/tool-registrations.json` relative to `meshingress.repository.root`. Runtime publication install records and status changes now persist through atomic whole-file JSON writes.
- Added `FileToolRegistrationStoreTests` to prove saved `PUBLICATION_RECORD` registrations survive a new store instance and that `install-rolled-back` status updates persist and are no longer treated as active after reload.
- The first focused run failed only because `OffsetDateTime` deserialized as the same instant normalized to UTC; the test now asserts instant equality instead of preserving the original textual offset.
- Verified with `.\mvnw.cmd -f toolspace\video-loop\pom.xml install "-Dmaven.test.skip=true" "-Dspring-boot.repackage.skip=true"`: `BUILD SUCCESS`.
- Verified with `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=FileToolRegistrationStoreTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: 2 tests, 0 failures/errors/skips, `BUILD SUCCESS` after the assertion fix.
- Verified with `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=ArtifactInstallerRollbackTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: 5 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- Verified with `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=McpPublicationInstallTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: 12 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- Ran `git diff --check -- app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/InMemoryToolRegistrationStore.java app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/FileToolRegistrationStore.java app/meshingress-server/src/test/java/dev/mrk/meshingress/controller/roles/registration/FileToolRegistrationStoreTests.java lib/meshingress-config/src/main/java/dev/mrk/meshingress/config/MeshingressProperties.java app/meshingress-server/src/main/resources/application.properties`; no whitespace errors. Git warned that LF in three existing files will be replaced by CRLF when Git next touches them.
- Runtime install hardening remains active. Startup reconciliation, repository API fetch, direct publication deactivation/revocation behavior, and missing-cache recovery tests remain unfinished.

## 2026-06-21 Startup Reconciliation Slice

- Added `RuntimePublicationRegistrationReconciler` as an application-start runner. It reads persisted active `PUBLICATION_RECORD` registration records, resolves their stored `runtimeCachePath`, activates the cached jar through `ToolRuntimeLoader`, and marks the record `reconciled`.
- Missing or blank runtime-cache paths are marked `missing-cache` and are no longer treated as active by the durable store.
- Added focused `RuntimePublicationRegistrationReconcilerTests` for successful cached-jar reconciliation and missing-cache recovery.
- The first `McpPublicationInstallTests` verification run failed because the suite intentionally dirties Spring contexts but reuses a static temp directory; the new durable store let one successful install leak into the next context, where startup reconciliation activated the prior module and changed an unapproved-scope assertion into an already-active internal error. Added runtime store/cache cleanup before and after each test method to preserve the existing test isolation contract.
- Verified with `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=RuntimePublicationRegistrationReconcilerTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: 2 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- Verified with `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=FileToolRegistrationStoreTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: 2 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- Verified with `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=ArtifactInstallerRollbackTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: 5 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- Verified with `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=McpPublicationInstallTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: first run failed as described above; rerun after test-isolation cleanup passed with 12 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- Ran `git diff --check -- app/meshingress-server/src/main/java/dev/mrk/meshingress/server/install/RuntimePublicationRegistrationReconciler.java app/meshingress-server/src/test/java/dev/mrk/meshingress/server/install/RuntimePublicationRegistrationReconcilerTests.java app/meshingress-server/src/test/java/dev/mrk/meshingress/mcp/McpPublicationInstallTests.java`; no whitespace errors. Git warned that LF in `McpPublicationInstallTests.java` will be replaced by CRLF when Git next touches it.
- Runtime install hardening remains active. Repository API artifact fetch, direct publication deactivation/revocation behavior, and repository-fetch checksum mismatch coverage remain unfinished.

## 2026-06-22 Repository API Artifact Fetch Slice

- Added a repository artifact download endpoint at `GET /artifact/{groupId}/{artifactId}/{version}/file`, guarded by the existing repository `READ` action, returning the stored artifact jar as `application/octet-stream`.
- Added `meshingress.repository.api-base-url` and `meshingress.repository.api-role` server configuration. A blank base URL preserves local repository file resolution; a configured URL makes runtime cache installation fetch artifact bytes from the repository API with `X-Repository-Role` and `X-Repository-Actor` headers.
- Added `RepositoryArtifactFetcher` as the runtime install fetch boundary. It validates `meshingress-repository://artifact/...` URIs, downloads configured repository artifacts, maps repository denial to JSON-RPC forbidden errors, and keeps checksum verification in `RuntimeToolCache` before atomically moving the staged jar into the cache.
- Updated `RuntimeToolCache` to stage fetched artifacts into a temp file, verify the publication checksum before moving into place, and delete failed temp files. This preserves the existing signature, trust, and scope gates while moving transport out of the cache layout code.
- Added repository flow coverage that downloads the approved uploaded jar through the new repository endpoint and byte-compares it with the uploaded multipart content.
- Added `RuntimeToolCacheTests` for blank-URL local fallback, configured repository API download including the publisher role header, and repository-fetch checksum mismatch without leaving a cache file behind.
- The first `RuntimeToolCacheTests` verification attempt failed at test compilation because `FileToolRegistrationStoreTests` constructed `MeshingressProperties.Repository` without the new API fields; the constructor call was updated.
- The first `McpPublicationInstallTests` verification attempt failed application-context startup because `RepositoryArtifactFetcher` exposed only a package-private test constructor and Spring could not choose a default constructor. Added an explicit `@Autowired` production constructor.
- The next `McpPublicationInstallTests` attempt exposed the local fallback temp-file bug: `Files.copy` was writing to a pre-created temp file without `REPLACE_EXISTING`, causing local publication installs to surface as internal errors. The local copy now uses `StandardCopyOption.REPLACE_EXISTING`.
- Verified with `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=ArtifactRepositoryFlowTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: 6 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- Verified with `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=RuntimeToolCacheTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: 3 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- Verified with `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=McpPublicationInstallTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: 12 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- Verified with `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=ArtifactInstallerRollbackTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: 5 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- Ran `git diff --check --` for the repository fetch files; no whitespace errors. Git warned that LF in several edited files will be replaced by CRLF when Git next touches them.
- Runtime install hardening remains active. Publication-record fetch, direct deactivation/revocation behavior, and rollback-after-partial-activation tests remain unfinished.

## 2026-06-22 Publication Delete And Partial Activation Rollback Slice

- Added runtime-cache cleanup to the existing `roles/tools/delete` phase-registration path for active `PUBLICATION_RECORD` registrations. When a publication-backed registration has a `runtimeModuleId`, deletion now deactivates the runtime module and removes the cached runtime JAR referenced by the registration `runtimeCachePath` before marking the record `deleted`.
- Extended delete responses with aggregate and per-record `runtimeCacheRemoved` fields so callers can distinguish runtime deactivation from cache cleanup.
- Added focused `ToolRegistrationServiceTests.deleteDeactivatesPublicationRuntimeModuleAndRemovesRuntimeCache` coverage for publication-backed delete behavior.
- Added `McpPublicationInstallTests.adminCanDeleteInstalledPublicationAndRemoveRuntimeCache`, which installs a signed publication, verifies the runtime cache JAR exists, deletes the publication-backed tool through `roles/tools/delete`, verifies cache removal, and confirms `tools/call` no longer sees the function.
- Added `ArtifactInstallerRollbackTests.installDeletesRuntimeCacheWhenPartialActivationFailsBeforeHandleReturns` to name and cover the partial-activation rollback boundary where activation starts but throws before a module handle is returned.
- Verified with `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=ToolRegistrationServiceTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: 2 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- Verified with `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=McpPublicationInstallTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: 13 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- Verified with `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=ArtifactInstallerRollbackTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: 6 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- Verified with the combined command `.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=ToolRegistrationServiceTests,ArtifactInstallerRollbackTests,McpPublicationInstallTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`: 21 tests, 0 failures/errors/skips, `BUILD SUCCESS`.
- Ran `git diff --check --` for the edited service and test files; no whitespace errors. Git warned that LF in edited files will be replaced by CRLF when Git next touches them.
- Runtime install hardening remains active only because the publication-record repository client contract/fetch item is still unchecked. Direct publication deactivation/revocation behavior and rollback-after-partial-activation tests are complete.

## 2026-06-23 Publication Record Fetch And Resolution

- Added repository publication-record fetch support to the existing server repository fetch boundary. When `meshingress.repository.api-base-url` is configured, `RepositoryArtifactFetcher.fetchPublication(...)` downloads `GET /artifact/{groupId}/{artifactId}/{version}/publication` with the configured repository role and the `meshingress-server` actor.
- Extended `roles/tools/installPublication` params so callers can supply either an inline signed `publication` payload or a repository `coordinate`. Inline payloads remain supported; coordinate-based installs fetch the signed publication record before running the existing signature, trust, checksum, activation, scope, and registration flow.
- Added focused fetch coverage in `RuntimeToolCacheTests` for successful publication-record download with role header propagation and for rejecting coordinate-based publication fetch when no repository API base URL is configured.
- Verified the existing direct publication install path still works after the service wiring change with `McpPublicationInstallTests`.
- This closes the final runtime install hardening checklist item. Scanner/sandbox policy, publication eligibility, and unrelated direct-registration cleanup remain separate follow-up work.
