# Tool Module Model Context

This directory is a portable context bundle for models that need to build, edit, or review Meshingress tool modules. Repository source remains authoritative; the files under `reference/` are compact snapshots and can drift as the Java APIs evolve.

## Recommended upload set

For normal tool-module work, upload:

1. `BUILDING_TOOL_MODULES.md`
2. `reference/tool-annotations.md`
3. `reference/function-annotations.md`
4. `reference/input-schema-annotations.md`
5. `reference/dispatch-results.md`
6. `reference/scope-model.md`

Add `reference/configuration-annotations.md` for secrets, timeout, audit, or tracing behavior. Add `reference/availability-policies.md` only when a tool or function has conditional availability. Add `examples/mcp-client-ui-specification.md` only for MCP client/UI work; it is not required to implement a tool module.

## Source-of-truth order

When repository access is available, prefer current files in this order:

1. `toolspace/helloworld/` for the smallest attached annotation-based module.
2. `lib/meshingress-tool-api/` for call context, descriptors, result objects, and the direct SPI.
3. `lib/meshingress-tool-annotations/` for annotations, schemas, and scopes.
4. `lib/meshingress-tool-framework/` for annotation scanning, availability, and runtime wiring.
5. `app/meshingress-tool-bundle/pom.xml` for modules attached to server startup.
6. `app/meshingress-server/src/test/` for `tools/list` and `tools/call` integration patterns.

Do not copy server-internal registry or dispatcher classes into a tool module. A module should depend on public libraries and attach through Spring Boot auto-configuration.

