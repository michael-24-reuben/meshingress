# Plan

1. Define a sealed, behavior-bearing artifact classification with one explicit installation target: `TOOL_MODULE`.
2. Replace the controller's `GENERATED_TOOL_MODULE` default with `TOOL_MODULE`; add a separate source/provenance field for generated modules.
3. Reject non-installable classifications at upload/publication/install boundaries until their own lifecycle handlers exist. Do not silently accept labels the runtime cannot honor.
4. Preserve SBOMs, attestations, schemas, and scope review data as coordinate-bound evidence/metadata rather than first-class installable artifacts; remove `SECURITY_POLICY`, `TEMPLATE`, `RUNTIME_PLUGIN`, `SBOM`, and `ATTESTATION` from the artifact enum after persisted-data migration is verified.
5. Specify the CLI harness recipe contract: source executable/package, platform/architecture, command mapping, argument schema, working-directory and filesystem limits, environment/secret references, required scopes, health check, exit/result mapping, and deterministic generated-module provenance.
6. Build the CLI harness in a sandboxed generation phase; require the generated output to pass the normal tool-module assessment, human scope review, publication-signing, and installation flow.
7. Design availability extension and policy contracts after the tool-framework discovery/classloader boundaries are documented. Keep availability distinct from authorization.
8. Replace the mutable four-list scope snapshot with immutable declared/detected evidence plus revisioned reviewer decisions. Support a justified manual addition of a known scope and a disable action; never delete detected evidence.
9. Require every scope-policy amendment to produce a new signed publication and reload installed modules before the new decision becomes effective.
10. Reconcile scope policy with the pending MCP authorization architecture: artifact approval governs what the code may do; caller policy governs who may call it; host configuration governs which capabilities are enabled.
11. Add focused tests proving direct installation is restricted to tool modules, provenance is preserved, unsupported types fail clearly, manually added scopes are auditable, and disabling a detected scope does not delete it.
