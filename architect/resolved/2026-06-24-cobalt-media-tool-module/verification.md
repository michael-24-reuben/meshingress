# Verification

Commands run:

```powershell
$env:JAVA_HOME='C:\Users\jbeas\scoop\apps\temurin25-jdk\current'
.\mvnw.cmd -pl toolspace\cobalt -am "-Dtest=CobaltClientTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl app\meshingress-server -am "-Dtest=McpCobaltToolMvcTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
git diff --check
```

Results:

- `CobaltClientTests`: passed, 3 tests.
- `McpCobaltToolMvcTests`: passed, 2 tests.
- `git diff --check`: exit code 0; only existing LF-to-CRLF warnings were printed.

Manual checks:

- Confirmed `toolspace/cobalt/upstream/cobalt/.git` was removed after recording the upstream commit.
- Confirmed the upstream files remain under `toolspace/cobalt/upstream/cobalt`.
