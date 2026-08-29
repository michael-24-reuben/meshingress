# Verification

## Passed

```powershell
.\mvnw.cmd -B -pl app/meshingress-server -am -DskipTests package
```

The complete 28-module server reactor built successfully. The executable server archive contains the distribution, starter, and all six selected tool JARs under `BOOT-INF/lib`.

```powershell
jar tf app\meshingress-server\target\meshingress.jar
```

```powershell
.\mvnw.cmd -B -pl app/meshingress-tool-bundle -am -DskipTests package
```

The legacy compatibility bridge and its transitive starter, tools, and distribution packaged successfully.

```powershell
.\mvnw.cmd -B -pl app/meshingress-server -am '-Dtest=BundleToolRegistrationStrategyTests,McpControllerTests,McpToolRegistrationPhaseApiSampleTests' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

`BundleToolRegistrationStrategyTests` passed 3/3 and `McpToolRegistrationPhaseApiSampleTests` passed 4 tests with 2 intentional skips. `McpControllerTests` ran 19 tests with 9 failures that returned `Admin role is required`; this broad class did not reach usable registration behavior for the updated assertion and is not used as a failure signal for the canonical-name migration.

## Known Test Limitation

```powershell
.\mvnw.cmd -B -pl app/meshingress-server -am test
```

This stopped in the unmodified `meshingress-tool-api` module at `DispatchExecutionResultTests.rawStructuredContentStillPreservesTheCallerProvidedWireShape`: expected raw `{"legacy":true}` but received the generated JSON-object envelope. No distribution, starter, or tool POM had begun building when the failure occurred.
