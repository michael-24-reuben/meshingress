# Context

## Discussion source

The filtered conversation is preserved at `temp/codex-session-01a04b49-ceb4-7211-b3b4-532489dd1c6c.md`. The user also named `codex-session-raw-01a04b49-ceb4-7211-b3b4-532489dd1c6c.md`; it was not present at that repository-relative path when this record was created.

Earlier storage-transfer discussion established the same direction: provider-specific configuration and credentials belong to provider modules, while the server retains only provider-neutral storage contracts.

## Current source snapshot

- `toolspace/nextcloud` is now a distributed provider module. It owns `NextcloudDelegatedStorageService`, its private delegated-workspace OCS adapter, delegated viewer state/API, module properties, and credential-reference resolution.
- The nine manuals under `toolspace/nextcloud/src/main/resources/docs` remain mirrored module resources. The sibling SDK remains an independent multi-module API/runtime and is not a Meshingress `McpToolHandler` module.
- `ToolStorageService` and `StorageFileApi` remain provider-neutral contracts in `lib/meshingress-tool-api`.
- `NextcloudDelegatedStorageService`, `ToolStorageRouter`, the delegated viewer API, the native Nextcloud target condition, and all Nextcloud-specific application properties have left the server application.
- `toolspace/local-storage` now owns native local workspace storage, optional WebDAV handoff behavior, retrieval/controller routes, cleanup scheduling, and its `meshingress.toolspace.local-storage.*` resource. The server has no native storage implementation or `meshingress.storage.*` application values.
- `LocalStorageProperties` is now module-owned and binds the local provider resource directly. `meshingress-config` retains no local-provider configuration structure or values.

## Agreed direction

1. All real storage providers, including local storage, become attachable modules. Cache storage remains native and is not exposed as a provider node.
2. `ToolStorageService` describes a capability. It is neither a tool node nor a service node by itself.
3. A concrete class such as `NextcloudDelegatedStorageService implements ToolStorageService` is a provider-node implementation. It may expose normal callable MCP functions when useful, but consumers use its typed in-process capability rather than invoking it through MCP.
4. Provider configuration, credentials, provider arguments, and scope declarations move with the provider module. Provider-specific fields must not remain under `meshingress.storage.*`; this includes local-provider settings. `meshingress.storage.lifecycle` is not a durable app-level provider-selection mechanism.
5. Tool nodes and provider nodes are independently discoverable. `tools/list` remains callable-tool discovery; `services/list` now publishes public enabled provider metadata without exposing provider instances. Studio may distinguish the kinds later.
6. A provider module broadcasts its provider-node implementation. Implementing a known capability contract can support compile-time scanning and metadata generation.
7. Provider scope declarations and consumer access requirements must participate in authorization. The eventual model must allow a consumer to receive additional required scopes for a supplied provider object without confusing those requirements with the consumer's own callable functions.

## Decisions still open

### Annotation grammar and terminology

The provisional `@McpNode` umbrella annotation was retired after reviewing its Java annotation shape. The approved split is direct and role-specific:

- `@McpTool` remains the callable-tool annotation and owns tool-level `requires` declarations.
- `@McpService` marks a provider implementation class and owns its `provides`, `requires`, visibility, and provider metadata.
- `@McpFunctionService(type = ..., oneOf = ...)` on a callable method parameter declares the exact typed capability to inject.

This does not turn capability interfaces such as `ToolStorageService` into tools. A concrete class that implements that interface becomes a provider only when it is explicitly annotated and broadcast as an `@McpService`.

### Consumer requirement metadata

Provider discovery is inferred from the concrete annotated class implementing a capability contract. Consumers state only the dependency they receive: class-level `@McpTool(requires = ...)` describes node-wide requirements, while a method parameter-level `@McpFunctionService(type = ..., oneOf = ...)` drives direct injection. The resolver must support multiple independent capability contracts for one tool and reject missing or ambiguous selections before invocation.

### Runtime resolution and lifecycle

`McpCallContext` carries a restricted resolver, not raw provider objects or credentials. The provider instance is resolved only while constructing invocation arguments. `McpServiceLifecycle` supplies optional no-argument `init()`/`stop()` hooks; a concrete provider's one typed `void init(...)` overload is scanned and invoked with the same parameter rules as a tool method, using its requirement object. Do not couple live service references to `DispatchExecutionResult`; cache/replay must remain response-data-only. Provider construction and teardown follow module/Spring ownership until a later explicit lifetime model is approved.

