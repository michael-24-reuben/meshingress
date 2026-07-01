# Assessment

The repository scanner backlog already had `ScannerFailurePolicy`, `ScannerPipelinePlan`, and `ScannerPipelineStage` classes in `lib/meshingress-artifact-security`, plus repository configuration that attaches timeout and failure-policy metadata to scanner results.

The missing boundary was process execution itself. Future external scanner CLI adapters would otherwise have to call `ProcessBuilder` directly and duplicate timeout, missing executable, non-zero exit, output capture, and failure-to-scanner-status handling.

This slice resolved only that shared runner boundary. It did not add any tool-specific external scanner adapter or host scanner installation.

## Scope Notes

- Uploaded `requestedScopes` remain claims only.
- The requested, inferred, approved, and denied scope separation was not changed.
- The runner reports process failure evidence; scanner adapters remain responsible for parsing successful scanner output into scanner-specific findings.
