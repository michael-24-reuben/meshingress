# Fixes

## Files Changed

- `lib/meshingress-artifact-security/pom.xml`
- `lib/meshingress-artifact-security/src/main/java/dev/mrk/meshingress/artifact/security/ScannerFailurePolicy.java`
- `lib/meshingress-artifact-security/src/main/java/dev/mrk/meshingress/artifact/security/ScannerProcessRequest.java`
- `lib/meshingress-artifact-security/src/main/java/dev/mrk/meshingress/artifact/security/ScannerProcessResult.java`
- `lib/meshingress-artifact-security/src/main/java/dev/mrk/meshingress/artifact/security/ScannerProcessRunner.java`
- `lib/meshingress-artifact-security/src/main/java/dev/mrk/meshingress/artifact/security/ScannerProcessStatus.java`
- `lib/meshingress-artifact-security/src/test/java/dev/mrk/meshingress/artifact/security/ScannerProcessRunnerTests.java`

## Behavioral Changes

- Added an argv-based `ScannerProcessRunner` that executes configured scanner commands without shell concatenation.
- Added `ScannerProcessRequest` for scanner id, command argv, working directory, timeout, environment overrides, and output-size bound.
- Added `ScannerProcessResult` and `ScannerProcessStatus` for success, non-zero exit, timeout, start failure, and interrupted execution.
- Added normalized failure conversion from process failure to `ScannerResult`, including command metadata, duration, stdout/stderr, timeout, exit code, and a `Finding`.
- Added `ScannerFailurePolicy.scannerStatusForFailure()` so `BLOCK` maps to `BLOCKED`, `REVIEW` maps to `REVIEW`, and `IGNORE` still maps process failure to `FAILED` rather than falsely approving a required scanner.
- Added `spring-boot-starter-test` as a test dependency for the artifact-security module.

## Non-Changes

- No Syft, Grype, Trivy, ClamAV, YARA, Semgrep, CodeQL, Scorecard, or Cosign adapter was added.
- No host scanner installation was attempted.
- No repository publication policy or runtime installation code was changed.
