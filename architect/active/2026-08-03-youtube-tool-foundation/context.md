# Context

- The existing local-only `toolspace/x-yt-dlp` module owns constrained media inspection and downloads. The YouTube module must reference that boundary rather than adding a second downloader.
- The official YouTube Data API provides resource reads and authorized write operations. YouTube Analytics supports targeted authorized reports, while YouTube Reporting supports bulk report workflows.
- The official Data API must not be represented as transcript text access. A future transcript implementation requires a separate provider and policy decision.
- The module currently offers only `youtube.capabilities` and `youtube.providers`; all operational function IDs are cataloged as `planned` and are intentionally absent from `tools/list`.
- This is a separate active entry and does not replace the unrelated Nextcloud root assignment.
