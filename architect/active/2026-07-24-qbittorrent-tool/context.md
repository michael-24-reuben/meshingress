# Context

`architect/resolved/2026-07-23-tool-owned-storage-transfer-model` provides the required provider-neutral handoff: torrent acquisition stays local; selected completed files are written through `ToolStorageService` and publication is polled through `storage/publication-status`.

qBittorrent uses its authenticated WebUI API under `/api/v2`. The module owns a fixed qBittorrent category and a unique per-job tag, so it never manages unrelated user torrents.
