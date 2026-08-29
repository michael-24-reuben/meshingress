# Assessment

Delegating external source downloads to the destination storage environment successfully decouples high-bandwidth media retrieval from Meshingress runtime memory and disk.

Key architectural assessments:
- **Separation of Bytes vs Descriptors**: Native workspace metadata (`book.json`, `chapter.json`) is uploaded directly as native bytes while the workspace is `OPEN`, while heavy asset URLs (e.g. comic pages) are delegated to Nextcloud.
- **Single Seal Boundary**: An explicit single seal guarantees that the destination worker only starts processing once all native files and source URLs are staged.
- **Evolution of Retry and Transfer Model**: Rather than managing complex async retry queues inside Meshingress for bytes it does not own, the system was refactored in `2026-07-23-tool-owned-storage-transfer-model` (introducing `DELEGATED_SOURCE_URLS`) and `2026-07-23-nextcloud-delegated-worker` (where Nextcloud's database tracks per-source retry counts and job leases).
