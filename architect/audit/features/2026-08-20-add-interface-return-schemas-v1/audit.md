# Capability Snapshot: Add Interface Return Schemas v1

- Feature ID: `add-interface-return-schemas-v1`
- Recorded: `2026-08-20T11:09:57-04:00`
- Status: Implemented declaration and discovery slice

## Feature memo

`@McpFunction` now has an optional `outputTypes` array. The annotation scanner converts a declared type into JSON Schema for the entire Meshingress `structuredContent` envelope: `kind`, `schema`, `version`, and `data`. A single type describes `data`; multiple types become `data.oneOf`.

Interface zero-argument abstract methods become properties, including JavaBean `get...` and boolean `is...` names. Records, fields, primitive values, enums, arrays and collections, maps, and recursive object graphs are also handled. The descriptor publishes the compiled `outputSchema` to MCP `tools/list`.

`structuredOutput` is a tri-state declaration: `INFER` means consumers infer it from `outputSchema`, while `ENABLED` and `DISABLED` emit an explicit Boolean annotation. Studio therefore honors an explicit value first, then uses output-schema presence as its fallback. The canvas explains whether the result contract is a declared schema, observed-schema policy, or disabled.

## Evidence summary

- Annotation contract: `McpFunction.java` lines 27-49.
- Descriptor publication: `McpFunctionDescriptor.java` lines 25-37.
- JSON Schema compilation and output-mode annotation: `McpToolAnnotationScanner.java` lines 155-162 and 626-776.
- Focused regression coverage: `McpToolAnnotationScannerTests.java` lines 183-205 and 338-375.
- Studio contract and display: `mcp.ts` lines 14-37 and `WorkflowCanvas.tsx` lines 732-736 and 792-826.

The focused verification command passed 10 tests with no failures:

```cmd
.\mvnw.cmd -pl lib/meshingress-tool-framework -am test "-Dtest=McpToolAnnotationScannerTests" "-Dsurefire.failIfNoSpecifiedTests=false"
```

## Known boundary

This snapshot does not claim server runtime JSON Schema validation, NetworkNT integration, validation diagnostics in MCP `_meta`, warn/enforce policies, Studio observed-schema caching/comparison, or configurable schema-drift event severity. Those remain active architect work.

`architect/audit/features/audit.schema.json`, referenced by the audit guidance, is not present in this workspace. The promoted Gemini-generated snapshot JSON was therefore manually inspected and normalized to the documented audit shape.

## Origin and currency

This is an implementation snapshot formed from source inspection, focused test output, and an Antigravity CLI / Gemini handoff. The handoff initially wrote into an isolated scratch workspace; its content was independently checked and promoted here. Live source and tests are authoritative, and later changes may make this record stale.
