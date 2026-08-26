# Tool Annotation Runtime Bridge

Continue the annotation-driven MCP tool-author API work by making annotated tool beans executable through the existing `tools/list` and `tools/call` path.

Current state:

- `lib/meshingress-tool-annotations` can scan annotated tool classes and produce metadata.
- `toolspace/helloworld` is annotation-shaped and no longer implements `McpToolHandler`.
- Server runtime still discovers and executes only `McpToolHandler` beans through `InMemoryToolRegistry` and `DefaultToolExecutor`.

Expected behavior:

- Annotated tool beans such as `HelloWorldTool` should appear in `tools/list`.
- `tools/call` should invoke the annotated default function and bind request arguments into the function parameter.
- `McpToolHandler` should remain available temporarily for legacy tools.
