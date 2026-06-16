# Context

## Findings To Revisit

- `ExperimentalToolRegistrationStrategy` requires `localJar.checksumSha256` when configured, but the current path should be reviewed to ensure it verifies the supplied checksum before runtime activation.
- `BundleToolRegistrationStrategy` already verifies a local JAR checksum before installing it into the local Maven repository.
- `McpToolRegistrationPhaseApiSampleTests` documents that its staging helper does not emulate production repository artifact installation behavior.

## Boundary

The production path should be repository publication records. Direct registration should remain development-oriented unless a later design intentionally promotes it.

## Related Work

- `architect/active/2026-05-27-meshingress-repository-artifact-implementation`
