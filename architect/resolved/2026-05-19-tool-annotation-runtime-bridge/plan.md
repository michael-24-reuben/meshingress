# Plan

1. Keep `McpToolHandler` as the compatibility runtime contract.
2. Relax annotation scanner validation so a common single args object parameter can be inferred without `@McpFunctionParam("args")`.
3. Add a server-side adapter that wraps an annotated Spring bean and implements `McpToolHandler`.
4. Discover beans annotated with `@McpTool`, create adapter handlers, and include them in the registry alongside legacy handlers.
5. Verify `tools/list` and `tools/call` through server MVC tests using the annotated helloworld module.
