# Verification

## Commands

```powershell
.\mvnw.cmd -pl app/meshingress-repository -am "-Dtest=ArtifactRepositoryFlowTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl app/meshingress-server -am "-Dtest=McpPublicationInstallTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

## Results

- `ArtifactRepositoryFlowTests`: passed, 1 test.
- `McpPublicationInstallTests`: passed, 8 tests.

## Remaining Risk

The repository still has no real malware scanner or sandbox scanner. That belongs in `2026-06-14-repository-scanner-sandbox-pipeline`.

