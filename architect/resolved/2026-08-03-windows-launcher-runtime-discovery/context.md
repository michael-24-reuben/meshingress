# Context

On 2026-08-03, PID 7800 listened on `100.121.15.11:4737` and returned HTTP 200 from `/actuator/health`. Its command line ran `dev.mrk.meshingress.MeshingressApplication` through Maven `spring-boot:run`; it was not a `java -jar app/meshingress-server/target/meshingress.jar` process.

`scripts/windows/private/Runtime.ps1` only accepted a recorded PID whose command line contained the recorded JAR path. `var/run/meshingress-services.json` was absent, so the control center reported `STOPPED` and `Status` reported no managed service.

The fix must retain the strict packaged-JAR matcher and add only a verified development-runtime form. A discovered process must own the configured listener, return healthy actuator status, and expose either the expected JAR path or the exact Meshingress application main class.
