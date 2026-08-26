# Fixes

- Kept callable `WorkflowNode.toolId` unchanged and introduced optional `moduleToolId` as independent module identity.
- Preserved registered manifest definitions by handler source type; annotated adapters report their underlying tool class.
- Registered classpath functions with `cp-...` catalog IDs and runtime functions with `rt-...` catalog IDs, then emitted `moduleToolId` in MCP `tools/list`.
- Added `createToolPresentationIndex`, which precomputes function-to-module and module-to-icon maps once per catalog refresh.
- Replaced `ToolNodeIcon` with `WorkflowNodeIcon`, a renderer that accepts only resolved built-in or image descriptors.
- Wired the shared index through Explorer, canvas, inspector, and variables drawer. A namespace backed by anything other than exactly one module deliberately renders `FolderIcon`.
