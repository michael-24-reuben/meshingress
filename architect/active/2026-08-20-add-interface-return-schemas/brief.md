# Add Interface Return Schemas

## Goal

Let annotation-based MCP tools declare zero, one, or several possible structured-output payload types. Compile those types, including interfaces, into the standard MCP `outputSchema` published by `tools/list`; validate declared output on the server; and give Workflow Studio a safe, configurable fallback for observing output consistency when no declared schema exists.

## Scope

- Add an `outputTypes` declaration to `@McpFunction`.
- Compile output types into `McpFunctionDescriptor.outputSchema` and publish `outputSchema` in `tools/list`.
- Generate schema properties from supported interface accessors as well as existing record/field-based DTOs.
- Validate server-produced `structuredContent` against declared output schemas and report policy violations in MCP result metadata.
- Add the authoritative `structuredOutput` signal and its Studio precedence rule.
- Use declared schemas for Studio output guidance and cache them separately from observed schemas.
- When `structuredOutput` is true but no declared output schema is available, infer and compare observed result schemas on later runs using configurable drift-event severities.

## Explicit Non-goals

- Do not make `DispatchExecutionResult<RT>` part of this initial change.
- Do not infer a schema from results when `structuredOutput` is false.
- Do not have Studio revalidate a result for which the server already validated a declared `outputSchema`.
- Do not permit remote or untrusted schema retrieval through `$ref`.
- Do not let an observed result schema replace a declared tool contract.
- Do not implement generic cross-tool result transformation, workflow rewriting, or request-input validation as part of this entry.

## Status

Planning only. No implementation or verification has been performed by this record.
