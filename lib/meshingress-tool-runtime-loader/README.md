# MCP Tool Module Lifecycle — `meshingress-tool-runtime-loader`

## Package Role

This package resolves a packaged tool, creates its isolated Spring runtime, registers handlers, and tracks its lifecycle.

## User-Visible Contribution

An operator can load a bundled, local JAR, local Maven coordinate, or plugin-directory tool and have its functions become callable through the server registry.

## Position in the Feature Path

```text
tool source or artifact coordinate
  -> resolver chain
  -> provisioning gate
  -> classloader + Spring context
  -> registration bridge
  -> callable MCP functions
```

## Entry Points

- `runtime.loader.DefaultToolRuntimeLoader` and `ToolRuntimeLoader`.
- Artifact sources/resolvers under `runtime.artifacts`.
- `StaticManifestToolProvisioningGate`.
- Lifecycle types `ToolModuleId`, `ToolModuleHandle`, `ToolModuleStatus`, and `ToolModuleState`.
- Spring context and classloader factories under `runtime.spring`.

## Local Execution Flow

The loader resolves a `ToolArtifactSource`, applies prerequisite gating, creates a module classloader and application context, derives handlers, and registers them through `ToolRegistrationBridge`. Its lifecycle types expose the resulting registration/state to the caller.

## Configuration and Resources

The dependency on `meshingress-config` provides application-supplied runtime settings. This package has no main resource; effective property values and precedence are owned by the consuming server.

## Feature Contract

```yaml
input: ToolArtifactSource
output: ToolModuleHandle and ToolModuleRegistration
sideEffects:
  - classloading
  - Spring application-context creation
  - tool-function registration
```

## Failure Behavior

Resolution, provisioning, classloading, context creation, or handler registration can stop activation. The caller decides how to surface the non-active module state.

## Verification

```powershell
.\mvnw.cmd -pl lib/meshingress-tool-runtime-loader -am test
```

## Evidence and Open Questions

Confirmed by `DefaultToolRuntimeLoader`, resolver-chain, Spring-factory, and lifecycle types plus focused resolver/provisioning tests.
