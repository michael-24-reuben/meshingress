# Verification

## Command

```powershell
.\mvnw.cmd -pl app/meshingress-server -am test '-Dmaven.compiler.source=22' '-Dmaven.compiler.target=22' '-Djava.version=22' '-Dmaven.compiler.release=22'
```

## Result

- Reactor modules: `meshingress`, `meshingress-tool-api`, `meshingress-tool-annotations`, `helloworld`, `instagram`, `meshingress-server`.
- Annotation scanner tests: 3 run, 0 failures, 0 errors.
- Server tests: 27 run, 0 failures, 0 errors.
- Reactor result: `BUILD SUCCESS`.

Existing MVC tests verified `tools/list` includes the attached helloworld module and `tools/call` returns `Hello, Meshingress!` through the server path.
