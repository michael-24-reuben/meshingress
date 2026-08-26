# Fixes

## Files Changed

- `pom.xml`
- `cli/nextcloud-mcp-cli/pom.xml`
- `cli/nextcloud-mcp-cli/src/main/java/org/mcp/nextcloud/cli/NextcloudMcpCli.java`
- `cli/nextcloud-mcp-cli/src/main/java/org/mcp/nextcloud/cli/NextcloudMcpCliApplication.java`
- `cli/nextcloud-mcp-cli/src/test/java/org/mcp/nextcloud/cli/NextcloudMcpCliApplicationTest.java`

## Behavior Added

- Added `nextcloud-mcp-cli` as a Maven reactor module.
- Added local CLI commands:
  - `tools list`
  - `call <tool> --arg key=value`
  - `accounts test [accountId]`
  - `config check`
- Added global options:
  - `--config <path>`
  - `--account <id>`
  - `--json`
  - `--arg key=value`
  - `--args-json <json-object>`
- Added JSON output for automation and compact human output for descriptor/config/account commands.
- Added account-scope-based policy context for tool calls.
- Added current-user probing before tool calls so file tools use the real Nextcloud user id rather than the internal config account id.
