# Plan: Create `meshingress-tool-provisioning`

## Architectural Position

The provisioning module sits between requirement metadata and runtime/tool activation.

```text
Static tool manifest / requirements
          |
          v
runtime-loader manifest-to-provisioning adapter
          |
          v
meshingress-tool-provisioning
  - provider selection
  - source preparation
  - installation lifecycle
  - verification lifecycle
  - evidence and readiness state
          |
          v
Provisioned resource reference
          |
          v
Runtime loader / tool activation
```

The runtime loader consumes verified resource references. It should not become the installation engine.

## Required Approval Boundary

External-source resolution and Python dependency installation are assessment-time candidate work,
not first-activation work. For each immutable source requirement, assessment must create an
isolated candidate workspace and export review evidence before an approver can publish it:

```text
upload -> static JAR assessment -> pinned source checkout -> candidate venv install
       -> dependency/verification evidence -> user approval -> publication
       -> activation promotes or reuses the approved candidate -> tool registration
```

The candidate must remain separate from the live runtime workspace until approval. Its evidence
must identify the artifact coordinate and digest, canonical source identity and resolved commit,
selected project metadata/lockfile, interpreter/platform fingerprint, installed distributions,
and verification output. Activation must reject a missing, mismatched, or unapproved candidate;
it may not silently download or install dependencies as a substitute.

Because package builds can execute arbitrary build hooks, candidate resolution must run with an
explicit isolation policy: no server credentials, constrained filesystem access, bounded network,
timeouts and output limits, and a disposable process/workspace. The existing static-quarantine
scanner remains static; this is a distinct, explicitly visible assessment stage.

`meshingress-tool-api-manifest` remains the owner of tool-authored declaration data such as
`ToolRequirement.sourceRepository(...)` and canonical repository identity. The runtime loader
is the integration boundary: it translates a trusted static manifest requirement into a neutral
provisioning request and withholds activation until required resources are ready. The provisioning
module must not depend on MCP transport, tool annotations, or artifact-manifest JSON.

## Proposed Module Boundary

```text
lib/meshingress-tool-provisioning/
├─ pom.xml
└─ src/
   ├─ main/java/dev/mrk/meshingress/provisioning/
   │  ├─ core lifecycle contracts
   │  ├─ provider extension contracts
   │  ├─ workspace and identity model
   │  ├─ source-repository support
   │  └─ python provider/adapters
   └─ test/
      ├─ local Git fixtures
      ├─ Python project fixtures
      └─ lifecycle integration tests
```

This is a conceptual package layout. The implementing agent should inspect current package conventions and avoid creating layers that duplicate existing abstractions.

## Core Concepts

### Provisioning orchestrator

Coordinates shared lifecycle behavior:

```text
validate request
 -> calculate identity
 -> acquire workspace lock
 -> inspect reusable installation
 -> stage source/resource
 -> delegate installation
 -> delegate verification
 -> finalize ready installation
 -> return structured result
```

### Requirement provider

Owns behavior specific to one requirement family. Initial provider:

```text
Python Git Repository Provider
```

Future providers may cover executable downloads, Node.js repositories, Java artifacts, native libraries, or OCI images.

### Provisioning identity

A stable fingerprint that determines whether an existing ready resource is reusable.

### Provisioned resource

A runtime-neutral reference to the prepared dependency. For a Python repository this may include:

- checked-out source location;
- virtual environment location;
- Python interpreter path;
- executable/script entry locations;
- resolved source revision;
- dependency evidence;
- verification evidence.

### Provisioning result

Represents ready, failed, repair-required, reused, or unsupported outcomes without exposing internal process objects.

## Lifecycle States

Recommended conceptual states:

```text
REQUESTED
INSPECTING
RESOLVING_SOURCE
STAGING
INSTALLING
ANALYZING
VERIFYING
READY
REPAIR_REQUIRED
FAILED
```

State names may be adapted to existing Meshingress conventions. The important boundary is that `INSTALLED` is not equivalent to `READY`.

## Implementation Phases

### Phase 1: Module scaffold and neutral contracts

Create the Maven module, add it to the reactor, and establish provider-neutral contracts.

Deliverables:

- module `pom.xml`;
- base package;
- request/result/status concepts;
- provider capability/selection contract;
- provisioning identity concept;
- structured diagnostic evidence;
- tests for provider selection and lifecycle result invariants.

Do not introduce Python-specific fields into the neutral result model.

### Phase 2: Managed workspace lifecycle

Add controlled staging and ready workspace behavior.

Deliverables:

- deterministic resource identity/fingerprint;
- staging path and finalized ready path;
- incomplete-install cleanup behavior;
- reuse check;
- process-safe or JVM-safe coordination consistent with existing project needs;
- invalidation metadata.

Do not invent repository storage architecture if one already exists. Integrate through the existing boundary.

### Phase 3: Git source preparation

Provide source repository preparation for providers that need a checkout.

Deliverables:

- normalized repository identity;
- revision resolution;
- resolved commit evidence;
- optional subdirectory support;
- checkout reuse rules;
- source preparation failure classification.

