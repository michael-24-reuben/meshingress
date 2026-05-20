# Notes

- The live helloworld tool currently has `call(HelloWorldGreetArgs arguments, McpCallContext context)` with no `@McpFunctionParam`, so scanner validation needs single args object inference before direct adapter scanning works.
- Existing legacy tools such as `ArchitectEntriesListTool` and `InstaFetchTool` still implement `McpToolHandler`; registry wiring must preserve those.
