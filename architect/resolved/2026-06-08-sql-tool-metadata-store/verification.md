# Verification

## Commands

```bash
.\mvnw.cmd -pl app/meshingress-repository -am "-Dtest=ArtifactRepositoryFlowTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl app/meshingress-server -am "-Dtest=McpPublicationInstallTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

## Results

- Repository flow passed: 1 test, build success.
- Server publication install passed: 8 tests, build success.

## Additional Checks

- Searched for canonical JSON metadata writes. Remaining `record.json`, `latest-review.json`, and `publication.json` references are negative assertions in the repository flow test.
- Confirmed raw scanner report files still exist for `assessment.json` and `cyclonedx-sbom.json`.

## Remaining Risk

The SQL implementation is intentionally narrow and repository-local. Direct MCP registration durability still requires a later SQL-backed `ToolRegistrationStore` slice.
