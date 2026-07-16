# Context

## Dependency Chain

- `architect/resolved/2026-06-05-embedded-assessment-enrichment` left SpotBugs and FindSecBugs as follow-up embedded Java analysis slices.
- `architect/resolved/2026-06-27-grype-vulnerability-scanner-adapter` added the first tool-specific external scanner adapter and pipeline-only execution filtering.
- Parent backlog: `architect/active/2026-05-27-meshingress-repository-artifact-implementation`.

## Boundaries

- Prefer an embedded Java/library path for this slice.
- Do not require a host-installed SpotBugs CLI for normal verification.
- Keep FindSecBugs deferred unless the base SpotBugs adapter lands cleanly.
- Preserve the requested/inferred/approved/denied scope separation.

## Likely Files

- `lib/meshingress-artifact-security/src/main/java/dev/mrk/meshingress/artifact/security/`
- `lib/meshingress-artifact-security/src/test/java/dev/mrk/meshingress/artifact/security/`
- `app/meshingress-repository/src/main/java/dev/mrk/meshingress/repository/config/`
- `app/meshingress-repository/src/test/java/dev/mrk/meshingress/repository/artifact/`
