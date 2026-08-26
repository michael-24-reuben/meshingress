# Context

`tools/list` must use the same call permission as `tools/call`; direct calls always re-check immediately before execution. Authorization bearer tokens prove access to Meshingress, not automatic authority to downstream services. No caller-token passthrough is implicit.
# Adopted prerequisite

`architect/resolved/2026-08-15-mcp-call-context-security-refactor` now owns
common list/call eligibility and provides `McpPrincipal`. This entry should
extend that decision path with verified grants and outbound credential
selection; it must not use raw incoming authorization evidence as a tool API.
