# Verification

## Live Runtime

- Confirmed PID 7800 was a Java process running `dev.mrk.meshingress.MeshingressApplication` through Maven `spring-boot:run`.
- Confirmed it owned `100.121.15.11:4737` and `/actuator/health` returned HTTP 200.
- Confirmed `var/run/meshingress-services.json` was initially absent.
- Ran `./Meshingress.ps1 -Action Status`; it adopted PID 7800 and reported `server: running`.
- Ran the same status command again; it recognized the saved runtime without a second adoption.
- The process was not stopped or restarted.

## Static Checks

- Parsed `scripts/windows/private/Runtime.ps1` with PowerShell `ScriptBlock.Create` before the live status checks.
