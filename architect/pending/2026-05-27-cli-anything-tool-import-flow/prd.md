# PRD: CLI Anything Tool Import Flow

## Goal

Enable Meshingress to convert selected external repositories into installable MCP tools by using `cli-anything` to generate a CLI harness, then wrapping that harness in a Meshingress Java tool module and publishing it only after repository-side validation.

## User Value

A developer should be able to point an agent at an arbitrary useful repository and produce a Meshingress-compatible tool artifact without manually rewriting the upstream project in Java.

The expected developer experience is:

```txt
select repo → pin commit → generate CLI harness → package Java wrapper → submit to repository → review/publish → install from approved publication
```

## Requirements

### R1: Pinned Source Import

The import process must clone external repositories at pinned commits or immutable refs. Mutable branches are not sufficient for publication.

### R2: CLI Harness Generation

The import process should use `cli-anything` as a generation step to produce a usable CLI wrapper around the upstream software.

The generated CLI should prefer machine-readable output, especially `--json`, so Meshingress can return structured tool results.

### R3: Java Tool Module Wrapping

The generated CLI and upstream executable should be wrapped by a Java Meshingress tool module.

The Java module should expose one or more MCP functions and should invoke the generated CLI using process execution without shell string concatenation.

### R4: Artifact Packaging

The Java wrapper, generated CLI, upstream executable or launchers, manifest, schemas, documentation, and verification metadata should be packaged into a JAR artifact suitable for upload to `meshingress-repository`.

### R5: Repository-Side Quarantine

Uploaded generated-tool artifacts must default to a quarantined state. Quarantined artifacts are not installable by official MCP runtimes.

### R6: Repository-Side Validation

The repository should scan and assess:

- source repo metadata
- pinned commit
- generated CLI wrapper
- Java wrapper module
- embedded executables or launchers
- declared scopes
- generated schemas
- SBOM and dependency metadata
- checksum and artifact identity
- test and smoke-test results

### R7: Trust and Publication

Only approved artifacts should receive immutable publication records containing checksum, signature, source provenance, generator metadata, declared scopes, runtime compatibility, and trust status.

### R8: Server-Side Install Gate

`meshingress-server` must install only approved publication records from the repository. The server must verify checksum/signature and reject artifacts that are revoked, unapproved, incompatible, or policy-disallowed.

### R9: Scope Enforcement

The generated module must declare the least privilege needed. Common generated-tool scopes are expected to include process execution and file read/write access, but these must be explicit and reviewable.

### R10: Auditability

The repository and server must preserve enough metadata to reconstruct how a generated tool was produced, reviewed, published, installed, and invoked.

## Non-Goals

- Do not implement a general plugin runtime in this architect.
- Do not bypass repository review for generated tools.
- Do not allow direct runtime installation from arbitrary generated JARs.
- Do not treat generated code as trusted merely because it compiles.
- Do not require every generated tool to expose fully typed functions in the first version; a validated generic `run` function may be acceptable for early prototypes if scoped and reviewed.

## Acceptance Criteria

- A generated-tool artifact can be represented with source provenance, generator metadata, declared scopes, checksums, and publication status.
- The repository can distinguish quarantined, reviewed, approved, published, rejected, revoked, and deprecated artifacts.
- The server can install only approved publication records.
- Generated CLI invocation is mediated by a Java tool wrapper that validates arguments and captures structured output.
- Implementation remains blocked until the repository artifact implementation architect is completed.
