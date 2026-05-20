# Summary

The `/mcp` MCP HTTP tool registry objective is resolved for the MVP. The service now exposes a JSON-RPC 2.0 MCP endpoint with initialization, ping, tool listing, tool calling, admin-only dynamic tool check/register/update/disable methods, in-memory registry versioning, mutation audit events, and a real static `architect.entries.list` tool over the local `architect/` folders. Verification passed with `.\mvnw.cmd -q test`; remaining work is the planned persistent registry, SSE notifications, and production-grade auth/rate-limit/output policies.
