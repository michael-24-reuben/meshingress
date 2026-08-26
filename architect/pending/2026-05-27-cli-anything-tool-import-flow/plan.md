# Plan

## Phase 0: Wait for Repository Artifact Foundation

Implementation is blocked until the prerequisite architect is complete:

```txt
2026-05-27-meshingress-repository-artifact-implementation
```

This dependency must provide the artifact upload, quarantine, checksum/signature, publication, and client download bridge needed by this generated-tool import flow.

## Phase 1: Define Generated Tool Artifact Contract

Define the artifact layout for a `cli-anything` generated tool module.

Candidate layout:

```txt
generated-tool.jar
├─ Java Meshingress tool wrapper classes
├─ META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
├─ foreign-tools/<tool-id>/manifest.json
├─ foreign-tools/<tool-id>/bin/generated-cli
├─ foreign-tools/<tool-id>/bin/upstream-executable-or-launcher
├─ foreign-tools/<tool-id>/schemas/*.json
├─ foreign-tools/<tool-id>/docs/generated-tool-contract.md
└─ foreign-tools/<tool-id>/sbom/*.json
```

## Phase 2: Define Manifest Schema

Create a manifest schema containing:

- tool ID
- source repository URL
- source commit
- generator name/version
- generation agent metadata
- generated CLI entrypoint
- upstream executable or launcher entrypoint
- supported functions/subcommands
- argument schemas
- declared scopes
- file access assumptions
- network access assumptions
- timeout defaults
- test result references
- SBOM references

## Phase 3: Create Wrapper Template

Create a reusable Java wrapper template that:

- extracts embedded executables to a runtime tool cache
- verifies checksums before execution
- validates requested subcommands against manifest allowlists
- invokes generated CLI using `ProcessBuilder`
- avoids shell command concatenation
- forces JSON output where possible
- maps stdout/stderr/exit code into `DispatchExecutionResult`
- supports timeouts and cancellation

## Phase 4: Repository Validation Pipeline

Extend `meshingress-repository` with generated-tool specific checks:

- static artifact structure validation
- manifest schema validation
- declared scope review
- executable inventory
- dependency and SBOM checks
- generated CLI smoke tests
- upstream executable smoke tests
- malware or suspicious behavior scan hooks
- publication record generation only after approval

## Phase 5: Server Installation Path

Extend server-side install behavior to:

- download only approved publication records
- verify checksums/signatures
- extract runtime payloads into a controlled cache
- enforce scopes declared by publication metadata
- expose MCP tools only after validation
- reject revoked or incompatible publications

## Phase 6: First Prototype

Use a low-risk external repository with simple CLI/processing behavior.

Prototype result should demonstrate:

- pinned source import
- `cli-anything` generation
- Java wrapper packaging
- upload to repository
- quarantine and review
- publication
- server install
- MCP invocation
