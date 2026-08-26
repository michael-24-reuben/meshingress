# Todo

- [x] Decide the public `McpCallContext` compatibility/versioning strategy.
- [x] Define credential-safe principal, ID, client metadata, lineage, and
  execution-control types in the tool API.
- [x] Define the server-only transport evidence and verified-principal adapter
  interfaces.
- [x] Add one context factory for HTTP, MCP WebSocket, and workflow routes.
- [x] Replace direct raw-header context construction at every ingress.
- [x] Replace caller-controlled role-header authorization with
  principal-based checks and a bounded legacy migration path if needed.
- [x] Add the shared tool eligibility decision/service.
- [x] Apply that decision consistently to `tools/list`, `tools/call`, and
  internal workflow tool calls.
- [ ] Define list-time versus call-time availability policy contracts.
- [x] Carry deadline, cancellation, and progress through execution control.
- [x] Preserve parent identity and correlation in workflow child contexts.
- [ ] Add complete HTTP, batch, workflow, disabled-admin-view, and dynamic
  availability regression coverage (follow-up verification hardening).
- [x] Run focused tool-API/framework/server compile and WebSocket regression tests.
- [x] Update the related OIDC and tool-authorization records with the adopted
  shared-context contract.
