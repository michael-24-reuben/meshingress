# Verification

## Passed

```powershell
.\mvnw.cmd -pl app/meshingress-server -am test -DskipTests
.\mvnw.cmd -pl app/meshingress-server -am test "-Dtest=McpWebSocketHandlerProgressTests" "-Dsurefire.failIfNoSpecifiedTests=false"
```

The reactor compiled successfully. The focused WebSocket progress suite passed
after validating the server-side transport-evidence compatibility adapter.
`McpControllerOutputTests` also passed all 11 tests after Spring startup with
the injected context factory. `rg` found no remaining public-server use of
`authorizationHeader()`, `roleHeader()`, or `adminHeader()`.

## Not Claimed

The broader `McpControllerTests` class is not recorded as passing: its selected
run reported legacy registration assertions and later test-context loading
errors, so it needs an isolated follow-up rather than a claimed green suite.
Comprehensive dynamic-availability runtime-policy tests are a
verification-hardening follow-up; existing scanner availability metadata
remains the static list-time input.
