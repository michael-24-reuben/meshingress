# Assessment

## Root Cause
- Tool availability annotations lived outside the MCP tool annotation flow, creating ownership confusion with route annotations.
- Invocation-time availability needed explicit enforcement to deny calls when policies return unavailable.
- Optional @McpConfigureMapping required a safe default for availability evaluation.

## Resolution
- Tool availability evaluation is centered in tool annotation scanning and runtime invocation.
- Invocation enforcement throws JSON-RPC TOOL_UNAVAILABLE when availability policies deny execution.
- Missing @McpConfigureMapping now defaults to McpAvailabilityMode.ALL during invocation evaluation.

## Affected Areas
- Tool annotation scanning (availability validation and policy evaluation).
- Tool invocation handler (availability enforcement and default mapping behavior).

