# Verification

## Backup

- `robocopy` backup completed successfully to `J:\LBUF\meshingress-robocopy-no-sql-20260702-155628`.
- The earlier full backup attempt at `J:\LBUF\meshingress-robocopy-20260702-155012` failed on the locked H2 file under `repository\sql`; the clean backup excluded only `repository\sql`.

## Commands

```powershell
.\mvnw.cmd -pl lib\meshingress-tool-api-manifest -am test
```

Result: build success. `McpToolMetadataTests` passed.

```powershell
.\mvnw.cmd -pl toolspace\powershell-cli -am test
```

Result: build success.

```powershell
.\mvnw.cmd -pl app\meshingress-repository -am "-Dtest=ArtifactRepositoryNativeMetadataExportTests,ArtifactRepositoryFlowTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result: build success. 8 tests passed.

```powershell
.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=RuntimeToolCacheTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result: build success. 5 tests passed.

```powershell
rg -n "McpToolReadme|@McpToolProperty|McpToolProperties|McpToolProperty\.java" lib app toolspace
```

Result: no matches.

```powershell
git diff --check -- app\meshingress-repository app\meshingress-server lib\meshingress-tool-api-manifest toolspace\powershell-cli
```

Result: no whitespace errors. Git reported existing CRLF conversion warnings for touched files.
