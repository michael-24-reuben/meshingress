# Fixes

- Updated `scripts/windows/private/Runtime.ps1` to discover and adopt a verified running Meshingress server when state is absent or stale.
- Preserved the packaged-JAR matcher and added the exact development application main class as the only additional process identity.
- Routed menu, status, logs, stop, restart, and duplicate-start checks through discovery so the control center sees the same runtime state.
- Documented the Windows `spring-boot:run` discovery behavior and its log-following boundary in `scripts/README.md`.
