# Embedded Assessment Enrichment

## Goal

Start chapter 2 by enriching repository artifact assessment beyond the current bytecode scope scanner.

Chapter 1 completed the repository-to-runtime MVP and runtime publication acceptance gate. The next objective is to add embedded assessment metadata that improves reviewer confidence before external scanner integrations become necessary.

## Initial Scope

- Add CycloneDX SBOM generation for artifact inventory metadata.
- Preserve the current bytecode scope scanner as the first scope inference path.
- Keep assessment output consumable by repository review and publication records.
- Stage SpotBugs, FindSecBugs, CodeQL, source-aware matching, and broader rule coverage as follow-up slices under this same chapter 2 record.

## Non-Goals

- Do not add repository UI work in this objective.
- Do not add server-side publication fetching in this objective.
- Do not integrate external scanner CLIs such as Syft, Grype, Trivy, ClamAV, YARA, Semgrep, or Cosign in the first slice.
- Do not replace the existing bytecode scanner or scope rule catalog unless the SBOM model requires a narrow compatibility change.

## Completion Criteria

- The repository assessment pipeline can generate and store embedded SBOM inventory metadata for uploaded artifacts.
- Tests prove SBOM generation works for a representative JAR artifact.
- The assignment file records what was implemented, what remains, and the safest next chapter 2 action.
