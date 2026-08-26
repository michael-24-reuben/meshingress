# Verification

## Automated Tests

Ran the server module and required upstream module tests:

```powershell
.\mvnw.cmd -pl app/meshingress-server -am test '-Dmaven.compiler.source=22' '-Dmaven.compiler.target=22' '-Dmaven.compiler.release=22' '-Djava.version=22'
```

Result:

```txt
Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Notes

The same command without Java overrides failed before test execution because the repository targets Java 25 and the local JDK only supports Java 22:

```txt
Fatal error compiling: error: release version 25 not supported
```

The successful run used the established local override for this checkout.
