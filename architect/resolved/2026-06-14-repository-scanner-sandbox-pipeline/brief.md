# Brief: Repository Scanner Sandbox Pipeline

## Goal

Introduce a configurable real scanner pipeline for repository artifact assessment, including malware/sandbox analysis and dependency-aware SBOM enrichment.

## Scope

- Define scanner pipeline configuration: required scanners, optional scanners, timeouts, versions, and failure policy.
- Add a sandboxed malware or behavior analysis stage before approval.
- Preserve raw scanner reports and compact summaries in repository metadata.
- Expand SBOM coverage beyond embedded JAR entries to include dependency-aware component metadata where possible.
- Keep scan execution isolated from the server runtime JVM.

## Non-Goals

- This entry does not replace the SQL metadata store.
- This entry does not define publication signing policy; that belongs in `2026-06-14-publication-trust-provenance-policy`.

