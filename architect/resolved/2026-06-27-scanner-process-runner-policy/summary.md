# Summary

Scanner process runner policy is resolved.

`lib/meshingress-artifact-security` now has a shared argv-based `ScannerProcessRunner` with timeout handling, normalized process statuses, bounded stdout/stderr capture, and failure conversion into scanner evidence. The existing scanner failure policy now maps process failures to scanner statuses without allowing ignored failures to masquerade as passed required scanners.

Focused verification passed for `ScannerProcessRunnerTests` and the downstream repository scanner-pipeline test. Tool-specific external scanner adapters remain the next separate slice.
