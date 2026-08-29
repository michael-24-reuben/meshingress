# Storage Provider Nodes and Nextcloud SDK Extraction

## Objective

Define the architecture for moving every non-cache storage provider out of the Meshingress application and into attachable provider modules, beginning with the existing Nextcloud SDK and the empty `toolspace/nextcloud` module.

## Scope

- Establish provider nodes as a first-class MCP node kind, distinct from callable tool nodes.
- Keep `ToolStorageService` and `StorageFileApi` as provider-neutral capability contracts rather than registered nodes.
- Treat a concrete implementation such as `NextcloudDelegatedStorageService` as a service-provider node that implements `ToolStorageService`.
- Define in-process, typed provider composition for consuming tools; provider access is not an MCP-to-MCP call.
- Move provider-specific configuration, credentials, constructor arguments, and provider scopes into the provider module. This applies equally to local storage.
- Keep cache storage native infrastructure only: it supports temporary tool data and is not a provider node.
- Plan the integration/extraction boundary for `C:\Users\jbeas\Repositories\Dev.java-2026\artifacts\nextcloud-sdk`, whose usage manuals are already mirrored under `toolspace/nextcloud/src/main/resources/docs`.

## Non-goals

- Implement annotations, discovery, service construction, endpoint changes, or module dependencies.
- Move or delete current native storage code or `meshingress.storage.*` properties.
- Publish the Nextcloud SDK or change its versioning, documentation, or Maven coordinates.
- Design a final authorization policy for provider resolver access in this record alone.

## Completion condition

The unresolved design decisions in `context.md` are made, the API and lifecycle changes are represented by an approved implementation plan, and this entry is activated before any source implementation begins.
