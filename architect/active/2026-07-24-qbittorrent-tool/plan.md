# Plan

1. Add the `toolspace/qbittorrent` Maven module and bundle dependency with module-local auto-configuration.
2. Implement an authenticated qBittorrent WebUI client, durable Meshingress job store, and approved-root collection validation.
3. Expose comprehensive job-scoped MCP operations: connectivity, add, list, status, files, transfer controls, collection, and safe cancellation/removal.
4. Add focused unit and MVC discovery tests; use a controlled authorized torrent only if a local qBittorrent service is configured and running.
