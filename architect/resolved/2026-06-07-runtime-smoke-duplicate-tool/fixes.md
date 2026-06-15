# Fixes

## Files Changed

- `app/meshingress-server/src/test/java/dev/mrk/meshingress/mcp/tools/runtime/ToolRuntimeLoaderSmokeInstance.java`

## Behavioral Changes

- `ToolRuntimeLoaderSmokeTestResults` now sets `meshingress.tools.registry.scan-on-startup=false`.
- The test still uses the production registry bridge and strict duplicate enforcement.
- The registry starts empty for the runtime-loader sample activation, matching the intent of a runtime installation smoke test.

No production code was changed.
