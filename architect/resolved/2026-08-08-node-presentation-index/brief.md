# Node Presentation Index

Replace string-matched tool icons with a shared Studio presentation index. MCP functions must identify their owning module with an opaque `moduleToolId`; the module catalog supplies the associated icon. Explorer namespace rows and workflow node renderers use resolved descriptors rather than parsing tool names.

Preserve the existing callable `WorkflowNode.toolId` and workflow execution shape. Persisting `moduleToolId` on newly created workflow nodes is a later slice.
