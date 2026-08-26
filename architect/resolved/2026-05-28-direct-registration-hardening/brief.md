# Brief: Direct Registration Hardening

## Goal

Track minor hardening gaps in direct MCP tool registration paths that predate the repository artifact implementation.

## Origin Location

- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/ExperimentalToolRegistrationStrategy.java`
- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/BundleToolRegistrationStrategy.java`
- `app/meshingress-server/src/test/java/dev/mrk/meshingress/mcp/McpToolRegistrationPhaseApiSampleTests.java`

## Context

The repository implementation will become the production path for installing reviewed artifacts through signed publication records. Existing direct registration phases are still useful for development and smoke testing, but they should be reviewed later for consistency with checksum, provenance, and approval expectations.

This is not blocking the repository MVP.
