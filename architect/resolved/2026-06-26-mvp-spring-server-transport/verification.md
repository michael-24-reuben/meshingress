# Verification

## Passed

```powershell
.\mvnw.cmd -pl app/nextcloud-mcp-server -am install -DskipTests
.\mvnw.cmd -pl app/nextcloud-mcp-server spring-boot:run '-Dspring-boot.run.arguments=--server.port=8080'
Invoke-WebRequest -Uri 'http://127.0.0.1:8080/health' -UseBasicParsing
```

Result: passed after installing reactor dependencies into the local Maven repository. `/health` returned:

```json
{"status":"UP","configLoaded":false}
```

```powershell
.\mvnw.cmd -pl app/nextcloud-mcp-server -am test
```

Result: passed.

```powershell
$env:NC_MCP_SDK_FUNCTIONALITY_TEST_ENABLED='false'
$env:NC_MCP_SMOKE_TEST_ENABLED='false'
.\mvnw.cmd test
```

Result: passed. The live SDK smoke tests were intentionally skipped by overriding the live flags for this Maven process.

```powershell
git diff --check -- app/nextcloud-mcp-server architect pom.xml
```

Result: passed; Git reported only CRLF normalization warnings for existing working-copy line endings.

```powershell
Get-ChildItem -Recurse -Filter meta.json -LiteralPath architect | ForEach-Object { Get-Content -LiteralPath $_.FullName | ConvertFrom-Json | Out-Null }
```

Result: passed.

## Environmental Failure Observed

```powershell
.\mvnw.cmd test
```

Result: failed after the server module passed, inside the existing live `NextcloudSdkFunctionalityTest`. The live Nextcloud host returned HTTP 502 for:

- `GET https://alphasunny11-07.tail93ea23.ts.net/ocs/v1.php/cloud/user`

This failure occurred in the admin module live smoke test and was not caused by the new server transport code.

## Remaining Risks

- JSON-RPC mapping is MVP-minimal and not a full MCP protocol compliance layer.
- Startup config validation is opt-in through `nextcloud.mcp.validate-on-startup`; without it, missing config is reported by endpoints instead of failing application startup.
- Java 25 test runs still emit the existing Mockito dynamic-agent warning from the Spring test stack.
