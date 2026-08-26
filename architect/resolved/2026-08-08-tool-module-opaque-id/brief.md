# Tool Module Opaque ID

Replace the public raw module identifier with a stable opaque `toolId` derived from the internal module key. Load the module catalog only after MCP `tools/list` succeeds, then give the Explorer's namespace tree direct access to module summaries for its icon selection.

Do not change workflow-node persistence or start assigning catalog IDs to workflow nodes in this slice.
