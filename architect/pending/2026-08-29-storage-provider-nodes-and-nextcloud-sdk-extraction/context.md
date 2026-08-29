# Context

## Discussion source

The filtered conversation is preserved at `temp/codex-session-01a04b49-ceb4-7211-b3b4-532489dd1c6c.md`. The user also named `codex-session-raw-01a04b49-ceb4-7211-b3b4-532489dd1c6c.md`; it was not present at that repository-relative path when this record was created.

Earlier storage-transfer discussion established the same direction: provider-specific configuration and credentials belong to provider modules, while the server retains only provider-neutral storage contracts.

## Current source snapshot (review only)

- `toolspace/nextcloud` is an empty module: its POM declares only the module identity and its Java source is a stub.
- The nine manuals under `toolspace/nextcloud/src/main/resources/docs` match manuals in the sibling `nextcloud-sdk` checkout.
- The sibling SDK contains its own multi-module Java API/runtime model and is not presently a Meshingress `McpToolHandler` module.
- `ToolStorageService` and `StorageFileApi` currently live in `lib/meshingress-tool-api`.
- `NextcloudDelegatedStorageService`, `MeshingressStorageConfiguration`, and `StorageLifecyclePolicy` currently live in the server application; `meshingress.storage.lifecycle` is therefore native today.

None of the above was changed while creating this record.

## Agreed direction

1. All real storage providers, including local storage, become attachable modules. Cache storage remains native and is not exposed as a provider node.
2. `ToolStorageService` describes a capability. It is neither a tool node nor a service node by itself.
3. A concrete class such as `NextcloudDelegatedStorageService implements ToolStorageService` is a provider-node implementation. It may expose normal callable MCP functions when useful, but consumers use its typed in-process capability rather than invoking it through MCP.
4. Provider configuration, credentials, provider arguments, and scope declarations move with the provider module. Provider-specific fields must not remain under `meshingress.storage.*`; this includes local-provider settings. `meshingress.storage.lifecycle` is not a durable app-level provider-selection mechanism.
5. Tool nodes and provider nodes are independently discoverable. The intended public distinction is `tools/list` for callable tools and a future service/provider listing for provider nodes; Studio may distinguish them later.
6. A provider module broadcasts its provider-node implementation. Implementing a known capability contract can support compile-time scanning and metadata generation.
7. Provider scope declarations and consumer access requirements must participate in authorization. The eventual model must allow a consumer to receive additional required scopes for a supplied provider object without confusing those requirements with the consumer's own callable functions.

## Decisions still open

### Annotation grammar and terminology

Use `@McpNode` as the umbrella concept, replacing the provisional `@McpInstance` name. The exact Java annotation grammar is not decided. A nested discriminated form such as `@McpNode(instance = @McpNode.Tool(...))` expresses the intent but requires validation against Java annotation type constraints. A separate kind-specific member form, such as `@McpNode(tool = ...)` or `@McpNode(service = ...)`, is a candidate—not an approved API.

### Consumer requirement metadata

The user prefers provider inference from `implements ToolStorageService` over repetitive consumer declarations. The architecture still needs enough metadata to determine which compatible provider(s) a tool may receive, including any `oneOf` selection. Resolve whether this is inferred at scan time, declared on the node, expressed by constructor/function parameters, or composed from those mechanisms.

### Runtime resolution and lifecycle

Determine where a consumer obtains a selected provider instance and how construction, caching, ownership, selection, and teardown work. `McpCallContext` is a candidate for a restricted resolver surface, but putting live provider objects or credentials directly in context is deferred. Do not couple live service references to `DispatchExecutionResult` until cache/replay and instance-lifetime semantics are separately defined.

### Authorization model

Define the provider's declared scopes, caller/tool blacklists, consumer-required supplemental scopes, and the enforcement point. Existing scope work is relevant but does not yet settle this model.

### SDK integration boundary

Choose whether the Nextcloud SDK is consumed as-is, adapted behind the provider module, or reshaped into Meshingress-facing submodules. Reconcile its coordinate/version and independent tool-runtime API with Meshingress' provider-node API before adding any dependency.
