# Verification

## Commands

```powershell
$env:JAVA_HOME='{{local:path.java25}}'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -pl app/meshingress-server -am test "-Dtest=dev.mrk.meshingress.mcp.McpControllerTests" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Result: passed. `McpControllerTests` ran 13 tests with 0 failures and 0 errors.

```powershell
$env:JAVA_HOME='{{local:path.java25}}'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -pl app/meshingress-server -am test
```

Result: passed. The server reactor test slice ran 40 tests with 0 failures and 0 errors.

## Notes

The full test command emitted existing Java 25 Mockito dynamic-agent warnings. They did not fail the build.
