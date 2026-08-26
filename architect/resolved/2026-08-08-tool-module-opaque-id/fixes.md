# Implementation

- Replaced public response `id` fields with `toolId`, including detail, README, and icon paths.
- Added server-side opaque-ID lookup while retaining raw module keys as internal catalog state only.
- Made Studio request the module catalog after a successful `tools/list`; a catalog failure leaves registered MCP tools usable.
- Passed module summaries directly to `ExplorerPanel`, where `ToolTreeNamespace` derives its icon from catalog data rather than a copied function property.
- Left `WorkflowNode` and workflow persistence unchanged for the owner-directed follow-up.
- Resolved catalog `href` and `icon.href` values against `RuntimeConfiguration.current.apiBaseUrl` before Studio rendering, so a separately hosted Studio requests the SVG from Meshingress rather than its own Vite origin.
