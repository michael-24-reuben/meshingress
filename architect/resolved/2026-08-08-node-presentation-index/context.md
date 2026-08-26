# Context

`WorkflowNode.toolId` is currently the callable tool segment used to form `<toolId>.<functionName>`, not the opaque module catalog identity. It cannot be replaced without changing execution semantics.

The server can identify a classpath function through the manifest registered for its handler class and a runtime function through its runtime owner. The shared UI index can therefore map callable function name to `moduleToolId` once, then resolve module presentation in constant time. A namespace with functions from multiple modules intentionally falls back to the folder icon rather than arbitrarily choosing one extension icon.
