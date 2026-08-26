# Fixes

## Files Changed

- `app/nextcloud-mcp-server/pom.xml`
- `app/nextcloud-mcp-server/src/main/resources/application.yaml`
- `app/nextcloud-mcp-server/src/main/java/org/mcp/nextcloud/server/*`
- `app/nextcloud-mcp-server/src/test/java/org/mcp/nextcloud/server/NextcloudMcpRuntimeServiceTest.java`
- `pom.xml`

## Behavior Added

- Added Spring config binding for config path, default account id, and optional startup validation.
- Added default beans for YAML config loading, config validation, secret resolution, ObjectMapper, and JDK HTTP client creation.
- Added runtime service wiring for MVP files, shares, and user tools.
- Added REST endpoints:
  - `GET /health`
  - `GET /api/v1/accounts`
  - `POST /api/v1/accounts/test`
  - `GET /api/v1/tools`
  - `POST /api/v1/tools/call`
  - `GET /mcp/tools`
  - `POST /mcp/tools/call`
- Added minimal JSON-RPC endpoint at `POST /mcp` for `initialize`, `tools/list`, `tools/call`, `accounts/list`, `accounts/test`, and `health`.
- Added tests with a fake HTTP adapter to verify descriptor listing avoids HTTP and tool calls use the OCS-returned user id for WebDAV paths.
