# Active Notes

## 2026-08-29 implementation start

The user authorized implementation after a clean checkpoint was committed and pushed. This entry itself is now the active source-level implementation record.

The first source slice introduced a resolver carried by `McpCallContext`, a Spring provider registry, and separate annotations. It deliberately leaves cache storage native, exposes no provider instances through `DispatchExecutionResult`, and preserves the source-compatible five-component `McpCallContext` constructor for direct callers.

The last known successful package run was before the annotation-direction revision: `mvnw.cmd -B -pl app/meshingress-server -am -DskipTests package` passed at 2026-08-29T18:06:27-04:00. The later package attempt reached server test compilation but did not yield a terminal result, so it is not verification for the final annotation shape.

## 2026-08-30 annotation direction revision

The user retired `@McpNode`. It has been removed rather than retained as a deprecated parallel API.

- `@McpTool` owns tool configuration and node-wide `requires` declarations.
- `@McpService` owns provider configuration (`provides`, `requires`, visibility, and metadata) and parameter-level service selection (`type`, `oneOf`).
- `McpServiceRegistry` discovers provider classes directly through `@McpService`.

This preserves a direct mapping from annotation to runtime role and removes the artificial Java union shape. The next code slice is method-parameter injection and resolver selection; service listing, authorization, and provider extraction remain unimplemented.

## 2026-08-30 direct injection and selection

- `@McpService` method parameters are now infrastructure parameters: scanning omits them from MCP input schemas and JSON argument metadata, while invocation resolves them from `McpCallContext.services()`.
- `McpServiceResolver` now offers `findAll`, and the registry supports multiple providers for one contract. An unqualified ambiguous request fails; `oneOf` selects a compatible concrete implementation.
- Focused verification passed:
  - `McpToolAnnotationScannerTests`
  - `McpServiceRegistryTests`
  - `AnnotatedMcpToolHandlerServiceInjectionTests`
- The legacy `McpCacheManagerTests` suite still has six pre-existing structured-content JSON-path failures. The new injection test was deliberately isolated rather than folded into that unrelated failing class.

## 2026-08-30 provider discovery

- Added the non-callable `services/list` MCP dispatch family. It publishes public enabled provider descriptors with identity, contracts, requirements, version, and visibility only; no instance, constructor input, or credential is serialized.
- Callable tools now publish class-level and method-level service requirements under their descriptor annotations, so an invoker can see declared capability needs before a call.
- Verified `services/list` route registration through the MVC dispatch test and provider metadata through `McpServiceRegistryTests`.
- Provider/consumer scope and blacklist enforcement remains explicitly deferred; this listing is intentionally descriptive rather than an authorization grant.

## 2026-08-30 Nextcloud SDK adapter and migration map

Read-only inspection established the module boundary before any storage code moves.

- `toolspace/nextcloud` is still empty and is not yet a dependency of `distribution/meshingress-tool-distribution`; it is the destination module for the concrete Nextcloud provider.
- The verified SDK coordinate is `io.github.uriakleahcim.nextcloud:nextcloud-sdk-client:1.0`, available in the local Maven repository. Its HTTP factory supports arbitrary authenticated OCS paths, while its high-level client does not model Meshingress' custom delegated-workspace application route.
- `NextcloudDelegatedWorkspaceClient` therefore moves as a narrow, provider-private adapter. The module may use the SDK HTTP layer/client for standard Nextcloud functionality, but it must not add `nextcloud-sdk-tool-api`, `nextcloud-sdk-tool-runtime`, `nextcloud-sdk-tool-catalog`, or the broad SDK distribution dependency; Meshingress remains the only tool/provider runtime in this process.
- Nextcloud-specific configuration, credential reference, viewer-capability database state, viewer endpoint, and service implementation move together. The app keeps no compile dependency on the provider implementation.
- Local workspace storage has a separate destination module, `toolspace/local-storage`, which does not exist yet. The app's cache remains native temporary infrastructure.
- The native `StoragePublicationMcpController` currently assumes one constructor-injected `ToolStorageService`. It must be redesigned around an explicit provider identity/resolver lookup or relocated with provider-specific status behavior; the old `ToolStorageRouter` and `meshingress.storage.lifecycle` must not survive as a hidden selection mechanism.

The map deliberately does not implement scope/blacklist enforcement. It is ownership and dependency planning only.

## 2026-08-30 Nextcloud provider extraction

