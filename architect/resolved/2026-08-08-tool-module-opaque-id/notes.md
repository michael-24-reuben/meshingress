# Notes

- 2026-08-08: Scope is limited to catalog ID publication and Explorer icon ownership. Workflow-node adoption is intentionally deferred.
- 2026-08-08: `toolId` is a full SHA-256 Base64URL digest with a source prefix. It is opaque and deterministic; server lookup maps it only to active catalog entries rather than decrypting it.
- 2026-08-08: Live server probe returned the PowerShell SVG with HTTP 200 and `image/svg+xml`. The missing Studio icon is therefore a client-origin bug: the API's relative `/api/v1/.../icon` link was applied against the Studio origin instead of `RuntimeConfiguration.current.apiBaseUrl`.
