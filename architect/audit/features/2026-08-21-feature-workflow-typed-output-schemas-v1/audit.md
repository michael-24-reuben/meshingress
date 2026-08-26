# Capability Snapshot: Workflow Typed Output Schemas v1

- Feature ID: `workflow-typed-output-schemas-v1`
- Recorded: `2026-08-21T22:19:02-04:00`
- Status: Implemented

## Feature memo

Workflow node results distinguish a result contract from observations made while it ran.

For a tool that returns a concrete `StructuredContent` implementation, the workflow generates `NodeResult.outputSchema` from that content class. The schema describes the complete response envelope and fixes its `kind`, `schema`, and `version`; its `data` member describes the content class fields. It is not inferred from an individual response value.

The `structuredContent(JsonNode)` path becomes `GeneratedJsonContent`. It intentionally has no stable field-level payload contract, so workflow results leave `outputSchema` absent for it. This prevents one generated object from becoming a false promise about later objects.

`NodeResult.diagnostics` is a separate, non-fatal extensibility surface. Current output-schema validation facts become `output.schema.violation` or `output.schema.unavailable`, with warning severity and details. A successful validation creates no diagnostic.

Studio treats a supplied node output schema as declared-type evidence and caches it accordingly. Only an eligible schema-less structured result proceeds to observed-schema inference; an `output.schema.violation` also blocks that inference.

## Illustrative result

```json
{
  "result": {
    "kind": "process.execution",
    "schema": "meshingress.process.execution.v1",
    "version": 1,
    "data": { "status": "completed", "exitCode": 0 }
  },
  "outputSchema": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "properties": {
      "kind": { "const": "process.execution" },
      "schema": { "const": "meshingress.process.execution.v1" }
    }
  },
  "diagnostics": []
}
```

## Evidence summary

- Schema generation and generated-JSON exclusion: `StructuredContentSchemaGenerator.java` lines 21-99.
- Workflow projection and diagnostic conversion: `WorkflowRuntime.java` lines 216-283.
- Node transport contract: `WorkflowRun.java` lines 33-60.
- Studio cache behavior: `WorkflowStudioPage.tsx` lines 487-502.
- Focused tests: `WorkflowRuntimeTests.java` lines 139-201.

Focused verification passed with nine tests:

```cmd
.\mvnw.cmd -pl app\meshingress-server -am test "-Dtest=WorkflowRuntimeTests,WorkflowWebSocketHandlerTests" "-Dsurefire.failIfNoSpecifiedTests=false"
```

## Known boundary

The reflective typed-content generator intentionally leaves `JsonNode`, `Object`, temporal values, and recursive branches unconstrained where the class does not provide a field-level JSON contract. It does not replace the function-level `@McpFunction(outputTypes)` descriptor schema in `tools/list`; the two surfaces have different lifecycles.

`architect/audit/features/audit.schema.json`, referenced by the feature-audit guidance, is absent from this workspace, so this JSON was inspected for the documented shape rather than validated against that file.

## Origin and currency

This is a source- and test-backed capability snapshot, not a source of truth. The referenced source and tests are authoritative; later implementation changes can make this record stale.
