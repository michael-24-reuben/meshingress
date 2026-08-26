# Product Requirements: Interface Return Schemas and Structured Output Policy

## Problem

Tool authors can currently publish `inputSchema`, but annotation-scanned functions do not publish their available structured-output contract even though `McpFunctionDescriptor` already has an `outputSchema` field. Consumers consequently cannot reliably understand a tool result before calling it, validate the returned `structuredContent`, or use a stable result shape for Studio editing guidance.

The platform also needs to distinguish a declared stable output contract from a Studio-learned shape. Treating every result-derived schema as authoritative would allow an inconsistent early result to poison future client guidance.

## Terminology

- **Output payload:** the value under `structuredContent.data` in the Meshingress structured-content envelope.
- **Declared output schema:** an MCP `outputSchema` generated from `@McpFunction.outputTypes`; it is the authoritative contract.
- **Observed schema:** a Studio-inferred result shape retained only as a fallback baseline.
- **Structured output:** a tool result that the author/server has designated as stable enough for schema-aware consumers.
- **Schema drift event:** a neutral fact found by comparing a later observed result with its cached observed baseline. Policy, not the comparison engine, assigns its severity.

## Authoring Contract

Add this member to `@McpFunction`:

```java
Class<?>[] outputTypes() default {};
```

Examples:

```java
@McpFunction(value = "search", outputTypes = SearchResult.class)

@McpFunction(
    value = "search",
    outputTypes = {SearchResult.class, SearchDeferredResult.class}
)
```

`outputTypes` is preferred over `returnType` because tool methods often return `DispatchExecutionResult`; this declaration describes the MCP structured output, not the Java method's direct return value. An empty array preserves today's absence of an output contract. The initial work must not require converting `DispatchExecutionResult` into a generic type.

### Multiple output variants

- One output type compiles to the matching payload schema.
- Several output types compile to a JSON Schema `oneOf` payload schema.
- Variants should be distinguishable, preferably through the existing stable structured-content `kind` discriminator; ambiguous overlapping variants must be rejected or documented as unsupported.
- The generated MCP `outputSchema` describes the complete `structuredContent` envelope, with the compiled type schema nested beneath `data`.

### Interface support

Interfaces are valid output types. The compiler must discover zero-argument abstract accessors, including inherited accessors, as object properties. It needs clear rules for JavaBean getter/property naming, nullability, requiredness, collections, maps, enums, recursive types, and unrepresentable generics.

Introduce output-specific metadata (for example, `@McpOutputField`) if title, description, requiredness, format, or constraints must be supplied on an interface accessor. Do not overload `@McpInputField`; its name and current targets are input-specific.

## Server Contract

### Tool discovery

The annotation scanner must compile declared output types into `McpFunctionDescriptor.outputSchema`. `McpFunctionDescriptor.toMcpJson(...)` must publish that field as standard MCP `outputSchema` when non-null.

The scanner also publishes an explicit boolean annotation, `structuredOutput`, when author configuration requires it. Studio resolves the signal with this exact precedence:

```ts
const structuredOutput =
  typeof tool.annotations?.structuredOutput === "boolean"
    ? tool.annotations.structuredOutput
    : tool.outputSchema != null;
```

Thus an explicit value is authoritative and output-schema presence is the fallback. Explicit `structuredOutput: true` without an output schema is allowed for Studio's observed-schema fallback, but is a configuration state that cannot receive server schema validation.

### Runtime validation

At the execution boundary, validate a produced `structuredContent` against the declared `outputSchema` when one is present. Use `com.networknt:json-schema-validator` version `3.0.6`, its Java 17+/Jackson 3 line, because this repository uses Spring Boot 4 / `tools.jackson.*` and MCP defaults to JSON Schema Draft 2020-12.

Validation policy modes:

- `off`: do not validate.
- `warn`: return the result and attach a machine-readable violation record in `_meta`.
- `enforce`: return an MCP tool error and suppress invalid structured content.

Metadata must make the source and execution state unambiguous, for example:

