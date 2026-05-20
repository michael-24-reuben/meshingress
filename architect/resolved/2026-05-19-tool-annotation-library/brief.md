# Tool Annotation Library

Create a new `lib/` Maven module that lets tool authors describe MCP tools with annotations, similar in spirit to the existing controller dispatcher annotations.

The initial scope is metadata and scanning only. Do not connect the new module to `ToolRegistry`, `ToolExecutor`, `ToolsMcpController`, or runtime execution wiring in this change.

The sample shape lives under `toolspace/helloworld/src/main/resources/temp`.
