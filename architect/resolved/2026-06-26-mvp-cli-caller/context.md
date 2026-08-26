# Context

## Parent

- `architect/pending/2026-06-26-nextcloud-mcp-core-architecture`

## Intended Module

- `cli/nextcloud-mcp-cli`

## Example Commands

```bash
nextcloud-mcp tools list
nextcloud-mcp call nextcloud.files.list --arg path=/Documents
nextcloud-mcp accounts test main
nextcloud-mcp config check
```

## Verification Baseline

```powershell
.\mvnw.cmd -pl cli/nextcloud-mcp-cli test
```
