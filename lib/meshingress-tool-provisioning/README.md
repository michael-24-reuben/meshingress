# MCP Tool Module Lifecycle — `meshingress-tool-provisioning`

## Package Role

This package defines and implements verified provisioning of tool prerequisites, including pinned Git sources and Python virtual environments.

## User-Visible Contribution

An installable tool can declare prerequisites and report whether they were provisioned, reused, unsupported, or failed before activation.

## Position in the Feature Path

```text
manifest requirement
  -> ToolProvisioningService
  -> ToolProvisioner implementation
  -> provisioned resource / evidence
  -> runtime activation gate
```

## Entry Points

- `ToolProvisioningService` and `ToolProvisioner`.
- `ProvisioningResult`, `ProvisionedResource`, and `ProvisioningEvidence`.
- Git and Python request records under `provisioning.git` and `provisioning.python`.

## Feature Contract

```yaml
input: provisioner-specific request
output: ProvisioningResult
states: ready | reused | failed | unsupported
evidence: source and environment facts suitable for review
```

## Dependencies

- Upstream: manifest requirements and `StaticManifestToolProvisioningGate`.
- Downstream: filesystem-backed tool prerequisites and `meshingress-tool-runtime-loader` activation.

## Failure Behavior

Unsupported requirement kinds, mutable or invalid source definitions, and failed external commands produce a non-ready provisioning result for the caller to block activation.

## Verification

```powershell
.\mvnw.cmd -pl lib/meshingress-tool-provisioning -am test
```

## Evidence and Open Questions

Confirmed by `ToolProvisioningService`, Git/Python request models, and provisioning tests. External tools and network access are deployment prerequisites, not bundled configuration.
