# Context

## Parent Backlog

Parent entry: `architect/active/2026-05-27-meshingress-repository-artifact-implementation`.

Relevant unchecked backlog items:

- `Add scanner process runner with timeout`.
- `Add scanner timeout and failure policy`.
- `Integrate Grype for vulnerability scanning`.
- `Integrate Trivy for vulnerability/misconfiguration/secret scanning`.
- `Integrate ClamAV for malware scanning`.
- `Integrate YARA for suspicious binary/text patterns`.
- `Integrate Semgrep for source/static analysis`.
- `Add vulnerability severity thresholds`.

This entry intentionally takes only the shared process-runner and timeout/failure-policy foundation. Tool-specific external scanner adapters should be separate follow-up records.

## Current Verified Baseline

Resolved entries leading into this slice:

- `architect/resolved/2026-06-05-embedded-assessment-enrichment`
- `architect/resolved/2026-06-14-repository-scanner-sandbox-pipeline`
- `architect/resolved/2026-05-28-direct-registration-hardening`
- `architect/resolved/2026-06-18-publication-eligibility-policy`

The embedded assessment path already retains raw scanner/SBOM evidence and keeps `requestedScopes`, `inferredScopes`, `approvedScopes`, and `deniedScopes` separate.

## Design Boundaries

- Prefer a Java/library API in `lib/meshingress-artifact-security` unless live code shows the repository app already owns the better scanner execution boundary.
- Keep command construction argv-based; do not use shell string concatenation.
- Default scanner failures should be representable as fail-closed for required scanners.
- Raw scanner output should be bounded or written to report files by later adapters; the first runner may capture bounded strings for focused tests.
- Do not introduce external binary dependencies in this slice.