```json
{
  "_meta": {
    "meshingress": {
      "outputSchema": {
        "validated": true,
        "valid": false,
        "policy": "warn",
        "violations": [
          {
            "instanceLocation": "/data/items/0",
            "schemaLocation": "#/properties/data/...",
            "keyword": "required",
            "message": "Missing required property: id"
          }
        ]
      }
    }
  }
}
```

An invalid output-type declaration, invalid generated schema, or unsupported type must fail at annotation scan/registration time. Runtime warnings are only for a valid declaration that a handler's result violates.

### Validation safety and operations

- Use the locally compiled schema only; do not allow a tool-provided remote `$ref` to trigger retrieval.
- Cap validation depth and the number/size of returned diagnostics.
- Retain structured locations, keywords, and messages; do not place violations in normal result `content`.
- Keep successful and skipped validation distinguishable: `validated: true`, `validated: false`, or a documented equivalent state. Absence must not be treated as success.

## Studio Contract

### Declared-schema path

When `outputSchema` is present, the backend is authoritative:

- Studio uses the declared schema to guide result-aware editing, client understanding, and hints.
- Studio caches the declared schema using tool identity/version plus a schema fingerprint.
- Studio reads server validation state and displays server-reported violations.
- Studio must not repeat the same validation locally.
- A server-invalid result is retained as an invalid observation only; it must not replace or update a declared schema.

### Observed-schema fallback

Only when `structuredOutput` is true **and** no `outputSchema` exists:

1. The first valid, unflagged structured result is converted into an **observed schema** and cached as a baseline.
2. A later valid, unflagged result is compared with that baseline.
3. The comparator emits neutral schema-drift events.
4. A configurable policy maps each event to a severity; Studio logs, presents, or ignores the event according to that policy.

No observed schema is generated when `structuredOutput` is false. A server-flagged-invalid result, error result, partial result, or result with sensitive values not safe to cache must never become the first baseline.

### Generated JSON normalization

Tool-authored parsed JSON without a declared field-level contract may use
`DispatchExecutionResult.Builder.generatedStructuredContent(JsonNode)`. It emits the normal
Meshingress envelope with a generated root-shape kind such as
`generated.json.object` or `generated.json.array`, a Meshingress contract identifier such as
`meshingress.generated.json.object.v1`, and the supplied JSON unchanged beneath `data`.

The generated kind describes only the root JSON shape; it does not assert a stable field-level
payload schema or imply `structuredOutput: true`. The legacy `structuredContent(JsonNode)` path
remains the raw wire-snapshot/replay escape hatch. Studio must infer and later compare only the
value at `structuredContent.data`, never envelope fields such as `kind`, `schema`, or `version`.

### Configurable drift events

The comparison engine emits facts, not fixed UI outcomes. Initial event identifiers include:

```ts
type SchemaDriftEvent =
  | "fieldAdded"
  | "fieldRemoved"
  | "fieldTypeChanged"
  | "fieldBecameNullable"
  | "fieldBecameRequired"
  | "arrayItemTypeChanged"
  | "rootShapeChanged";

type DriftSeverity = "none" | "info" | "low" | "medium" | "high";
```

For example, an installation may begin with `fieldAdded: "info"` and `fieldTypeChanged: "high"`, but both are policy configuration rather than hardcoded behavior. Ordinary value changes within the same schema are not drift. A morph means a shape/type change, such as string-to-object, object-to-array, missing required field, or incompatible array-item type.

## Acceptance Criteria

- An annotated function can declare no, one, or many output payload types.
- An interface output type produces useful JSON Schema properties, including inherited accessors.
- `tools/list` includes standard `outputSchema` for declared output types.
- Generated schema validates the actual Meshingress structured-content envelope and compiled schemas are valid JSON Schema.
- A declared schema violation is reported consistently under the defined MCP metadata policy.
- Studio uses explicit `structuredOutput` before the output-schema-presence fallback.
- Studio never revalidates a server-validated declared schema.
- Studio infers/compares observed schemas only for explicit structured output without a declared schema.
- Drift severities are configurable per event and cache invalidation prevents old observed schemas from surviving an incompatible tool/schema version change.
- Tests cover scanner compilation, MCP discovery/call behavior, validator diagnostics, Studio precedence, cache/baseline lifecycle, and configured drift severities.
