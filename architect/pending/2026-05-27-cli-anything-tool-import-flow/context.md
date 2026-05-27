# Context

## External Reference

`cli-anything` repository:

```txt
https://github.com/HKUDS/CLI-Anything
This architect assumes cli-anything is used as the external CLI harness generator in the import pipeline. The exact commit or release used during an import must be recorded in generated-tool provenance metadata.
```

## Discussion Summary

The discussion started from evaluating `cli-anything`, which appears useful because it can let agents generate usable CLI wrappers for arbitrary programs. This suggests a Meshingress import path where external repositories in any language can be converted into MCP-accessible tools without hand-writing a native Java integration for each upstream project.

The generated CLI wrapper should not be exposed directly to the MCP runtime. Instead, it should be wrapped in a Java Meshingress tool module, packaged as a JAR, uploaded to `meshingress-repository`, quarantined, scanned, reviewed, and only then published for installation.

## Backtrack Point

This architect follows from the `cli-anything` discussion where generated wrappers are treated as a way to convert external repositories into Meshingress tools, then hardened by a separate repository/security-review stage before MCP exposure.

## Intended Flow

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

## Repository/Server Boundary

```txt
meshingress-repository = import, assessment, trust, publication
meshingress-server     = verify, install, enforce scopes, expose MCP tools
```

This boundary is intentional. The server should not become the place where arbitrary repos are cloned, generated, compiled, scanned, and trusted. The repository should own that responsibility and publish only approved artifacts.

## Relationship to Existing Tool Module System

Generated imports should eventually produce normal Meshingress tool modules. They should use the same tool annotation and SPI model as manually authored modules, including tool IDs, function declarations, scopes, configuration, and structured results.

The generated Java module should be thin. It should primarily:

- validate arguments
- invoke the generated CLI safely
- parse structured output
- return `DispatchExecutionResult`
- declare required scopes
- avoid hiding broad privileges behind a clean tool name

## Relationship to Repository Artifact Implementation

This architect depends on:

```txt
2026-05-27-meshingress-repository-artifact-implementation
```

That prior architect is expected to establish the repository artifact model and the client/server download bridge. Until that exists, this `cli-anything` import flow has no trustworthy place to upload, quarantine, validate, approve, publish, download, or verify generated tool artifacts.