- `toolspace/nextcloud` is now a distributed module with `nextcloud-sdk-client:1.0` as its sole Nextcloud SDK dependency. It does not import the SDK tool API, runtime, catalog, or broad distribution artifact.
- The module owns `NextcloudDelegatedStorageService` as `@McpService(provides = ToolStorageService.class)`, its narrow custom delegated-workspace OCS adapter, delegated viewer capability persistence/API, property binding, and credential-reference resolution. Its properties are module resources under `meshingress.toolspace.nextcloud.storage.*`, not application-owned `meshingress.storage.*` fields.
- The application no longer owns Nextcloud implementation classes, delegated viewer routes, target condition, `ToolStorageRouter`, `delegated-target`, or `Provider.NEXTCLOUD`. The distribution attaches the module; the application has no implementation dependency on it.
- `ToonverseTool` now receives `ToolStorageService` through its `@McpService` method parameter rather than capturing an application-wired storage bean in its constructor.
- The stable `storage/publication-status` dispatch no longer selects a single native storage service. It checks temporary native local storage first and then registered providers through `McpServiceResolver`; this preserves the public RPC without restoring a global router. The local-first bridge must be removed when local storage becomes a provider module.
- Focused verification passed at 2026-08-30T02:43:17-04:00: `StoragePublicationMcpControllerTests` (4 tests) and `McpAnnotationDispatchMvcTests` (3 tests). The MVC test calls `services/list` and confirms `nextcloud.storage.delegated-workspaces` is published. Earlier module-focused tests for the Nextcloud adapter, viewer controller, and auto-configuration also passed.
- A first standalone module property-binding attempt showed that a dependency JAR's generic `application.properties` was not bound early enough. The module now loads its specifically named `nextcloud-storage.properties` resource before binding, which keeps provider configuration module-owned and avoids a server configuration import.

## 2026-08-30 Local-storage provider extraction

- Created and distributed `toolspace/local-storage`. It owns the local workspace provider, workspace metadata/files/retrieval/cleanup, local file API, optional WebDAV handoff implementation, HTTP download route, route advice, and scheduling/startup cleanup.
- `ToolWorkspaceStorageService` is now the concrete `@McpService(provides = ToolStorageService.class)` provider. The native server source retains only provider-neutral `storage/publication-status`, which queries all registered providers through `McpServiceResolver`.
- The application no longer contains `meshingress.storage.*` values or native storage implementation classes. Local module defaults and overrides use `meshingress.toolspace.local-storage.*`; the old storage-reference Markdown is marked historical.
- Focused module verification passed: the moved local storage, retrieval, controller, metadata, asynchronous handoff, WebDAV, and file API tests (18 tests total), including `LocalStorageAutoConfigurationTests` proving the module resource creates the annotated provider.
- The attempted MVC app rerun found a stale pre-move `MeshingressStorageConfiguration.class` under the existing server `target/classes`, not a duplicate source definition. It is deliberately left untouched because the target directory contains preserved generated artifacts. Fresh builds do not contain that class; the module auto-configuration test covers source-level bootstrap.
- The temporary remaining ownership debt is `MeshingressProperties.Storage`: it is no longer populated from app properties and is used only as the local module's binding shape. Move that DTO into the module before resolving this record.

## 2026-08-30 tools and services documentation snapshot

- `MeshingressApplication#documentationSnapshotRefresher` now writes `docs/meshingress-tools-and-services.json` together with the dispatch and OpenAPI snapshots.
- The generated document has the public discovery shapes: callable function metadata under `tools` (including optional `moduleToolId`) and non-callable provider metadata under `services`. It serializes descriptors only; it never contains provider instances, constructor inputs, or credentials.
- The output is controlled by `meshingress.documentation.refresh.tools-and-services-output`, defaulting to `docs/meshingress-tools-and-services.json`.
- Focused serialization verification passed in `MeshingressApplicationDocumentationSnapshotTests`. A running development application also refreshed the checked-in file, which currently contains 18 public functions and one public service.

## 2026-08-30 service annotation separation

- `@McpService` is now class-only and declares a provider-node implementation. It no longer carries parameter-injection members.
- `@McpFunctionService(type = ..., oneOf = ...)` now marks a callable tool-function parameter for internal typed provider resolution. It is never a JSON input field.
- Scanner validation, generated requirement metadata, invocation binding, and the Toonverse storage consumer now use the function-specific annotation. Focused scanner, injection, and Toonverse tests passed.

## 2026-08-30 function-service display metadata

