# Current Evidence and Model Decisions

## Verified Current State

- `MeshingressArtifactType` declares twelve values, but no repository, installer, or runtime code selects behavior by one of them.
- The upload endpoint accepts any enum value and defaults to `GENERATED_TOOL_MODULE`; `ArtifactRecord` and `ArtifactPublicationRecord` instead default a missing type to `TOOL_MODULE`.
- `ArtifactInstaller` always caches the publication as a JAR and activates it through `ToolRuntimeLoader`. It has no safe path for a raw executable, a policy document, or an analysis file.
- SBOM generation, scope inference, review, publication eligibility, signatures, and attestations are already lifecycle evidence attached to a tool artifact, not independently installable workloads. The existing SBOM scanner and publication signature evidence remain required; only their representation as artifact types is being removed.

## Existing Availability Capability

- Tool methods can declare custom availability annotations via `@McpToolAvailabilityCondition` and `AvailabilityCondition` implementations.
- `@McpConfigureMapping(availabilityMode = ALL | ANY)` combines those discovery-time checks.
- This is code-level tool metadata. A future distributable annotation-provider artifact must be explicitly designed for classloading, compatibility, and discovery before it can be accepted by the repository.

## Existing Scope Capability

- `@McpToolScopes` declares a tool/function's requested scopes.
- Assessment records requested and inferred scopes; review records approved and denied scopes; the signed publication embeds that declaration.
- `InstallPolicyEvaluator` rejects disabled approved scopes and requires every installed function scope to be approved. `DefaultToolExecutor` applies the server's runtime scope switches at invocation.
- Per-principal authorization and a production policy engine are separately pending in `2026-07-16-mcp-oidc-principal-and-header-migration` and `2026-07-16-mcp-tool-authorization-and-credential-strategies`.

## Proposed Artifact Taxonomy

| Existing enum value | Status | Intended authority |
|---|---|---|
| `TOOL_MODULE` | active | Directly installable JAR that exposes Meshingress tools. |
| `GENERATED_TOOL_MODULE` | retire as a type | Keep generation provenance/recipe on a tool module record instead. |
| `CLI_HARNESS` | deferred | A build pipeline input/recipe; its output is a reviewed `TOOL_MODULE`. |
| `AVAILABILITY_ANNOTATION` | design later | Java extension package that supplies discovery-time conditions; never a raw runtime tool. |
| `AVAILABILITY_POLICY` | design later | Host/operator configuration governing discovery or enablement; not yet implemented. |
| `SCOPE_POLICY` | retain the concept, not a second artifact authority | Scope declarations are part of the signed tool publication; server and future principal policies remain explicit policy data. |
| `JSON_SCHEMA` | embedded metadata | Function schemas belong in tool descriptors/manifests. |

`SECURITY_POLICY`, `TEMPLATE`, and `RUNTIME_PLUGIN` are intentionally out of scope and must not be retained merely as speculative enum values. `SBOM` and `ATTESTATION` remain generated evidence, but are not artifact categories.

## Scope Decision Contract

The artifact's observations and permissions must be separate:

| Layer | Mutability | Meaning |
|---|---|---|
| Declared scopes | versioned source claim | What the tool/module says it needs. |
| Detected scopes | immutable assessment evidence | What static analysis observed for that artifact revision. A later scan may add a new assessment result; it must not erase earlier evidence. |
| Scope decisions | revisioned reviewer policy | For every known scope, a reviewer can enable or disable it, with an origin (`detected` or `manual`), actor, reason, and timestamp. |
| Effective scopes | derived | The enabled decisions in the current signed publication, intersected with host capability switches and future caller authorization. |

- A reviewer may manually add a known `McpToolScope` that was not detected, but it must be labeled `manual`, justified, and included in the next signed publication.
- A reviewer disables a scope rather than deleting it. Detection remains visible, and the decision history records every enable/disable transition.
- An amendment after approval creates a new scope-policy revision, invalidates the prior publication for new installation, and requires re-signing. A currently installed module must be reloaded against the new signed publication before the effective scope set changes.
- Unknown scope names are rejected by default; a compatibility escape hatch, if retained, must be explicit and never silently grant an unknown scope.
