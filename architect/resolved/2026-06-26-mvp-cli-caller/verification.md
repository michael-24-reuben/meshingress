# Verification

## Focused

```powershell
.\mvnw.cmd -pl cli/nextcloud-mcp-cli -am test
```

Result: passed.

Covered:

- `config check` reports valid config without printing direct app-password values.
- `tools list --json` exposes files, shares, and user tool descriptors.
- `tools list` does not resolve missing app-password environment secrets.
- `accounts test` probes only current user identity.
- `call nextcloud.files.list` uses OCS user id `temporary` for WebDAV path construction even when config account id differs.
- Tool call is denied when the selected account lacks the required scope.

## Full Reactor

```powershell
.\mvnw.cmd test
```

Result: passed across 19 modules.

The full run also executed the existing live SDK smoke test against the temp account:

- user identity: `temporary`, display name `tempo`
- capabilities version: `34.0.0`
- uploaded `/CodexScratch/nextcloud-mcp-smoke.txt`
- downloaded 72 bytes
- cleaned up with delete status `204`
- admin list probes remained read-only; no OCC execution or app enable/disable occurred.

## Remaining Checks

Run architect metadata parsing and diff whitespace checks after final handoff edits.