- `@McpFunctionService` now accepts optional `value`, `title`, and `description` in addition to its resolver fields, `type` and `oneOf`.
- The scanner exports nonblank display metadata in the function-level `annotations.requires` item, so it is available through `tools/list` and the checked-in tools-and-services snapshot. The fields do not alter provider resolution.
- Toonverse declares `toonverse.book-storage` with a title and description. Focused scanner, injection, and Toonverse tests passed; the generated snapshot contains all three fields.

## 2026-08-30 inferred function-service contract

- `McpFunctionService.type()` now defaults to `Void.class` as a sentinel. When it is not explicitly declared, scanning and invocation derive the capability contract from the annotated function parameter's declared Java type.
- Explicit `type` remains available when a caller needs it, and validation still rejects an explicit contract that differs from the parameter type. `oneOf` is checked and resolved against the effective contract in either form.
- Scanner metadata and runtime resolver injection are both covered by focused tests using the omitted-type form. Toonverse also now uses the concise inferred form.

## 2026-08-30 service lifecycle and client input bridge

- Every injectable capability contract now participates in `McpServiceLifecycle`; `ToolStorageService` extends it, and provider discovery rejects a declared contract that does not.
- The initial function-level input annotation direction was corrected before completion. `@McpFunctionService` only declares the dependency; it does not own provider configuration or alter the callable tool's `inputSchema`.
- A provider declares its initialization type through `McpServiceLifecycle.initInputType()`. `McpServiceRegistry` compiles that type with the normal tool-input schema rules, and `tools/list` publishes it at `requirements[requirementKey].inputSchema`, using the consumer's stable `@McpFunctionService.value()` as `requirementKey`.
- Clients submit provider values separately under `tools/call.params.requirements[requirementKey]`. Invocation binds that value through the existing typed JSON argument binder, calls `init(McpServiceInvocation)` before the tool function, and calls `stop(McpServiceInvocation)` in reverse resolution order afterward. `McpServiceInvocation.arguments(SomeType.class)` gives providers the already-bound argument object.
- `tools/list` now publishes the matching requirement metadata as a key-addressed object: `requirements[requirementKey]` is the descriptor. The `key` is therefore not duplicated inside the descriptor, and a function cannot publish two requirements under the same key.
- The feature does not yet migrate a concrete Nextcloud/local configuration object into a tool input. That remains separate from the pending ownership cleanup of `MeshingressProperties.Storage`.
- Focused verification passed: scanner (11), registry (4), lifecycle injection (1), documentation snapshot (1), and Toonverse (9) tests. The key-addressed descriptor update was reverified by the documentation snapshot, registry, and lifecycle injection tests.

## 2026-08-31 local-provider configuration ownership

- Moved the complete former `MeshingressProperties.Storage` binding shape to module-owned `LocalStorageProperties`. `toolspace/local-storage` now binds `meshingress.toolspace.local-storage.*` directly and no longer depends on `meshingress-config`.
- Kept the established default root through the module resource with the standalone fallback `${meshingress.cache.location:.cache/meshingress}/storage`; normal application cache-location overrides still apply.
- Removed the core record component and nested `Storage` type. A deprecated, ignored compatibility constructor remains temporarily so callers that still pass the former final argument can migrate without reviving provider configuration in core.
- Replaced the unused Nextcloud WebDAV helper dependency on the former shared target shape with a provider-private `WebDavTarget` value object.
- Focused local-storage and Nextcloud tests passed, including module auto-configuration, lifecycle, workspace, handoff, metadata, and WebDAV helper coverage. The server MVC and documentation snapshot focused tests also passed after confirming the core configuration binder selects the canonical `MeshingressProperties` constructor.

## 2026-08-31 typed service-init method binding

- Replaced `initInputType()` plus `McpServiceInvocation` with the normal-function model. `McpServiceLifecycle` now supplies optional no-argument `init()` and `stop()` lifecycle hooks.
- A concrete provider may declare exactly one parameterized `void init(...)` overload. The scanner applies normal tool-method rules: `@McpFunctionParam` fields form the requirement schema, while `McpCallContext`, progress, and `@McpFunctionService` parameters remain injected internally.
- The provider's typed initializer is invoked from the corresponding `tools/call.params.requirements[requirementKey]` object before the tool function. Its `stop()` hook still runs in reverse initialization order, including tool failure paths. Providers with no typed overload receive the no-op/default `init()` and publish an empty schema.
- Removed the obsolete `McpServiceInvocation` bridge. Focused scanner, provider-registry, lifecycle-injection, and documentation snapshot tests passed; the affected reactor also compiled with tests skipped.
