# Summary

Tool availability annotations now live in the MCP tool annotation surface and are enforced at invocation time. The scanner validates and evaluates availability metadata, while the invocation handler denies calls with TOOL_UNAVAILABLE when policies disallow execution. Optional @McpConfigureMapping defaults to McpAvailabilityMode.ALL during invocation evaluation to avoid null handling issues. Tests were run to confirm the change.
