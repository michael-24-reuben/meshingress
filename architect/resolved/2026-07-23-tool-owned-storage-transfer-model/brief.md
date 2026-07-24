# Tool-Owned Storage Transfer Model

Refactor tool storage so that the application property describes only Meshingress-managed byte placement, while each tool method selects how its content is transferred.

The current global lifecycle selection combines byte placement, transfer ownership, provider selection, and execution scheduling. That prevents a single server from correctly supporting both a local-byte producer such as qBittorrent and a URL-delegating producer such as Toonverse.

No production implementation is authorized by this record alone.
