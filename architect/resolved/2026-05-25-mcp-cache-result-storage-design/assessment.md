# Assessment

`@McpCacheResult` was present as a declarative annotation but had no runtime enforcement. Annotated MCP tools were invoked directly through `AnnotatedMcpToolHandler`, so repeated calls always executed the method even when a cache policy was declared.

The correct interception point is the annotated tool handler because it has the method, tool descriptor, function descriptor, arguments, call context, and adapted `DispatchExecutionResult`. Keeping cache behavior there avoids putting storage logic into tool modules or the annotation library.

The MVP backend is local filesystem storage, with no-op and memory stores included so storage selection is abstracted from the annotation contract. Cache entries are addressed by SHA-256 keys built from canonical JSON key material and stored under the configured cache directory.