For a source repository requirement with canonical identity `github.com/owner/repo`, the managed
source checkout path is:

```text
repository/vendor/github.com/owner/repo
```

The checkout ref and resolved commit participate in the provisioning identity. The source path is
shared only when the canonical identity and compatible revision policy match; a ready Python
environment remains a separate resource derived from that source.

Prefer reuse of existing Git/process abstractions.

### Phase 4: Python project inspection

Introduce the initial Python provider and deterministic manifest selection.

Deliverables:

- Python project recognition;
- supported dependency metadata inventory;
- manifest/lockfile selection evidence;
- package-project versus script-project classification;
- Python runtime compatibility input;
- unsupported/ambiguous project result.

Framework detection may provide verification hints, but frameworks remain ordinary dependencies and should not become separate provider types.

### Phase 5: `uv` installation adapter

Use `uv` as the first installation backend.

Deliverables:

- runtime/tool availability check;
- isolated environment creation;
- locked-project synchronization;
- requirements-file installation;
- package installation when applicable;
- additional explicit package installation;
- bounded command output capture;
- installation evidence.

The adapter invokes `uv`; it does not reproduce `uv` dependency resolution.

### Phase 6: Undeclared dependency analysis

Integrate FawltyDeps as an advisory checker.

Deliverables:

- checker availability detection;
- scan invocation;
- normalized findings;
- import-to-distribution suggestions when available;
- warnings for ambiguous findings;
- policy input for whether any finding may be repaired.

Absence of FawltyDeps should produce an explicit capability result unless policy requires the checker.

### Phase 7: Controlled repair inputs

Allow caller-provided and policy-approved repair metadata.

Deliverables:

- explicit additional distributions;
- explicit import-to-distribution mappings;
- trusted-mapping extension point if approved;
- maximum repair attempts if runtime smoke repair is enabled;
- repair evidence and reason;
- refusal of arbitrary public package-name guesses.

Keep automatic repair conservative in the initial release. A `REPAIR_REQUIRED` result is preferable to unsafe mutation.

### Phase 8: Verification and readiness

Add Python verification strategies and finalization.

Deliverables:

- explicit verification command support;
- import verification support;
- executable existence/`--help` verification where applicable;
- dependency consistency verification;
- timeout and exit-code handling;
- readiness evidence;
- promotion from staging to reusable ready resource only after success.

### Phase 9: Runtime integration

Connect the module to the component that prepares tools before activation.

Deliverables:

- a manifest-to-provisioning adapter in `meshingress-tool-runtime-loader` that maps
  `ToolRequirement` values to neutral provisioning requests;
- explicit dependencies from the runtime loader to the manifest and provisioning modules, without
  making the provisioning module depend on manifest JSON or runtime activation classes;
- tool readiness gating;
- ready resource attachment/reference;
- broken-resource or reprovision-required behavior;
- no provisioning mutation during ordinary tool calls.

The server should depend on the module only through the runtime component that owns lifecycle coordination.

### Phase 10: Documentation and verification fixtures

Document the supported Python formats and extension model.

Fixture matrix:

1. `requirements.txt` script project;
2. standard `pyproject.toml` package;
3. `uv.lock` project;
4. project with explicit additional dependency;
5. project with undeclared import detected by FawltyDeps;
6. project with ambiguous missing import that becomes `REPAIR_REQUIRED`;
7. project whose install succeeds but smoke verification fails;
8. cached ready project reused;
9. source revision change invalidates the ready environment.

## Integration Decisions

### Preferred external tools

- `uv`: environment creation and Python dependency installation.
- FawltyDeps: advisory undeclared dependency analysis.
- Git or existing repository service: source checkout.
- Railpack: deferred optional provider for OCI image output, not required for the first local-runtime implementation.

### Dependency direction

The provisioning module may depend on stable requirement abstractions and shared process/filesystem utilities. Tool APIs, annotation scanning, HTTP transport, and server controllers should not become dependencies unless a narrowly justified existing contract requires them.

Avoid circular dependencies with the runtime loader. If shared requirement models currently live inside a runtime implementation module, move only the neutral abstractions needed to establish a clean dependency direction.

## Risks and Mitigations

### Risk: Module becomes a universal build system

Mitigation: delegate language-specific resolution to external tools and keep providers narrow.

### Risk: Unsafe dependency confusion

Mitigation: never install arbitrary missing import names; require declarations, explicit overrides, trusted mappings, or approval.

### Risk: Dynamic imports evade static checking

Mitigation: treat FawltyDeps as advisory and require runtime smoke verification.

### Risk: Installation succeeds but native runtime is incomplete

Mitigation: classify executable/shared-library verification failures separately from Python package failures.

### Risk: Cache contains partial environment

Mitigation: stage first, verify, then finalize atomically or through an equivalent ready marker.

### Risk: Python environment is not portable

Mitigation: include platform, architecture, and interpreter version in the provisioning identity. Rebuild rather than copy incompatible environments.

### Risk: Runtime loader and provisioner overlap

Mitigation: provisioner produces verified resources; runtime loader loads or exposes them. Document this boundary and test it.
