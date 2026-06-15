# Context

## Source Objective

This entry follows `architect/resolved/2026-05-27-meshingress-repository-artifact-implementation`.

That resolved entry explicitly deferred scanner and metadata enrichment after the runtime acceptance gate. Its immediate-objective handoff named the next phase as embedded assessment enrichment:

- CycloneDX SBOM generation for artifact inventory metadata.
- SpotBugs and FindSecBugs embedded Java findings.
- Broader scope-rule coverage for environment, secrets, network, process, and file behavior.
- Source-aware analysis and CodeQL query-pack generation when source/build context exists.

## Current Implementation Surface

- `lib/meshingress-artifact-scope-scanner/` contains the current reusable bytecode scope inference module.
- Current scanner output feeds repository artifact review through normalized inferred scopes.
- Existing tests live under `lib/meshingress-artifact-scope-scanner/src/test/java/dev/mrk/meshingress/artifact/scope/`.
- The current scanner module already depends on ASM and SootUp.

## Chapter 2 Direction

Start with embedded libraries and deterministic tests before external process-running scanners. The first slice should make the assessment pipeline produce useful artifact inventory metadata without depending on a local CLI install.

CycloneDX is the preferred first slice because it enriches repository review without changing runtime install policy. SpotBugs, FindSecBugs, and CodeQL should follow once the SBOM shape is settled.

## Branch

Expected branch for this chapter is `codex/chapter-2-embedded-assessment-enrichment`.
