# Verification

## Commands

```powershell
.\mvnw.cmd -pl lib/meshingress-tool-annotations -am clean test '-Dmaven.compiler.source=22' '-Dmaven.compiler.target=22' '-Djava.version=22' '-Dmaven.compiler.release=22'
```

## Result

- `meshingress-tool-api`: success.
- `meshingress-tool-annotations`: success.
- Tests run: 2.
- Failures: 0.
- Errors: 0.
- Skipped: 0.

The Java 22 overrides were required because the live root POM targets Java 25 while the installed local JDK is Java 22.
