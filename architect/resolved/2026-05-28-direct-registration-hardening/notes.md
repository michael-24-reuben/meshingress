# Notes

## 2026-06-08 Activation

- Activated after resolving the embedded CycloneDX SBOM assessment slice.
- This objective should stay separate from repository-backed publication installation.
- First safe action: inspect direct registration strategies and tests, then add focused negative coverage for checksum mismatch if the current code path needs it.

## 2026-06-14 Resolution

- Reviewed the current diffs in `ExperimentalToolRegistrationStrategy` and `McpToolRegistrationPhaseApiSampleTests`.
- Confirmed `BundleToolRegistrationStrategy` already verifies local-JAR checksums before installing into the local Maven repository.
- Confirmed the experimental direct local-JAR path now verifies an optional supplied checksum before calling `runtimeLoader.activate(...)`.
- Corrected the checksum mismatch test so it uses a non-bundled tool id and reaches checksum validation instead of classpath override protection.
- Verification passed with the focused MCP registration sample test class.
