# Context

## Current Evidence

- `scripts/maintain.ps1` only provisions a Mockito agent and cannot locate the current project root because it requires the deleted `app/meshingress-repository` module.
- `scripts/download-mockito-agent.ps1` also targets that deleted module.
- The root Maven build already copies `org.mockito:mockito-core` into each module's `target/agents/mockito-core.jar` during `process-test-classes` and configures Surefire with that JAR as `-javaagent`.
- Mockito is a test-scoped Maven dependency; application runtime must not depend on a manually downloaded Mockito JAR.

## Ownership Model

| Class | Authoritative source | Maintenance behavior |
|---|---|---|
| Maven dependencies | Maven wrapper, `pom.xml`, and resolved Maven metadata | Build, resolve, and verify only; never change versions. |
| Wrapped vendors | Committed vendor lock manifest | Validate by default; download or replace only with an explicit repair command. |
| User/project-stored tools | Project/runtime storage | Report availability only; never overwrite, move, or delete. |
| Runtime data | Runtime storage | Never clean or rebuild as part of maintenance. |

## Definition: Wrapped Vendor

A wrapped vendor is a third-party executable, archive, script, model, or runtime that Meshingress downloads, packages, or launches on behalf of a tool module, but that is not resolved by Maven as a normal Java dependency.

The project must record a wrapped vendor before maintenance is allowed to download or replace it.
