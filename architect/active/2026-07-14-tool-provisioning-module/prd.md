# PRD: Tool Dependency Provisioning

## Product Intent

Meshingress tools may depend on software that is not part of the server JVM. The provisioning module prepares those external dependencies in a controlled, reproducible, and inspectable manner before tool invocation.

The initial implementation targets Python projects sourced from Git repositories, but the module contract must remain usable for future executable, native-library, Node.js, Java-runtime, container-image, and other requirement providers.

## Functional Requirements

### FR-1: Dedicated module

Create the Maven module:

```text
lib/meshingress-tool-provisioning
```

Recommended base package:

```text
dev.mrk.meshingress.provisioning
```

Add the module to the root Maven reactor. Attach it only to the application or runtime modules that actually coordinate dependency preparation.

### FR-2: Provisioning contract

Expose a module-level contract that accepts a requirement and provisioning context and returns a structured result.

The contract must represent at least:

- requirement identity;
- source identity and revision when applicable;
- requested destination or managed workspace;
- lifecycle status;
- provisioned runtime/resource location;
- installed dependency evidence;
- verification evidence;
- warnings;
- failure category and diagnostic summary;
- whether the result was newly created or reused.

Names may differ after repository inspection, but terminology should remain centered on `Provisioner`, `ProvisioningRequest`, `ProvisioningResult`, and `ProvisionedResource` or equivalent concepts.

### FR-3: Separate installation from verification

Installation and verification must remain distinct lifecycle phases.

An installation process returning exit code zero is not sufficient evidence that the dependency can support the tool. A resource becomes `READY` only after required verification succeeds.

### FR-4: Provider registry

Support provider selection through an extension point keyed by requirement kind or supported requirement type.

The orchestrator must not contain a growing conditional chain for every language and installer. Providers own requirement-specific behavior while the orchestrator owns shared lifecycle policy.

### FR-5: Isolated workspace

Each provisioned dependency must use a managed workspace derived from stable identity inputs, such as:

- requirement kind;
- normalized source repository URL;
- pinned revision or resolved commit;
- relevant configuration fingerprint;
- platform and architecture;
- runtime version where applicable.

Installations must occur in staging and become reusable only after verification. A failed attempt must not be exposed as a ready environment.

### FR-6: Git repository support

The first source provider must support:

- clone or fetch;
- pinned commit, tag, or branch resolution;
- normalized repository identity;
- optional subdirectory selection;
- reuse of an existing checkout when safe;
- recording the resolved commit;
- avoiding repository mutation outside the managed workspace.

Existing repository/download utilities should be reused if Meshingress already provides them.

### FR-7: Python project detection

The Python provider must inspect recognized dependency metadata without reimplementing dependency resolution.

Candidate formats include:

```text
uv.lock
pyproject.toml
poetry.lock
pdm.lock
Pipfile.lock
requirements.txt
requirements/*.txt
setup.cfg
setup.py
```

Selection rules must be deterministic and recorded in the provisioning result. Lockfiles take precedence when their corresponding workflow is supported.

### FR-8: Python environment installation

The initial preferred resolver is `uv`, invoked through a process abstraction rather than embedded package-resolution logic.

The provider must support these broad paths:

- synchronize a locked `uv` project;
- install a standard `pyproject.toml` project;
- install requirements-file dependencies;
- install the repository itself when it is a package;
- prepare script-only repositories without forcing `pip install .`;
- expose the environment interpreter and relevant executable paths.

Alternative backends may be introduced later behind the provider contract.

### FR-9: Undeclared dependency inspection

The Python provider must support an advisory inspection phase for likely undeclared third-party imports. FawltyDeps is the preferred initial external checker.

Inspection output must distinguish:

- declared dependencies;
- likely undeclared dependencies;
- unresolved import-to-distribution mappings;
- optional or test-only findings where detectable;
- checker warnings and unsupported cases.

Static inspection does not by itself authorize installation.

### FR-10: Controlled dependency repair

Missing or undeclared Python dependencies may be installed only through an explicit repair policy.

Supported policy levels should conceptually include:

