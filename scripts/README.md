# Meshingress launchers

Meshingress runs as one packaged Spring Boot application:

- JAR: `app/meshingress-server/target/meshingress.jar`
- Server health: `http://<server-address>:<server-port>/actuator/health`
- Artifact surface: `http://<server-address>:<server-port>/artifact`

Use the root script for your operating system; it forwards to the platform-specific action dispatcher:

```powershell
.\Meshingress.ps1 -Action Start
```

```bash
bash ./scripts/Meshingress.sh Start
```

The launchers use the existing JAR and build it only when missing. Use `Build` / `--build` to package a fresh JAR, and `SkipTests` / `--skip-tests` only when tests were run separately.

```powershell
.\Meshingress.ps1 -Action Start -Build
.\Meshingress.ps1 -Action Start -Headless
.\Meshingress.ps1 -Action Status
.\Meshingress.ps1 -Action Stop
.\Meshingress.ps1 -Docs OpenAPI
.\Meshingress.ps1 -Docs README
```

`-Docs` is separate from `-Action`. Its values are `OpenAPI`, `README`, `LICENSE`, and `CHANGELOG`.

```bash
bash ./scripts/Meshingress.sh Start --build
bash ./scripts/Meshingress.sh Start --headless
bash ./scripts/Meshingress.sh Status
bash ./scripts/Meshingress.sh Stop
```

`Debug` / `--debug` enables Spring Boot debug logging. `EnableJvmDebug` / `--enable-jvm-debug` opens a loopback-only debugger listener on port `5005`; override it with `ServerDebugPort` / `--server-debug-port`.

`Headless` / `--headless` starts the application through a detached launcher. Logs are written to `var/logs/meshingress/`; managed process state is stored in `var/run/meshingress-services.json`. Both paths are ignored by Git.

The launchers block duplicate starts and existing port listeners, and wait for the anonymous actuator health endpoint before reporting success. Use `ServerAddress` / `--server-address` and `ServerPort` / `--server-port` to override the normal binding.
