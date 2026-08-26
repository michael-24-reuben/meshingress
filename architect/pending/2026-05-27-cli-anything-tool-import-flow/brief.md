# CLI Anything Tool Import Flow

## Problem Statement

Meshingress should be able to import useful external repositories written in arbitrary languages by using `cli-anything` as a CLI harness generator.

The target outcome is that an agent can point Meshingress tooling at a foreign repository, clone it at a pinned commit, use `cli-anything` to generate a usable command-line wrapper, wrap the generated CLI and upstream executable inside a Java Meshingress tool module, package the module as a JAR, and submit the artifact to `meshingress-repository` for validation and publication.

## Proposed Import Flow

```txt
foreign repo
  → agent clones repo at pinned commit
  → cli-anything generates usable CLI wrapper
  → Meshingress wraps generated CLI + upstream executable in Java tool module
  → module is packaged as JAR
  → artifact is uploaded to meshingress-repository
  → repository quarantines, scans, reviews, assigns trust status
  → approved artifact receives checksum/signature/publication metadata
  → official MCP runtime installs only approved publication records
```

## Key System Split

```txt
meshingress-repository = import, assessment, trust, publication
meshingress-server     = verify, install, enforce scopes, expose MCP tools
```

The repository owns import-time risk assessment and publication. The server owns runtime verification, installation, scope enforcement, and MCP exposure.

## Scope

This architect covers the future `cli-anything` generated-tool import flow and its security model.

It does not implement repository artifact storage, artifact indexing, publication records, checksums, signatures, or client download behavior directly. Those belong to the prerequisite architect:

```txt
2026-05-27-meshingress-repository-artifact-implementation
```

## Current Status

Blocked. Implementation of this architect must not begin until the todos for `2026-05-27-meshingress-repository-artifact-implementation` are complete.
