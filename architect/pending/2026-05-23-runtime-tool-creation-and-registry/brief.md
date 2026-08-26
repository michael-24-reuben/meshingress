# Runtime Tool Creation and Registry

## Goal

Create a comfortable starting foundation for Meshingress tool creation that separates authoring, distribution, and runtime installation.

## Core Applications

1. Keep `toolspace/` as the source workspace for first-party and locally authored tools.
2. Add a separate runtime `tools/` installation area for validated, built tool artifacts.
3. Add a Meshingress artifact registry with a webpage and API so foreign tool authors can depend on Meshingress libraries without cloning the monorepo.
4. Support runtime-installed tools that can be attached without server restart.
5. Add runtime configuration support, including custom environment variables per installed tool.
6. Add or formalize scope protection for reading runtime environment variables.

## Initial Decision

This is a solid starting point for tool creation. It preserves the existing static module workflow while opening a path to dynamic/runtime tool installation.

## Scope Boundary

This entry describes the product and architecture package. It does not implement the dynamic classloader, registry service, or installer yet.
