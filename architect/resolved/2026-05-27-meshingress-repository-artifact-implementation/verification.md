# Verification

## Commands Run

```powershell
./mvnw.cmd -pl app/meshingress-server -am "-Dtest=McpPublicationInstallTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result: passed. `McpPublicationInstallTests` ran 8 tests with 0 failures, 0 errors, and 0 skipped.

```powershell
./mvnw.cmd -pl app/meshingress-server -am test
```

Result: passed. Server reactor ran 60 tests with 0 failures, 0 errors, and 0 skipped.

```powershell
./mvnw.cmd -pl app/meshingress-repository -am test
```

Result: passed. Repository reactor ran the repository flow test and upstream scope-scanner tests successfully.

## Covered Runtime Gate Cases

- Valid signed approved publication installs and exposes the expected MCP function.
- Invalid publication signature is rejected.
- Unsigned publication record is rejected.
- Artifact checksum mismatch is rejected.
- Revoked publication record is rejected.
- Non-installable trust status is rejected.
- Installed function scope missing from publication `approvedScopes` is rejected.
- Publication approval of a locally disabled scope is rejected.

## Remaining Risk

The test run reported existing JDK/Mockito dynamic-agent warnings and a deprecated `Unsafe` warning from Guava in the repository reactor. They did not fail the build and are not specific to this architect objective.
