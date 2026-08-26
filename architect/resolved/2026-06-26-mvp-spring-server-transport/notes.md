# Notes

## 2026-06-27 Activation

- Activated from `pending/` for implementation.
- Server transport must mirror CLI behavior: descriptor listing is local-only, while account tests and tool calls resolve the current OCS user id before WebDAV-backed file tools run.
- Admin controllers and WebSocket transport remain out of scope for this slice.

## 2026-06-27 Implementation

- Added Spring configuration properties for `nextcloud.mcp.config-path`, `nextcloud.mcp.default-account-id`, and `nextcloud.mcp.validate-on-startup`.
- Added server runtime composition around `nextcloud-mcp-config`, `nextcloud-mcp-client`, `nextcloud-mcp-tool-runtime`, and the MVP files/share/user tool modules.
- Added `/health`, `/api/v1/accounts`, `/api/v1/accounts/test`, `/api/v1/tools`, `/api/v1/tools/call`, `/mcp/tools`, `/mcp/tools/call`, and `/mcp` JSON-RPC mappings.
- Kept admin controllers and WebSocket transport out of scope.
- Focused server verification passed. Full live-enabled reactor failed only because the existing live SDK smoke test received HTTP 502 from the Nextcloud host; the full non-live reactor passed with live smoke flags disabled in-process.
