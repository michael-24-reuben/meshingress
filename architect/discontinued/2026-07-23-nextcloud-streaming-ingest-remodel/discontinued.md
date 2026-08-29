# Discontinuation Notice

This architect initiative investigated replacing custom Nextcloud background workers with a Meshingress-driven streaming upload pipeline using standard WebDAV (`PUT` and chunked upload v2).

## Reason for Discontinuation

1. **Alternative Path Adopted and Verified**: The parallel proposal `2026-07-23-nextcloud-delegated-worker` was implemented, verified, and confirmed working as intended.
2. **Bandwidth and Runtime Decoupling**: Having Nextcloud's background worker fetch media streams directly from external sources preserves Meshingress memory and local disk without requiring high-throughput proxy streaming.
3. **No Immediate Business Requirement**: Since the delegated worker architecture satisfies all functional requirements, building an additional WebDAV streaming ingestion pipeline is redundant.
