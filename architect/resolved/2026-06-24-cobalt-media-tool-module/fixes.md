# Fixes

Files and areas changed:

- Added `toolspace/cobalt/` as a new Maven tool module.
- Vendored `imputnet/cobalt` under `toolspace/cobalt/upstream/cobalt`.
- Added `toolspace/cobalt/UPSTREAM.md` with source repository and commit `a636575b09de1fc55d9b8cd98cac88f5f2f16b42`.
- Added the Java MCP wrapper classes under `toolspace/cobalt/src/main/java/dev/mrk/toolspace/cobalt/`.
- Exposed `cobalt.info` and `cobalt.process`.
- Added Cobalt module auto-configuration with defaults:
  - `meshingress.cobalt.base-url=http://127.0.0.1:9000`
  - `meshingress.cobalt.auth-header=`
  - `meshingress.cobalt.user-agent=Meshingress-Cobalt/0.1`
- Added `toolspace/cobalt` to the root Maven reactor.
- Added `dev.mrk.toolspace:cobalt` to `app/meshingress-tool-bundle`.
- Added unit and MVC verification tests.
