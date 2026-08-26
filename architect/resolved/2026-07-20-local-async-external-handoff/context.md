# Context and Constraints

The existing storage design stages tool output locally. The synchronous external lifecycle performs final handoff in `ToolWorkspaceStorageService.publish`, so the tool request stays open while the external publisher performs its work. Current failure handling is appropriate for a synchronous terminal result but is unsuitable for a durable asynchronous transfer: queueable and failed handoffs need their local files and metadata retained instead of being immediately cleaned up.

The related resolved record `2026-07-20-storage-lifecycle-and-write-only-handoff` establishes the foreign-target boundary: a direct WebDAV handoff is create-only and write-only. The target is not an external source of truth for reads, listings, mutation, deletion, or cleanup. Generated request/session/file directories must be created before file upload; the configured base path remains an operator-owned prerequisite and must not be created by the application.

`EXTERNAL_EXTERNAL` is intentionally separate. It requires a provider-managed upload/session capability and must not be repurposed for direct WebDAV publication.

The active MCP progress-runtime architect record remains independent. Async handoff may report progress through that surface, but it must not lengthen or evade the fixed tool request deadline. This pending record must not replace that active assignment.
