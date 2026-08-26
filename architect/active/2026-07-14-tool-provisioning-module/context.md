# Context

## Existing Meshingress Direction

Meshingress tool modules are Maven modules that expose tools through shared APIs and annotations. Tool dependencies may include source repositories and external runtimes that are not naturally managed by Maven.

The provisioning concern should therefore remain a reusable `lib/` module rather than being implemented inside an individual `toolspace/` module.

## Manifest Integration and Superseded Scope

`architect/resolved/2026-07-02-tool-manifest-external-requirements` completed the declaration
surface: class-based manifests, static `tool-manifest.json`, typed `ToolRequirement`, and canonical
source-repository identities. It intentionally deferred managed checkout, shared reuse, cleanup,
and readiness gating.

This active record owns that deferred behavior. Do not create another Maven module for it:

```text
meshingress-tool-api-manifest       -> declaration
meshingress-tool-runtime-loader     -> trusted manifest to provisioning request adapter
meshingress-tool-provisioning       -> source, environment, verification, ready-resource lifecycle
```

The future default managed source layout is `repository/vendor/<canonical-identity>`, for example
`repository/vendor/github.com/SYSTRAN/faster-whisper`. The runtime loader, rather than a normal
MCP tool call, must request provisioning and gate activation on its result.

## Related Code and Reuse Boundaries

| Area | Existing responsibility | Provisioning decision |
|---|---|---|
| `lib/meshingress-tool-api-manifest` | Owns `McpToolManifestDefinition`, `ToolRequirement`, canonical source identity, and static `tool-manifest.json` import/export. | Reuse as the declaration source. Do not duplicate manifest records or URL canonicalization in a tool module. |
| `app/meshingress-server/.../RuntimeToolCache` and `RepositoryArtifactFetcher` | Copies/downloads trusted published tool artifacts and `application.yaml`, `README.md`, and `tool-manifest.json` into `<cached-jar-parent>/resources/`. | Reuse this established manifest transport. Do not extend it into a public-Git clone client or add a second manifest cache. |
| `lib/meshingress-tool-runtime-loader` | Resolves artifacts, creates a classloader and child Spring context, then registers handlers in `DefaultToolRuntimeLoader#activate`. `ResolvedToolArtifact` currently exposes the JAR but not its resource directory. | Add the manifest-to-provisioning adapter and readiness gate here, before classloader/context creation and handler registration. Derive the trusted manifest resource from the resolved JAR's parent or add one explicit resource-directory field; do not fetch or execute tool code for metadata. |
| `lib/meshingress-artifact-storage` | Owns the configured repository layout and safe filesystem path handling. | Extend `RepositoryLayout` narrowly with a vendor root if needed, so `repository/vendor/<canonical-identity>` shares the configured root without duplicating path validation. |
| `app/.../MeshingressRepositoryProperties` | Owns `meshingress.repository.root`. | Reuse this configuration as the single repository-root source; do not add a competing vendor-root property for the first slice. |
| Artifact quarantine and security scanners | Quarantine is for untrusted archive inspection; `ScannerProcessRunner` is an artifact-security-specific process utility. | Keep provisioning workspaces separate from quarantine. Extract a generic bounded-process abstraction only if both domains need it; do not couple the provisioner to artifact-security APIs. |
| `toolspace/powershell-cli` | Pilot manifest that declares an executable and scopes. | Use as the declaration-only control case; it should not acquire a Git source. |
| `toolspace/x-faster-whisper` | Local vendored source simulation, module-local venv, and Java/Python bridge. | Use as the first source-repository fixture after publication. Preserve its vendor checkout as read-only and never provision during `transcribe`. |

No existing Git source checkout/provisioner was found in the inspected Meshingress modules. That
functionality belongs in the active provisioning module behind a narrow source-preparation boundary;
it must not be recreated in the server, runtime cache fetcher, or individual toolspace modules.

## Chosen Name

```text
lib/meshingress-tool-provisioning
```

