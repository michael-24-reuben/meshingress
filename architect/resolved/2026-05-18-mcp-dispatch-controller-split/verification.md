# Verification

Ran:

```powershell
./mvnw.cmd -pl app/meshingress-server -am test
```

Result:

- Reactor build succeeded.
- `meshingress-tool-api`, `helloworld`, and `meshingress-server` succeeded.
- Tests run: 19.
- Failures: 0.
- Errors: 0.
- Skipped: 0.

Notes:

- Maven still prints Mockito dynamic-agent warnings from the test stack. They did not fail the run.
