# MCP Tool Module Lifecycle — `meshingress-tool-annotations`

## Package Role

This package declares the annotation metadata used to turn Spring methods into MCP tools and functions.

## User-Visible Contribution

Annotations such as `@McpTool`, `@McpFunction`, input-schema annotations, scopes, availability policies, cache metadata, and secret markers determine what an MCP client can discover and call.

## Position in the Feature Path

```text
annotated tool class
  -> meshingress-tool-annotations
  -> meshingress-tool-framework scanner
  -> server adapter / tools/list and tools/call
```

## Entry Points

- `api.tools.annotation.McpTool` and `McpFunction`.
- `McpFunctionParam`, `McpInputSchema`, and `McpInputField`.
- `McpToolScopes`, `McpSecret`, cache annotations, and availability-policy interfaces.
- `schema.McpJsonSchemaProvider` and annotation model records.

## Configuration and Resources

`src/main/resources/tool-annotation-catalog.json` is package-owned annotation metadata. The exact runtime consumer should be rechecked when changing its schema.

## Feature Contract

```yaml
source: annotated Java class and methods
metadata:
  tool: McpTool
  functions: McpFunction
  input: McpInputSchema and McpFunctionParam
  policy: scopes, availability, caching, secrets
output: scanner-readable annotation model
```

## Dependencies

- Upstream: tool authors and route annotations reference this metadata.
- Downstream: `meshingress-tool-framework`, `meshingress-tool-api-manifest`, and the server's annotated handler adapter.

## Failure Behavior

Annotation validation failures occur in scanners or server startup/registration; this package only defines metadata and policy contracts.

## Verification

```powershell
.\mvnw.cmd -pl lib/meshingress-tool-framework -am test "-Dtest=McpToolAnnotationScannerTests"
```

## Evidence and Open Questions

Confirmed by the annotation types, availability interfaces, schema provider, and catalog resource. Configuration precedence for the catalog is unresolved here.
