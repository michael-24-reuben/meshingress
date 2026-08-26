# Verification

## Commands

- `.\mvnw.cmd -pl lib\meshingress-artifact-security -am "-Dtest=ScannerProcessRunnerTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Result: first run failed at compile because `ScannerProcessResult` used `Map.of(...)` with more than ten key/value pairs.
  - Fix: replaced that raw process metadata construction with a `LinkedHashMap`.
  - Rerun result: pass; 4 tests, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS`.

- `.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=ArtifactRepositoryScannerPipelineTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Result: pass; 1 test, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS`.

## Covered Cases

- Successful process captures stdout and stderr.
- Non-zero process exit is normalized as `SCANNER_PROCESS_NON_ZERO_EXIT`.
- Missing executable is normalized as `SCANNER_PROCESS_START_FAILED`.
- Timeout destroys the process and normalizes as `SCANNER_PROCESS_TIMEOUT`.
- Failure policy mapping preserves fail-closed behavior for `BLOCK`, review evidence for `REVIEW`, and does not turn `IGNORE` failures into `PASSED`.

## Remaining Risks

- Tool-specific adapters still need to decide how much successful scanner output to retain as raw reports versus compact summaries.
- The runner captures bounded output strings; future adapters that expect large reports should write raw reports to files and store paths in `ScannerResult.rawReportPath`.
