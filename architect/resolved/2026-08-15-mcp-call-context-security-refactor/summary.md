# Summary

Resolved the MCP call-context security refactor. Tool handlers now receive a
typed, credential-safe principal and execution context; raw authentication
evidence and role hints are server-only. Caller-controlled role-header
privilege was retired, and listing/execution share one eligibility decision.

The result is an integration-ready boundary for the still-pending OIDC
principal migration and per-tool authorization/credential strategy work.
