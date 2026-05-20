# Summary

Annotated MCP tool beans are now bridged into the existing server runtime. The scanner infers a single args object parameter, the server wraps `@McpTool` beans in `McpToolHandler` adapters, and `InMemoryToolRegistry` indexes those adapters alongside legacy handlers. The existing helloworld annotation-shaped tool now works through `tools/list` and `tools/call`, while legacy `McpToolHandler` tools remain supported. Focused Maven verification passed with Java 22 compiler overrides.
