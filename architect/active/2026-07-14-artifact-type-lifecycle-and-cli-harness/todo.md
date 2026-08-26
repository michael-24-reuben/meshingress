# Todo

- [x] Make scope policy the active assignment; retain authentication and authorization as the last pending todo.
- [x] Normalize current uploads, documentation, and test publications to `TOOL_MODULE`.
- [x] Verify the local database, remove unused artifact enum values, and add the idempotent generated-module migration with publication re-signing.
- [x] Execute and verify the startup migration against the local repository database.
- [ ] Introduce explicit artifact lifecycle capabilities and an installability check.
- [ ] Add API validation and error responses for unavailable/deferred classifications.
- [ ] Define the CLI harness recipe and generated tool-module manifest schemas.
- [ ] Select the raw-program intake formats and Windows/Linux platform support.
- [ ] Define process containment, allowed paths, network policy, secret injection, and audit behavior for harness-generated tools.
- [ ] Design availability annotation provider packaging and availability policy ownership.
- [ ] Define immutable declared/detected scope evidence plus revisioned `enabled`/`disabled` reviewer decisions.
- [ ] Add a justified manual-scope addition flow that accepts only known `McpToolScope` values.
- [ ] Require re-publication, signature verification, and runtime reload after every scope-decision amendment.
- [ ] Reconcile scope declaration, host enablement, and caller authorization with the authentication architecture.
- [ ] Add repository, publication, installer, and end-to-end MCP tests for each implemented lifecycle.
