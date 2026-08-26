# Context

## Conversation Reference

This entry packages the owner-approved architecture from the Codex conversation:

```txt
title="Add interface return schemas", sessionId="01a01d8a-e8ec-7383-8314-f7fa8a5ad320"
```

## Current Repository Evidence

- `lib/meshingress-tool-annotations/.../McpFunction.java` currently provides name, title, description, visibility, and enabled metadata; it has no output-type member.
- `lib/meshingress-tool-framework/.../McpToolAnnotationScanner.java` already compiles input schemas through `inputSchemaFor(...)`, `objectSchemaFor(...)`, and `schemaForType(...)`. It currently supplies `null` as the output schema when it creates `McpFunctionDescriptor`.
- `lib/meshingress-tool-api/.../function/McpFunctionDescriptor.java` already owns `inputSchema` and `outputSchema`, but its MCP JSON formatter currently publishes only `inputSchema`.
- `app/meshingress-server/.../controller/tools/ToolsMcpController.java` serializes each public enabled function through `McpFunctionDescriptor.toMcpJson(...)`; publishing there is therefore descriptor-driven.
- `DispatchExecutionResult` holds typed or raw `structuredContent`. Typed content is serialized through `StructuredContentMapper` into the stable `{ kind, schema, version, data }` envelope.
- `@McpInputField` is intentionally limited to fields and record components. Output interface accessor metadata needs an output-specific design rather than a semantic/target expansion of that input annotation.

## Relevant Existing Architect Records

- `architect/active/2026-08-03-mcp-input-constraint-schema/` established the current annotation-to-JSON-Schema compiler and Studio schema-reading work.
- `architect/resolved/2026-05-17-mcp-http-tool-registry/` already modeled `outputSchema` and identified output validation as a desired registry behavior.
- `architect/resolved/2026-07-26-workflow-runtime-beta/` is related because stable result contracts inform workflow result use.

## Library Decision

Use NetworkNT JSON Schema Validator:

```xml
<dependency>
    <groupId>com.networknt</groupId>
    <artifactId>json-schema-validator</artifactId>
    <version>3.0.6</version>
</dependency>
```

Its 3.x line is compatible with Java 17+ and Jackson 3, supports Draft 2020-12, and can return detailed machine-readable output including instance locations, schema locations, keywords, and messages. Use local compiled schemas only; the library assumes loaded schemas are trusted.

## Deliberate Deferrals

- `DispatchExecutionResult<RT>` and `StructuredContent<RT>` would be a separate internal type-safety refactor. Generics alone are erased and are not the source of public MCP discovery metadata.
- Parsed JSON can now be normalized through `DispatchExecutionResult.Builder.generatedStructuredContent(JsonNode)`. It emits a generated root-shape envelope but does not create a declared payload schema. The legacy `structuredContent(JsonNode)` method remains the raw wire-snapshot/replay path and must preserve its supplied shape.
- Runtime validation is warn-only for this slice. It preserves a successful result and its content, then emits bounded facts at `_meta.meshingress.outputSchema`; configurable `off`/`warn`/`enforce` behavior is a later policy decision.
- Input-side validation against all compiled input schemas is related but not included.

## Rejected Typed-Variant Selection

- Do not select a narrower `outputTypes` schema from the returned `StructuredContent` class. A module-owned content class can intentionally include flexible fields such as `JsonNode response`, so class identity does not establish a closed payload schema.
- Keep runtime validation against the aggregate declared `outputSchema`. Generated JSON has no field-level declared schema; Studio observes and compares only its `data` payload when the tool explicitly enables structured output.
- The exact persisted cache store and settings UI are implementation decisions, but their data model must preserve the declared/observed distinction and configurable policy mapping.
