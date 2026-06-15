# Notes

## 2026-06-07

- Created from PAS next action.
- Initial search found the failure only recorded in PAS, ASSIGNMENT, and embedded assessment notes; no dedicated architect existed for the smoke baseline.
- The likely affected tests are `ToolRuntimeLoaderSmokeTestResults` and `ToolRuntimeLoaderSmokeTests`.

## Resolution

- Reproduced the broad smoke failure. The first runtime activation in `ToolRuntimeLoaderSmokeTestResults` failed before any unload path ran.
- Root cause: `ToolRuntimeLoaderSmokeTestResults` uses the real Spring `ToolRegistry` while startup scanning is enabled. The bundled application context can already contain a `helloworld.text` function before the runtime-loader sample JAR is activated.
- Applied the same test-scoped startup-scan override used by repository publication install tests: `meshingress.tools.registry.scan-on-startup=false`.
- Focused runtime smoke and broad smoke verification both passed after the change.
