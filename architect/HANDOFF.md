# Assignment Handoff

## Assignment Status
- assignmentStatus: active
- lastUpdatedAt: 2026-09-14T10:08:48-04:00
- updatedBy: Codex
- currentBranch: development (checked during reconciliation)
- expectedBranch: development
- objectiveId: 2026-09-05-workflow-studio-scene-state-ownership
- objectiveTitle: Workflow Studio Scene State Ownership
- objectiveStatus: active

## Current Architect Entries
- primary: [Scene state ownership](active/2026-09-05-workflow-studio-scene-state-ownership/meta.json)
- completedWork: [Checklist](active/2026-09-05-workflow-studio-scene-state-ownership/todo.md)
- implementationEvidence: [Notes](active/2026-09-05-workflow-studio-scene-state-ownership/notes.md)
- related: Active Honeycomb slots, port routing, scene builder, and component lifecycle entries.
- otherActive: Storage provider extraction remains active with authorization policy deferred; its previous handoff is preserved below.

## Completed Implementation Through September 7
- Root maps and stable path identity, public read-only attribute projection, aggregate subscriptions, typed mutations, cross-node selection exclusivity, and independent node input/output capabilities.
- Scene Builder now owns WorkflowModelSnapshot directly; the scene event tracker and bridge adapter are retired.
- Consolidated attribute-name derivation, shared prototype placements, and factory slot initialization without removing state methods.
- Contract coverage includes all nine phase mutation categories, target payloads, and renderer attributes.
- parseWorkflowStudioScene validates and normalizes input with legacy-parser parity coverage; existing legacy callers remain.
- HoneycombWorkflowModelAdapter routes integrated click, hover, focus, and blur overlay changes through the root model. Standalone local fallback remains.

## Remaining Work and Decisions
- Replace renderer-local canvas/slot/node snapshot construction with the approved render-only adapter.
- Decide the public optional scene fallback contract before removing it.
- Migrate remaining legacy scene consumers and verify parser/creator and renderer fallback compatibility before retiring workflow-canvas-scene.ts.
- Preserve every WorkflowStudioState method and use public root APIs.
- The old tracker-based component-lifecycle sketch is historical and cannot be implemented unchanged.

## Verification Details
- Historical implementation result: September 7 notes record npm test with 6 passing tests and npm run build passing with the existing Vite chunk-size advisory.
- Current run: Documentation reconciliation only; no fresh implementation or browser verification.
- Verification gap: Full render-only lifecycle and final legacy-file retirement remain unfinished.

## Last Run Summary
- outcome: Reconciled stale root pointers with the latest active entry at the user's request.
- filesChanged: ASSIGNMENT.md, HANDOFF.md, and primary entry context.md/meta.json.
- implementationChanged: none
- commitCreated: no
- sourceOfTruth: Primary entry metadata, checklist, context, and implementation notes.

## Existing Dirty Work Preserved
Git status shows extensive pre-existing staged, unstaged, and untracked work across server/storage, tool modules, Studio, documentation, and Architect records. The Studio state, renderer, and test directories include untracked work. Root handoff files already had local modifications; prior storage handoff content is retained below. Do not reset, clean, stage, or commit these areas as part of this documentation update. Preserve the earlier generated-artifact boundaries recorded in the historical handoff.

## Next Action
Obtain the owner's decision on the exported standalone scene fallback, then scope the render-only replacement and verify the approved slice with npm test and npm run build from app/meshingress-studio-web. The current request authorizes record updates only.

## Historical Storage-Provider Handoff
The following is the preserved August 31 ledger, not the current assignment. Its timestamps, next actions, verification, and checklist are historical. Storage scope/blacklist policy remains deferred; consult that active entry before resuming it. Some earlier design notes describe superseded lifecycle approaches; the later typed-init implementation section records the replacement.

---
# Assignment Handoff

## Assignment Status
- assignmentStatus: active
- lastUpdatedAt: 2026-08-31T12:23:19-04:00
- updatedBy: Codex
- currentBranch: development
- expectedBranch: development
- objectiveId: 2026-08-29-storage-provider-nodes-and-nextcloud-sdk-extraction
- objectiveTitle: Storage Provider Nodes and Nextcloud SDK Extraction
- objectiveStatus: active

