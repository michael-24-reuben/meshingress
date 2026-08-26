# MCP Call Context Security and Eligibility Refactor

Refactor the public MCP call context and its HTTP, WebSocket, dispatcher,
workflow, listing, and tool-execution paths so that tool handlers receive
verified, credential-safe caller context rather than raw transport headers.

The implementation must create one shared eligibility decision for `tools/list`
and `tools/call`, preserve disabled-tool visibility for authorized
administrators without making such tools executable, and distinguish static
availability metadata from call-time availability decisions.

This entry prepares the shared boundary for the existing OIDC-principal and
tool-authorization records. It does not itself select an OIDC provider, wire
Spring Security, or enable Aegis in Meshingress.
