# Scanner Process Runner Policy

## Goal

Add a narrow, reusable scanner process runner boundary for future external scanner CLI integrations.

The runner should execute configured scanner commands with deterministic timeouts, captured stdout/stderr, exit status, and normalized failure policy metadata. It should not integrate Syft, Grype, Trivy, ClamAV, YARA, Semgrep, or CodeQL directly in this slice.

## Problem

The repository assessment pipeline now has embedded Java/library evidence, raw report retention, sandbox metadata, review policy, and publication eligibility checks. The remaining external scanner CLI work needs a common execution boundary before tool-specific adapters are added.

Without this boundary, every scanner adapter would need to invent its own timeout handling, unavailable-tool behavior, raw output capture, and fail-open/fail-closed semantics.

## Scope

In scope:

- Add a scanner process runner abstraction in the security/repository assessment area.
- Capture command, arguments, working directory, timeout, exit code, duration, stdout, stderr, and failure reason.
- Add a timeout/failure policy model that can map scanner failure into repository assessment status.
- Add focused tests for success, non-zero exit, missing executable, and timeout behavior.
- Keep uploaded `requestedScopes` as claims; do not change requested/inferred/approved/denied scope semantics.

Out of scope:

- Tool-specific Syft, Grype, Trivy, ClamAV, YARA, Semgrep, CodeQL, or Scorecard adapters.
- External scanner installation or host package management.
- Dynamic sandbox execution.
- Publication eligibility rewrites beyond consuming normalized scanner failure evidence if needed by tests.

## Acceptance Criteria

- A future scanner adapter can call one runner API instead of using `ProcessBuilder` directly.
- Timeouts terminate scanner processes and report deterministic timeout metadata.
- Missing executables and non-zero exits produce normalized failure evidence.
- Required scanner failures can be represented as fail-closed evidence for repository assessment.
- Focused Maven verification passes.