## Current Architect Entries
- primary: architect/active/2026-08-29-storage-provider-nodes-and-nextcloud-sdk-extraction
- terminalEntry: architect/resolved/2026-08-28-tool-distribution-modules
- related: architect/resolved/2026-05-27-server-side-tool-registration-phases; architect/active/2026-07-14-tool-provisioning-module
- pendingFollowUps: provider lifetime/access model and deferred scope enforcement.
- blockedBy: none; user authorized implementation.
- shouldNotTouch: main branch, preserved generated artifacts, credentials, and any transport-based live-provider handoff.

## Objective Scope
- goal: Move real storage providers from native app ownership into attachable modules through typed provider composition.
- scope: Provider metadata, service discovery, typed internal composition, completed Nextcloud/local migration, and deferred provider access policy.
- nonGoals: main-branch work, credential exposure, and transporting live provider instances in response envelopes.
- completionCriteria: Provider nodes are separately discoverable, typed services can be injected internally, concrete storage providers are module-owned, native lifecycle selection is removed, and focused verification passes.
- currentLifecycleState: active.

## Current Implementation Slice

- completed: `@McpService` configuration/discovery, typed injection, `services/list`, distributed Nextcloud/local storage modules, module ownership of the local-provider configuration DTO, and normal typed service-init binding. The server has no native storage implementation, router, lifecycle configuration, provider binding DTO, or `meshingress.storage.*` application values.
- verified: Focused lifecycle scanner, provider registry, injection, and documentation snapshot tests passed after typed service-init binding replaced the opaque invocation bridge. Earlier local-storage/Nextcloud and server MVC verification remains passing.
- next: Await user approval of the provider/consumer scope and blacklist model before implementing authorization semantics.
- notImplemented: provider/consumer scope and blacklist enforcement (explicitly deferred).

## Latest Documentation Slice

- `MeshingressApplication#documentationSnapshotRefresher` now writes `docs/meshingress-tools-and-services.json` from `ToolRegistry` and `McpServiceRegistry` public descriptors.
- The document uses the MCP discovery payload shapes: `tools` contains callable function JSON plus optional `moduleToolId`; `services` contains provider metadata only. Live provider objects, constructor inputs, and credentials are not serialized.
- The path is configurable through `meshingress.documentation.refresh.tools-and-services-output` and defaults to the checked-in `docs/meshingress-tools-and-services.json`.
- `MeshingressApplicationDocumentationSnapshotTests` passed. The running development application refreshed the file with 18 tools and one public service at 2026-08-30T12:57:19-04:00.

## Latest Annotation Slice

- `@McpService` is now a class-only provider-node annotation. Its provider metadata remains unchanged.
- New `@McpFunctionService(type, oneOf)` is parameter-only and represents a callable function's typed internal provider request.
- Scanner schema omission, requirement metadata, and runtime resolver injection moved to `@McpFunctionService`; `ToonverseTool` was migrated.
- Focused verification passed: `McpToolAnnotationScannerTests` (11), `AnnotatedMcpToolHandlerServiceInjectionTests` (1), and `ToonverseToolTests` (9).
- `@McpFunctionService` also carries optional `value`, `title`, and `description`; the scanner emits each nonblank value in its `annotations.requires` discovery object. They describe the dependency and do not affect resolver selection.
- `@McpFunctionService.type()` now defaults to `Void.class`; scanner and invocation resolve that sentinel to the annotated parameter's Java type. Explicit incompatible contracts still fail validation, and `oneOf` continues to be checked against the effective type.

## Latest Service Lifecycle Slice

- `McpServiceLifecycle` is the common capability root. It supplies optional no-argument `init()` and `stop()` hooks; `ToolStorageService` extends it, and registry discovery rejects a provider contract that does not participate in this lifecycle.
- `@McpFunctionService` only describes the dependency and does not add provider configuration to the tool `inputSchema`.
- A concrete provider may declare exactly one typed `void init(...)` overload. Registry discovery scans its parameters with normal tool-function rules and publishes the resulting schema at `requirements[requirementKey].inputSchema`, using consumer `@McpFunctionService.value()` as the object key. The client submits the provider value at `tools/call.params.requirements[requirementKey]`, not under `params.arguments`.
- The invocation handler binds and invokes that typed overload before the tool function. `McpCallContext`, progress, and nested typed services remain injected; client fields use `@McpFunctionParam`. Initialized services receive no-argument `stop()` in reverse order, including tool failure paths. The obsolete `McpServiceInvocation` bridge was removed.
- Focused verification passed: scanner, provider registry, lifecycle injection, and documentation snapshot tests; the affected reactor compiled with tests skipped. `git diff --check` passed with only pre-existing CRLF conversion warnings.

