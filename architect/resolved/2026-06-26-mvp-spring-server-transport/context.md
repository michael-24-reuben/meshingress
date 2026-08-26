# Context

## Parent

- `architect/pending/2026-06-26-nextcloud-mcp-core-architecture`

## Intended Module

- `app/nextcloud-mcp-server`

## MVP Endpoints

```text
GET  /health
GET  /api/v1/tools
POST /api/v1/tools/call
GET  /api/v1/accounts
POST /api/v1/accounts/test
POST /mcp
GET  /mcp/tools
POST /mcp/tools/call
```

## Verification Baseline

```powershell
.\mvnw.cmd -pl app/nextcloud-mcp-server test
```
