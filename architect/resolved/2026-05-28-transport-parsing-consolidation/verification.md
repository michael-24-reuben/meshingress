# Verification

## Automated Checks

- `.\mvnw.cmd -pl app/meshingress-server,lib/meshingress-tool-framework -am test "-Dtest=dev.mrk.meshingress.mcp.McpControllerTests,dev.mrk.meshingress.framework.scanning.McpToolAnnotationScannerTests" "-Dsurefire.failIfNoSpecifiedTests=false"`
  - Result: passed. `McpControllerTests` ran 19 tests with 0 failures and 0 errors.
- `.\mvnw.cmd -pl app/meshingress-server -am test`
  - Result: passed. Reactor test run completed with 52 tests, 0 failures, 0 errors, and 0 skipped.

## Coverage Notes

- HTTP invalid JSON now has explicit parse-error coverage.
- Existing HTTP dispatch tests continued to pass through the shared transport dispatcher.
- WebSocket oversized-message behavior remains transport-specific and unchanged.
