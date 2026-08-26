# qBittorrent Tool

Add a local qBittorrent-backed MCP tool that manages only Meshingress-created torrent jobs and can hand completed, approved-root files into the existing `LOCAL_BYTES + QUEUED` storage publication flow.

The tool must not include torrent-index search, accept arbitrary save paths, expose qBittorrent credentials in calls, or delete downloaded data implicitly.