```text
DISABLED
DECLARED_ONLY
TRUSTED_MAPPINGS
EXPLICIT_OVERRIDES
APPROVAL_REQUIRED
```

Exact enum names may differ.

The initial implementation should prioritize:

1. dependencies declared by the repository;
2. explicit additional dependencies declared by the Meshingress requirement;
3. explicit import-to-distribution overrides;
4. trusted mappings approved by Meshingress policy;
5. failure or repair-required state.

It must not blindly install a public package whose name matches a missing import.

### FR-11: Runtime smoke verification

A Python repository must support one or more verification strategies, selected from explicit requirement metadata or conservative project inspection.

Examples include:

- import a known package/module;
- execute a declared CLI with `--help`;
- execute a project-provided smoke command;
- inspect an expected executable;
- run dependency consistency checks;
- validate expected files or runtime markers.

Verification commands must be low-side-effect and bounded by timeout.

### FR-12: Failure classification

Provisioning failures must be classified sufficiently for the caller to distinguish:

- unsupported requirement;
- source resolution failure;
- unavailable runtime;
- dependency manifest failure;
- dependency conflict;
- package build failure;
- missing executable or native library;
- undeclared dependency;
- verification failure;
- timeout;
- policy denial;
- corrupted or incomplete cached installation.

Raw command output may be preserved in diagnostic evidence, but returned summaries should remain bounded and safe for normal logs.

### FR-13: Immutable execution boundary

Ordinary tool invocation must not mutate provisioned environments.

When a previously ready environment fails because a dependency is missing or corrupted, the runtime should surface a broken/reprovision-required condition. Repair must return to the provisioning lifecycle.

### FR-14: Reuse and invalidation

A ready installation may be reused only when its provisioning identity still matches. Relevant changes must invalidate it, including:

- resolved source revision;
- dependency metadata or lockfile fingerprint;
- additional dependency overrides;
- repair policy inputs;
- selected Python runtime;
- platform or architecture;
- provider implementation/schema version where compatibility requires it.

### FR-15: Observability

Expose structured phase and result data suitable for logs, audit records, status APIs, or future persistence without binding the module to a particular storage implementation.

Sensitive environment values, credentials, and repository access tokens must not be included in ordinary result metadata.

## Non-Functional Requirements

### Safety

- Provisioning must fail closed when provider selection or repair authorization is ambiguous.
- Network, process execution, filesystem write, and plugin-install privileges must remain explicit at the integration boundary.
- Source revisions should be pinned before an installation is considered reproducible.
- Incomplete staging directories must never be presented as ready runtimes.

### Determinism

- Provider selection and manifest selection must be deterministic.
- Resolved commit and runtime version must be recorded.
- A ready environment should be reproducible from its source and provisioning inputs where upstream artifacts remain available.

### Extensibility

- New providers should not require changes to the orchestration lifecycle.
- External tools such as `uv`, FawltyDeps, Railpack, or future resolvers must remain adapters, not domain models.

### Testability

- Process execution, source checkout, filesystem workspace handling, and runtime detection must be testable through boundaries or controlled fixtures.
- Provider tests should use small local fixtures wherever practical rather than depending on live public repositories.

## Acceptance Criteria

The entry is complete when:

1. `lib/meshingress-tool-provisioning` exists and builds in the Maven reactor.
2. The module defines a provider-neutral provisioning lifecycle contract.
3. Installation and verification are represented as separate phases.
4. A provider registry selects a provider without server-specific conditionals.
5. A Python Git repository fixture can be checked out into a managed workspace.
6. A recognized Python manifest can be selected deterministically.
7. A Python environment can be created and dependencies installed through `uv`.
8. The repository can be installed as a package or treated as a script project as appropriate.
9. FawltyDeps findings can be captured as advisory evidence.
10. Explicit additional dependencies and import-to-distribution overrides can be applied.
11. Arbitrary missing import names are not automatically installed.
12. A smoke verification determines whether the resource reaches `READY`.
13. Failed verification prevents publication of the staging environment as ready.
14. A verified environment can be reused when its provisioning fingerprint matches.
15. Unit and integration tests cover success, undeclared dependency, verification failure, and cache invalidation paths.
