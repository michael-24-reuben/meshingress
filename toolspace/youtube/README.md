# YouTube Tool Foundation

This module currently exposes two honest, local MCP functions:

- `youtube.catalog.capabilities` lists stable future function IDs and their delivery status.
- `youtube.catalog.providers` records the intended provider and authorization boundary for each capability family.

It deliberately does **not** make YouTube network calls, accept API keys, start OAuth, upload or delete media, retrieve transcripts, scrape YouTube, or duplicate `media.ytdlp` downloads. The full future catalog is returned at runtime so clients do not need to infer an unavailable feature from a tool-list entry.

## Provider boundaries

| Provider | Future responsibility |
|---|---|
| `official-data-api` | YouTube Data API v3 reads and authorized creator/live/comment operations. |
| `official-analytics-api` | Targeted analytics queries. |
| `official-reporting-api` | Bulk report jobs and downloads. |
| `dedicated-transcript-provider` | Transcript access only after a policy-compliant provider is selected. |
| `ai-orchestrator` | Summaries and chapter proposals from already acquired transcripts. |
| `media.ytdlp` | Existing local constrained media inspection/download companion. |

`YoutubeProviderClient` is the adapter seam. Adding an adapter must also add typed request/response contracts, credential storage and OAuth decisions, scope mapping, quota/error handling, and per-function tests.

## Credential manifest

`YoutubeManifest` declares blank metadata properties for an API-key reference, OAuth client ID, OAuth client-secret reference, and OAuth redirect URI. It never contains the credential values.

The current manifest feature describes a `secret-ref`; it does not yet resolve, encrypt, inject, refresh, or revoke credentials. Live official API work still needs a credential-store adapter and a per-account OAuth authorization-code lifecycle before it can use these values.
