# Brief: Meshingress Tool Provisioning Module

## Request

Create a new reusable Maven library module at:

```text
lib/meshingress-tool-provisioning
```

The module owns the installation and verification of dependencies required by Meshingress tools. The first concrete requirement family is a Python project hosted in a Git repository, where the repository may declare dependencies through `requirements.txt`, `pyproject.toml`, lockfiles, or another supported Python dependency format.

The module must also support identifying dependencies that are imported by the project but omitted from its declared dependency metadata. Such findings must be handled through explicit policy rather than arbitrary package installation.

## Problem

Tool dependency installation currently risks becoming distributed across runtime loading, repository handling, tool modules, and server startup logic. That produces several failure modes:

- dependency resolution is duplicated across requirement types;
- installation and verification are treated as the same operation;
- tool execution may discover missing dependencies too late;
- environments may be mutated during ordinary tool calls;
- repository-specific logic leaks into the server or runtime loader;
- installation results are difficult to cache, inspect, reproduce, or audit;
- undeclared Python dependencies tempt unsafe `pip install <import-name>` behavior.

## Goal

Introduce one library boundary that turns a tool requirement into a verified, reusable provisioned resource.

```text
requirement
  -> resolve source and requested revision
  -> inspect requirement metadata
  -> install into an isolated destination
  -> verify the installed resource
  -> record evidence and resolved state
  -> expose READY, FAILED, or REPAIR_REQUIRED
```

## Primary Deliverable

A new Maven module with a stable provisioning contract and an initial Python Git repository provisioning path.

## Scope Boundaries

The module is responsible for:

- provisioning lifecycle orchestration;
- isolated installation workspaces;
- installation and verification contracts;
- provider selection by requirement type;
- result and evidence modeling;
- retry/repair policy boundaries;
- reuse of a previously verified installation when its identity still matches;
- cleanup of incomplete staging environments;
- Python repository environment provisioning as the first provider.

The module is not responsible for:

- registering MCP tools;
- invoking MCP functions;
- deciding caller authorization;
- publishing untrusted repositories as approved Meshingress artifacts;
- implementing a general-purpose package manager;
- silently installing packages during normal tool execution;
- replacing Maven, `uv`, `pip`, Poetry, PDM, Railpack, FawltyDeps, Git, or another underlying resolver;
- defining database schemas or repository persistence internals.

## Desired Outcome

A tool dependency can be installed before the tool becomes callable. A successful provision produces a verified runtime location and evidence describing what was installed and how it was checked. A failed provision prevents the affected tool from being marked ready.
