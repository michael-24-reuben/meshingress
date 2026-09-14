# Storage Provider Nodes and Nextcloud SDK Extraction

## Objective

Implement the approved architecture for moving every non-cache storage provider out of the Meshingress application and into attachable provider modules, beginning with the existing Nextcloud SDK and the empty `toolspace/nextcloud` module.

## Scope

- Establish provider nodes as a first-class MCP node kind, distinct from callable tool nodes. Completed through `@McpService` and `services/list`.
- Keep `ToolStorageService` and `StorageFileApi` as provider-neutral capability contracts rather than registered nodes.
- Treat a concrete implementation such as `NextcloudDelegatedStorageService` as a service-provider node that implements `ToolStorageService`.
- Define in-process, typed provider composition for consuming tools; provider access is not an MCP-to-MCP call.
- Move provider-specific configuration, credentials, constructor arguments, and provider scopes into the provider module. This applies equally to local storage.
- Keep cache storage native infrastructure only: it supports temporary tool data and is not a provider node.
- Extract the Nextcloud provider behind the verified integration boundary for `C:\Users\jbeas\Repositories\Dev.java-2026\artifacts\nextcloud-sdk`, whose usage manuals are already mirrored under `toolspace/nextcloud/src/main/resources/docs`.

## Non-goals

- Publish the Nextcloud SDK or change its versioning, documentation, or Maven coordinates.
- Design a final authorization policy for provider resolver access in this record alone.

## Completion condition

Provider nodes remain independently discoverable; concrete Nextcloud and local providers are module-owned; all provider-specific application storage configuration and the native lifecycle router are removed; cache remains native; and focused verification passes. Scope/blacklist enforcement remains a deliberately deferred follow-up.
