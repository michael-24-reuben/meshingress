# Notes

- Initial design reserves function IDs in runtime catalog data rather than registering unimplemented MCP functions. This keeps `tools/list` truthful while making planned scope visible to clients and future implementers.
- The catalog uses a single dot in every function name, matching the repository's `tool.function` registry convention; family grouping is encoded with hyphens.
- 2026-08-03: `YoutubeToolTests` and `McpYoutubeToolMvcTests` passed. Server discovery exposed only `youtube.capabilities` and `youtube.providers`, not planned operational functions.
- 2026-08-06: `YoutubeManifest` declares blank `api-key-ref`, OAuth client ID, client-secret reference, and redirect URI. The manifest's `secret-ref` classification is declarative only; a runtime secret resolver plus per-account OAuth authorization-code, token-refresh, revocation, and audit lifecycle remain required before live official API calls.
- 2026-08-12: The production-only ignored file now supplies `meshingress.youtube.api-key` directly for public reads. `youtube.data.search`, `youtube.data.videos-get`, `youtube.data.channels-get`, and `youtube.data.playlists-get` call the official YouTube Data API only after that property is nonblank; without it, they fail closed before networking. OAuth fields remain blank placeholders and are not consumed by this slice.