### Authorization model

Define the provider's declared scopes, caller/tool blacklists, consumer-required supplemental scopes, and the enforcement point. Existing scope work is relevant but does not yet settle this model.

### SDK integration boundary

The verified integration boundary is an adapter inside the Nextcloud provider module, not a second runtime inside Meshingress.

- The eligible SDK dependency is `io.github.uriakleahcim.nextcloud:nextcloud-sdk-client:1.0` (and its ordinary SDK dependencies). It provides the standard Nextcloud client surface and, through `NextcloudHttpRequestFactory`, arbitrary authenticated OCS request construction.
- Do not depend on `nextcloud-sdk-tool-api`, `nextcloud-sdk-tool-runtime`, `nextcloud-sdk-tool-catalog`, or the broad SDK distribution artifact. Those modules define a separate tool SPI/runtime and would duplicate Meshingress' node discovery and invocation boundary.
- The existing delegated-workspace protocol is a Meshingress-specific OCS application route, not a standard SDK client operation. Move `NextcloudDelegatedWorkspaceClient` into `toolspace/nextcloud` as a narrow provider-private adapter. It may use the SDK HTTP request factory/adapter, but it must preserve the current OCS envelope handling, payload shapes, and `X-Meshingress-Tool-Id` request header until the SDK deliberately exposes an equivalent extension point.
- `toolspace/nextcloud` owns its own `@AutoConfiguration`, provider configuration binding, credential-reference resolution, provider-private persistence, delegated viewer endpoint/controller, and `@McpService` implementation of `ToolStorageService`. The server must not import a Nextcloud implementation class.
- The SDK manuals mirrored under `toolspace/nextcloud/src/main/resources/docs` remain module resources. The provider POM must pin the documented SDK coordinate/version; it must not use a sibling checkout path, `systemPath`, or a reactor link to the SDK.

The resulting dependency directions are intentionally one-way:

```text
app/meshingress-server -> distribution/meshingress-tool-distribution
distribution            -> toolspace/nextcloud -> meshingress tool API/annotations + nextcloud-sdk-client
distribution            -> toolspace/local-storage -> meshingress tool API/annotations

Forbidden: toolspace/* -> app/meshingress-server
Forbidden: app/meshingress-server -> Nextcloud provider implementation or Nextcloud SDK runtime
Forbidden: toolspace/nextcloud -> nextcloud-sdk-tool-api|tool-runtime|tool-catalog
```

### Native storage removal map

All real provider behavior leaves the application. The next extraction must retain only provider-neutral contracts in the tool API and temporary cache infrastructure in the app.

| Current native ownership | Destination | Required boundary |
| --- | --- | --- |
| `NextcloudDelegatedStorageService`, `NextcloudDelegatedWorkspaceClient`, `DelegatedViewerCapabilityStore`, `DelegatedViewerService`, and delegated-viewer HTTP API | `toolspace/nextcloud` | Module-private Nextcloud configuration, credential reference, database state, and `@McpService` provider implementation. |
| `ToolWorkspaceStorageService`, `WorkspaceFiles`, `WorkspaceMetadataStore`, `WorkspacePathLayout`, `WorkspaceRetrievalService`, cleanup/scheduling, and local file HTTP API | new `toolspace/local-storage` | A separate local provider module; no app-owned local storage implementation or `meshingress.storage.local.*` configuration remains. |
| `ToolStorageRouter`, native `StorageLifecyclePolicy`, `MeshingressStorageConfiguration`, async external handoff selection, and `meshingress.storage.lifecycle` | removed from application | Both providers own their own configuration and behavior. There is no global storage lifecycle/provider selector. `LocalStorageProperties` is owned by `toolspace/local-storage`, not by the app configuration library. |
| Native `StoragePublicationMcpController` | provider-neutral resolver-aware dispatch | Completed: it queries registered `ToolStorageService` providers. It does not retain or recreate `ToolStorageRouter` or a local-first special case. |
| Cache storage | remains native infrastructure | Cache is temporary tool data, not a discoverable storage provider and not part of this migration. |

Provider and consumer scope/blacklist enforcement is still deliberately deferred. This map records ownership and dependency direction only; it does not grant resolver access or expose credentials.
