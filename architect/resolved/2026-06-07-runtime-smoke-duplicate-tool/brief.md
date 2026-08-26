# Brief

Investigate and fix the broad server smoke failure in `ToolRuntimeLoaderSmokeTestResults`.

## Observed Failure

The broad smoke command failed with:

```txt
IllegalStateException: Duplicate MCP function descriptor name: helloworld.text
```

The focused `ToolRuntimeLoaderSmokeTests` command passed, so this entry should determine whether the duplicate comes from test fixture overlap, shared runtime state across smoke tests, registry behavior, or an actual runtime-loader defect.

## Scope

- `app/meshingress-server/src/test/java/dev/mrk/meshingress/mcp/tools/runtime/ToolRuntimeLoaderSmokeInstance.java`
- `app/meshingress-server/src/test/java/dev/mrk/meshingress/mcp/tools/runtime/ToolRuntimeLoaderSmokeTests.java`
- Runtime-loader and registry code only if the failure proves to be production behavior instead of test isolation.

## Non-Goals

- Do not implement the CycloneDX SBOM slice in this entry.
- Do not change unrelated toolspace modules.
- Do not clean unrelated worktree state.
