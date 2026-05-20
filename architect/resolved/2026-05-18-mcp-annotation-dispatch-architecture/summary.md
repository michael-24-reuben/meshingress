# Summary

Optional annotation-based MCP dispatch is now implemented as a server-local adapter that plugs into the existing `McpMethodController` SPI. The dispatcher was remodeled to discover controller beans and reject duplicate method ownership at startup. Annotated handlers can now declare MCP method mappings, resolve typed arguments from JSON-RPC params, inject `McpCallContext`, and expose schema metadata without coupling schema classes to Java binding implementation. Verification passed with `.\mvnw.cmd -pl app/meshingress-server -am test`.
