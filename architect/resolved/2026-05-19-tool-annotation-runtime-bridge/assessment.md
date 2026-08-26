# Assessment

The annotation metadata module was complete enough to describe tools, but the runtime still indexed only Spring beans implementing `McpToolHandler`. That left annotation-shaped tools such as `toolspace/helloworld` compilable but not executable through `tools/list` or `tools/call`.

The lowest-risk bridge is to keep `McpToolHandler` as the internal compatibility contract and adapt annotated Spring beans into handler instances during registry construction. This keeps existing legacy tools working while allowing tool modules to move to annotation-first authoring.

The scanner also needed one ergonomic rule: a single non-`McpCallContext` function parameter can be inferred as the `args` object instead of requiring `@McpFunctionParam("args")`.
