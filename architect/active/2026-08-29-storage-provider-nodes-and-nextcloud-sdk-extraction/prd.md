# Product Requirements: Storage Provider Nodes

## Problem

Meshingress currently owns specific storage-provider behavior in the application. That makes provider configuration, credentials, lifecycle behavior, and implementation choices app concerns even though they are optional client-selected capabilities. It also prevents the empty Nextcloud tool module from becoming the natural home for the feature-rich sibling SDK.

## Required outcomes

1. The server exposes stable, provider-neutral storage capability contracts only. `ToolStorageService` and `StorageFileApi` remain contracts rather than provider registrations.
2. A concrete storage implementation can be discovered as a provider node and supplied to another node as its declared interface type.
3. Callable tool nodes and provider nodes have separate discovery surfaces and metadata appropriate to their kind.
4. Consumers receive a provider through typed internal composition. They must not make an MCP request merely to use another module's capability.
5. A provider may additionally expose callable MCP functions when useful; those functions preserve normal `DispatchExecutionResult` behavior.
6. Provider-specific configuration, credentials, local filesystem details, constructor input, and provider-owned argument schemas travel with the provider module.
7. The application retains cache storage solely as native temporary infrastructure for tools. It is neither migratable provider functionality nor a discoverable provider node.
8. Provider selection, eligibility, and scope requirements are visible to discovery/validation before a consuming function is called.
9. The eventual Nextcloud provider module has a documented, dependency-safe relationship with the sibling Nextcloud SDK and its mirrored usage documentation.

## Acceptance criteria for a future implementation

- No provider-specific `meshingress.storage.*` configuration remains after migration; a documented replacement exists in the relevant module.
- Local storage and Nextcloud are both represented as provider modules, while cache storage remains internal.
- Provider and tool listings do not conflate their node kinds.
- A consuming tool receives a compatible `ToolStorageService` implementation through the internal provider-composition mechanism and does not call the provider over MCP.
- Provider selection and authorization failure modes are deterministic and represented in public metadata/errors.
- The new Nextcloud integration is verified against the SDK API and its module documentation without duplicating incompatible runtime ownership.