The term **provisioning** was selected because the responsibility includes more than installation:

```text
resolve
install
inspect
verify
record
reuse or invalidate
```

Names such as `installer`, `dependency-manager`, or `runtime-loader` were rejected because they understate the verification lifecycle, imply replacement of existing package managers, or overlap with runtime loading.

## Initial Use Case

A Meshingress tool declares a dependency that points to a Python GitHub repository. The system must:

1. obtain the repository at a resolved revision;
2. detect its supported Python dependency metadata;
3. use the appropriate external resolver workflow;
4. install the project in an isolated environment;
5. inspect likely undeclared dependencies;
6. apply only explicitly authorized repair inputs;
7. verify that the environment can support the intended tool use;
8. make the resulting runtime reusable by the tool;
9. keep installation behavior out of normal tool calls.

## Existing Software Strategy

The architecture should compose existing open-source software rather than create a new package manager.

### `uv`

Preferred first backend for Python environment creation, lockfile synchronization, requirements installation, and project installation.

### FawltyDeps

Preferred advisory checker for imports that appear to be undeclared by the Python project.

### Railpack

Potential later backend when the desired provisioned resource is an OCI image. It is not required for the initial local virtual-environment path.

### Repo2Run

Useful as a reference for aggressive automated repository repair, but not recommended as the deterministic core because it introduces Docker/agent-oriented behavior and broader mutation authority.

## Important Distinctions

### Compiling versus provisioning

The requirement is not to compile a Python virtual environment. The requirement is to create and verify an isolated environment from repository metadata.

### Declared versus undeclared dependencies

Package resolvers handle declared direct and transitive dependencies. They cannot reliably infer every dependency omitted by the repository. Static analysis and smoke verification provide evidence, but repair remains policy-controlled.

### Import name versus distribution name

Python import names may not equal package distribution names:

```text
PIL      -> Pillow
cv2      -> opencv-python or opencv-python-headless
yaml     -> PyYAML
sklearn  -> scikit-learn
bs4      -> beautifulsoup4
```

This mismatch is the primary reason arbitrary `pip install <missing-import>` behavior is prohibited.

### Python packages versus native requirements

A missing Python distribution is different from a missing executable, DLL/shared library, compiler, Java runtime, browser binary, CUDA runtime, or operating-system package. Future providers may handle those requirement families, but Python package installation must not pretend to satisfy them.

## Recommended Terminology

```text
ToolRequirement
ToolProvisioner
ProvisioningRequest
ProvisioningResult
ProvisioningStatus
ProvisioningEvidence
RequirementProvisioner or ProvisioningProvider
ProvisionedResource
ProvisioningIdentity
VerificationResult
```

Final names should follow existing repository conventions after code inspection.

## Suggested Result Semantics

A result should clearly distinguish:

```text
READY
FAILED
REPAIR_REQUIRED
UNSUPPORTED
REUSED
```

`REUSED` may be represented as metadata on `READY` rather than a separate terminal state.

## Security Context

Provisioning can require high-risk capabilities:

- outbound network access;
- process execution;
- filesystem writes;
- Git repository access;
- plugin/package installation;
- possible use of credentials for private repositories.

The module should expose these needs to the integrating policy layer. It should not bypass Meshingress scope, approval, audit, or secret-resolution rules.

Credentials must be supplied through existing secret mechanisms and omitted from normalized repository identity, logs, and provisioning evidence.

## Files and Areas Likely to Change

Exact paths must be confirmed from the repository, but expected areas include:

```text
pom.xml
lib/meshingress-tool-provisioning/**
existing requirement model module
existing runtime loader integration
app/meshingress-server/pom.xml or runtime assembly module
architect/ASSIGNMENT.md
architect/HANDOFF.md
```

The implementing agent should avoid unrelated changes to MCP transport, tool result serialization, annotation schemas, availability policies, and scope definitions unless a direct compile-time dependency requires a narrow adjustment.