## Last Run Summary
- runEndedAt: 2026-08-31T12:23:19-04:00
- outcome: Provider initialization is now declared with normal typed `init(...)` parameters instead of an opaque lifecycle argument bridge.
- workCompleted: Replaced `initInputType()`/`McpServiceInvocation` with optional no-argument lifecycle hooks plus one scanned typed `void init(...)` overload, updated service discovery and invocation binding, and migrated Nextcloud/test providers.
- workPartiallyCompleted: Scope and blacklist enforcement remains explicitly deferred.
- testsRun: affected-reactor compile; `AnnotatedMcpToolHandlerServiceInjectionTests`, `McpServiceRegistryTests`, `MeshingressApplicationDocumentationSnapshotTests`, and `McpToolAnnotationScannerTests`.
- testResult: The affected reactor compiled and all listed focused tests passed. A local-storage source/test import omission from the previous configuration move was corrected during the compile check.
- verificationSetup: metadata parse, Architect reread, source search for retired lifecycle bridge references, focused tests, and `git diff --check`.
- commitCreated: no; the pre-implementation checkpoint is commit `8c816bb` and is backed by immutable branch `backup-20260829-175647`.
- commitHash: 8c816bb

## Files Changed By This Run

| File | State | Reason | Verification |
|---|---|---|---|
| lib/meshingress-tool-api/.../McpCallContext.java and services/* | modified/added | Carry a restricted resolver and public provider discovery descriptors without placing live objects in response results. | Focused registry/invocation tests passed. |
| lib/meshingress-tool-api/.../services/McpServiceLifecycle.java, registry, annotation scanner, and invocation handler | added/modified/deleted | Replace the opaque lifecycle input bridge with concrete typed service `init(...)` methods, bound from requirement metadata using normal tool-method rules. | Scanner, registry, lifecycle injection, and snapshot focused tests passed. |
| lib/meshingress-tool-annotations/.../McpTool.java, McpService.java, McpServiceRequirement.java | modified/added | Separate callable-tool and provider configuration after retiring `@McpNode`. | Scanner tests passed. |
| lib/meshingress-tool-framework/.../McpToolAnnotationScanner.java and app/.../mcp/services/McpServiceRegistry.java | modified/added | Publish requirements, resolve selected typed providers, and discover provider metadata. | Scanner and registry tests passed. |
| toolspace/nextcloud/** and distribution/meshingress-tool-distribution/pom.xml | added/modified | Move and attach the concrete Nextcloud provider, its OCS adapter, viewer API/state, properties, and SDK client dependency. | Focused module tests passed. |
| toolspace/local-storage/** and pom.xml | added/moved/modified | Own local provider behavior, configuration DTO, HTTP API, WebDAV handoff, scheduling, resources, and tests. | Focused module suite passed. |
| lib/meshingress-config/.../MeshingressProperties.java | modified | Remove the core provider configuration component/type; retain only a deprecated source-compatibility constructor that ignores the retired argument. | Server MVC and configuration binding tests passed. |
| app/.../storage/** and app/application.properties | modified/deleted | Leave only provider-neutral publication-status and remove all native provider behavior/values. | Local auto-configuration test passed; app output has a known stale compiled class. |
| architect/active/2026-08-29-storage-provider-nodes-and-nextcloud-sdk-extraction and architect/{ASSIGNMENT.md,HANDOFF.md} | modified | Record completed module configuration ownership and the remaining deferred policy work. | Metadata parse and final reread pending. |

## Existing Dirty Work Preserved

| Area | State | Handling | Reason |
|---|---|---|---|
| target/agents/mockito-core.jar | staged | preserved | Pre-existing generated artifact. |
| toolspace/x-open-ink-library/target/agents/mockito-core.jar | staged | preserved | Pre-existing generated artifact. |
| toolspace/yt-dlp/vendor/python/yt_dlp/**/__pycache__/*.pyc | staged | preserved | Pre-existing vendor-generated artifacts. |
| Existing source edits and Architect lifecycle changes in this objective | dirty | active work | Do not discard or overwrite; finish the active slice in place on development. |

## Unfinished Files

| File | State | Remaining Work | Safe Next Action |
|---|---|---|---|
| lib/meshingress-tool-api/src/test/java/dev/mrk/meshingress/api/result/DispatchExecutionResultTests.java | needs-review | Existing wire-shape expectation fails the full baseline. | Create a new architect entry before diagnosing or changing it. |

## Design Decisions Recorded

- Every real storage provider, including local storage, moves to an attachable module; native cache storage stays temporary infrastructure only.
- `ToolStorageService` and `StorageFileApi` remain provider-neutral capability contracts, not node registrations.
- A concrete `ToolStorageService` implementation is a provider node. It composes with consumers in-process through typed capability access, not an MCP call.
- `@McpNode` is retired. `@McpTool` owns callable-node configuration; `@McpService` owns provider-node configuration and service-parameter selection.
- Provider-specific configuration, credentials, constructor inputs, and scopes belong to the provider module. They must not remain in provider-specific `meshingress.storage.*` application properties.
- The Nextcloud provider may depend on `nextcloud-sdk-client:1.0` inside `toolspace/nextcloud`; it must not depend on that SDK's independent tool API/runtime/catalog or broad distribution artifact. Its custom delegated-workspace OCS adapter remains provider-private.
- Local workspace storage is a provider module. `ToolStorageRouter`, native lifecycle selection, app property values, and the legacy core binding DTO are removed from core ownership.
- Injectable capability contracts implement `McpServiceLifecycle`. Service input is a named, typed function-schema property, published as `requirements[requirementKey]`, and crosses into providers only as an already-bound `McpServiceInvocation`; lifecycle cleanup runs after the tool call.

## Blockers

- none; the user authorized the direct implementation path.

## Risks

- Risk: Provider lifetime, scopes, and consumer blacklists are not enforced by the descriptive discovery and resolver layer.
  - Mitigation: Keep scope/blacklist policy explicitly deferred and do not treat `services/list` metadata as an authorization grant.
- Risk: Passing provider identity through response results could conflict with caching, replay, and instance lifetime.
  - Mitigation: Keep live typed resolution separate from `DispatchExecutionResult` until those semantics are designed.
- Risk: The sibling Nextcloud SDK has an independent runtime/API model and documentation/version drift.
  - Mitigation: The approved boundary admits only `nextcloud-sdk-client:1.0` as a module-local dependency and retains custom delegated OCS behavior in a narrow provider-private adapter.
- Risk: The full test baseline remains red at an unmodified API test.
  - Mitigation: Preserve the earlier terminal record; do not attribute that failure to this planning work.

## Verification Details

- commandsRun: focused local-storage/Nextcloud module tests, `McpAnnotationDispatchMvcTests`, `MeshingressApplicationDocumentationSnapshotTests`, and source search for core storage type/property removal.
- result: local module tests and its auto-configuration bootstrap passed. MVC and documentation snapshot tests passed after the core configuration binder was explicitly directed to its canonical constructor.
- knownGap: The full reactor baseline remains red only at the pre-existing `DispatchExecutionResultTests.rawStructuredContentStillPreservesTheCallerProvidedWireShape` assertion; it is outside this objective.
- preservedBaseline: The earlier full-suite API assertion failure remains outside this record.

## Repository State

- gitStatusSummary: Active source and Architect changes are uncommitted on development; the immutable pre-implementation backup is available locally and on GitHub.
- branch: development
- safeDirectoryRequired: no
- commitCreated: pre-implementation checkpoint only
- commitHash: 8c816bb

## Next Action

Await the user-approved provider/consumer scope and blacklist model before implementing authorization semantics. Do not add provider configuration back into core or recreate a global storage lifecycle selector.

## Resume Commands

```powershell
git status --short
.\mvnw.cmd -q -B -pl toolspace/local-storage,toolspace/nextcloud -am '-Dtest=LocalStorageAutoConfigurationTests,StorageLifecyclePolicyTests,StorageServiceTests,AsyncExternalHandoffWorkerTests,WebDavHandoffPublisherTests,WorkspaceMetadataStoreTests,WebDavStorageFileApiTests' '-Dsurefire.failIfNoSpecifiedTests=false' test
.\mvnw.cmd -q -B -pl app/meshingress-server -am '-Dtest=McpAnnotationDispatchMvcTests,MeshingressApplicationDocumentationSnapshotTests' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

## Handoff Integrity Checklist

- [x] Active implementation entry and root handoff point to the same objective.
- [x] Nextcloud/local extraction and module configuration ownership are recorded.
- [x] Existing generated artifacts remain preserved.
- [x] The remaining action is explicitly deferred pending the authorization model.
