# SpotBugs Embedded Java Assessment Adapter

## Goal

Add an embedded Java static-analysis scanner adapter for SpotBugs findings in repository artifact assessment.

## Problem

The repository now has embedded SBOM/scope evidence and an opt-in Grype vulnerability scanner, but the parent backlog still lacks a Java bug/risk scanner that can run as an embedded library path without depending on host CLI installation. SpotBugs is the next focused embedded scanner slice before broader external scanner expansion.

## Scope

In scope:

- Inspect the current scanner adapter and repository assessment pipeline.
- Evaluate the smallest Maven dependency set for running SpotBugs against uploaded JAR artifacts.
- Add a SpotBugs-backed scanner adapter if the dependency and invocation path are viable in this reactor.
- Normalize SpotBugs findings into `ScannerResult` / `Finding` records.
- Keep tests independent from host-installed scanner CLIs.

Out of scope:

- FindSecBugs rules unless the base SpotBugs adapter is already stable and the dependency is low-risk.
- CodeQL, Semgrep, Syft, Trivy, ClamAV, YARA, Scorecard, Cosign, or host scanner installation.
- Trusting uploaded `requestedScopes` as authority.

## Acceptance Criteria

- Repository assessment can include a SpotBugs scanner result when configured.
- Findings include severity, code/id, message, and class/path context.
- The scanner has focused tests with a controlled fixture artifact.
- Focused Maven verification passes.
