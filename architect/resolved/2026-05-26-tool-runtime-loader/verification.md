# Verification

## Commands Run

```powershell
$env:JAVA_HOME='{{local:path.java25}}'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -pl lib/meshingress-tool-runtime-loader -am test
```

Result: passed. The new resolver unit test confirmed that a Maven coordinate source resolves the locally installed main JAR and includes a locally installed runtime dependency while excluding a test-scoped dependency.

```powershell
$env:JAVA_HOME='{{local:path.java25}}'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -pl app/meshingress-server -am test -DskipTests
```

Result: passed. The server reactor compiled with the runtime-loader module, registry bridge, runtime handler factory, and existing toolspace modules.

## Notes

- The first compile attempt without setting `JAVA_HOME` failed because the default shell JDK did not support Maven release 25.
- Full tests were not run for the server reactor; the server verification command compiled tests but skipped execution.
- Remote Maven/private repository resolution and checksum/signature policy were not verified because they are not part of this MVP implementation.
