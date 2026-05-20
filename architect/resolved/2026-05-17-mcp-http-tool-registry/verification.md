# Verification

## Automated

```powershell
.\mvnw.cmd -q test
```

Result: passed.

Covered behavior:

- Application context starts.
- `initialize` returns MCP tool capabilities and server info.
- `tools/list` exposes the static `architect.entries.list` tool.
- `tools/call` invokes `architect.entries.list` and returns structured entries.
- Invalid JSON-RPC envelopes return protocol errors.
- Admin registry methods reject non-admin callers.
- Admin callers can check, register, update, and disable a dynamic tool alias.
- `GET /mcp` returns `405 Method Not Allowed`.
- `DELETE /mcp` returns `202 Accepted`.

## Notes

- Maven initially failed under the sandbox when writing Maven cache metadata, then failed with `release version 25 not supported`; the project now targets Java 22 to match the local JDK.
- Persistence, SSE notifications, production auth scopes, rate limits, and output sanitization remain follow-up work.
