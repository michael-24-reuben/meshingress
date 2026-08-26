# Fixes

## Files Changed

- `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/roles/registration/ExperimentalToolRegistrationStrategy.java`
- `app/meshingress-server/src/test/java/dev/mrk/meshingress/mcp/McpToolRegistrationPhaseApiSampleTests.java`

## Behavioral Changes

- Experimental local-JAR registration now calculates the SHA-256 digest of the resolved JAR before runtime activation when `localJar.checksumSha256` is provided.
- A checksum mismatch now fails with `TOOL_REGISTRATION_CHECKSUM_MISMATCH` and JSON-RPC invalid params instead of activating the module.
- The focused MCP sample test now asserts the checksum mismatch error through `roles/tools/register` using a non-bundled tool id.

## Non-Changes

- Direct registration remains development-oriented.
- Repository-backed publication installation remains the trusted production path.
- Scope approval semantics, repository UI, external scanner CLI behavior, and broad runtime-loader behavior were not changed.
