# Meshingress service launcher

`Meshingress.ps1` manages the two packaged Spring Boot services:

- `meshingress-repository` on port `8080` by default.
- `meshingress-server` on the address and port in its application properties, currently `100.121.15.11:4737`.

Run it from the project root or any child directory:

```powershell
.\scripts\Meshingress.ps1
```

The launcher uses existing jars. It automatically builds only when a jar is missing. Use `-Build` to package fresh jars before starting; add `-SkipTests` only when a separate test run has already been completed.

```powershell
.\scripts\Meshingress.ps1 -Build
.\scripts\Meshingress.ps1 -Build -SkipTests
.\scripts\Meshingress.ps1 -Debug -Foreground
.\scripts\Meshingress.ps1 -EnableJvmDebug -Foreground
.\scripts\Meshingress.ps1 -Headless
```

`-Debug` enables Spring Boot and Meshingress debug logging. `-EnableJvmDebug` exposes local-only JVM debugger ports `5005` for the server and `5006` for the repository; override them with `-ServerDebugPort` and `-RepositoryDebugPort` if needed.

`-Headless` and `-Detached` both start a hidden background launcher, then return the calling terminal without opening the control-center UI. Its lifecycle trace is written to `var/logs/meshingress/headless.log`.

Logs are written to `var/logs/meshingress/` with separate stdout and stderr files for each service. The latest managed service state is stored in `var/run/meshingress-services.json`; both locations are ignored by Git.

```powershell
.\scripts\Meshingress.ps1 -Action Status
.\scripts\Meshingress.ps1 -Action Logs
.\scripts\Meshingress.ps1 -Action Stop
.\scripts\Meshingress.ps1 -ForceRestart
```

The launcher blocks duplicate starts and existing port listeners. It waits for the repository TCP listener and for the MCP server's anonymous actuator health endpoint before declaring success. Use `-ServerAddress`, `-ServerPort`, `-RepositoryAddress`, and `-RepositoryPort` to override the normal bindings for a launch.
