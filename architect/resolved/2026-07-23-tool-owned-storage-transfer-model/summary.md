# Summary

Resolved on `development`. Storage lifecycle now expresses only local-byte placement, while transfer and local-byte handoff scheduling are tool-requested workspace contracts. Toonverse uses delegated source URLs explicitly; a future qBittorrent tool can use the local-byte path without a configuration switch.

The related Nextcloud records remain active because their separate deployment and streaming-ingest work is not closed by this refactor.

Reopened twice: first to prevent an explicitly blank delegated target from registering the optional viewer surface, then to make the named delegated target bind through the immutable compatibility record. Both cases now have focused regression coverage.
