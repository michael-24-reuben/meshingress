# Module Contract

## Purpose

`meshingress-tool-provisioning` converts external tool requirements into verified resources that a runtime loader can safely consume.

## Inputs

The module receives a requirement plus environmental and policy context. The request may contain:

- requirement metadata;
- source repository identity and revision;
- requested runtime version constraints;
- optional subdirectory;
- explicit additional dependencies;
- explicit dependency mappings;
- verification intent;
- provisioning workspace context;
- repair policy;
- cancellation and timeout context.

The module should not require HTTP controller, JSON-RPC, or MCP call objects.

## Outputs

A successful result exposes a provisioned resource reference. A Python resource may include:

```text
sourceRoot
resolvedRevision
runtimeRoot
pythonExecutable
virtualEnvironmentRoot
entryExecutables
selectedDependencyManifest
resolvedDependencyEvidence
verificationEvidence
provisioningFingerprint
```

These are conceptual fields, not a required Java record definition.

## Provider Responsibilities

A provider must be able to answer:

1. Does it support this requirement?
2. What stable identity inputs affect reuse?
3. How is the source/resource prepared?
4. How is installation performed?
5. What advisory analysis is available?
6. How is readiness verified?
7. What evidence and failure category should be returned?

## Orchestrator Responsibilities

The orchestrator owns:

- request validation;
- provider selection;
- lifecycle sequencing;
- staging and ready-state rules;
- reuse/invalidation;
- shared timeout/cancellation propagation;
- result normalization;
- failure cleanup;
- readiness publication.

It does not own language-specific dependency commands.

## Python Provider Responsibilities

The initial Python provider owns:

- Python project recognition;
- dependency metadata selection;
- Python runtime selection inputs;
- `uv` command planning/invocation;
- package versus script project handling;
- FawltyDeps advisory analysis;
- explicit dependency repair inputs;
- Python-specific smoke verification;
- Python environment resource metadata.

## Integration Contract

The runtime integration should follow this sequence:

```text
Tool discovered
  -> requirements evaluated
  -> provisioning requested
  -> required resources READY
  -> tool activated/registered as callable
```

A failed required provision keeps the tool unavailable and exposes a diagnostic state. Optional requirements may be represented later, but should not weaken required-resource semantics.

## Mutation Rule

```text
Provisioning lifecycle: mutation allowed under policy.
Tool invocation lifecycle: provisioned environment is read-only by convention and must not self-repair.
```

## External Tool Boundary

External tools are adapters:

```text
uv
fawltydeps
Git
Railpack (future)
```

Their command syntax, output formats, and installation locations must not leak into the neutral provisioning API.

## Versioning

Provisioning metadata should include a schema or provider version when needed to invalidate old ready resources after incompatible provider behavior changes.
