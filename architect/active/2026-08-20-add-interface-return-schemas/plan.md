# Plan

## 1. Lock the public contracts

1. Add `Class<?>[] outputTypes() default {}` to `@McpFunction`.
2. Specify `structuredOutput` ownership, serialization location, explicit-value precedence, and compatibility behavior.
3. Specify generated envelope schema shape and `oneOf` behavior for multiple variants.
4. Define unsupported/ambiguous type handling before changing a scanner.

## 2. Extend schema compilation

1. Extract or generalize safe reusable type-schema compilation from the input path.
2. Add interface accessor discovery and output-specific property metadata.
3. Preserve collections, maps, enums, nested DTOs, nullability, and recursive-type guards.
4. Compile output payload schemas then wrap them for the Meshingress `structuredContent` envelope.
5. Validate generated schemas against the JSON Schema meta-schema during focused tests.

## 3. Publish and validate server output

1. Feed the compiled schema into `McpFunctionDescriptor.outputSchema`.
2. Serialize standard MCP `outputSchema` from `McpFunctionDescriptor.toMcpJson(...)`.
3. Add the NetworkNT Jackson 3 validator to the smallest appropriate server/runtime module.
4. Validate only declared schemas at the tool execution boundary.
5. Implement bounded warn-only `_meta` diagnostics without changing the tool result.
6. Follow with configurable `off`, `warn`, and `enforce` policy behavior when enforcement semantics are approved.
7. Reject invalid declarations at scan/registration time and prevent remote schema resolution.

## 4. Add Studio declared-schema behavior

1. Resolve `structuredOutput` with explicit boolean precedence and schema-presence fallback.
2. Treat declared schemas as authoritative for hints, editing guidance, and cache identity.
3. Render server-reported output-schema validation state and violations without running a duplicate client validator.
4. Invalidate declared-schema cache entries when tool identity, tool version, or schema fingerprint changes.

## 5. Add Studio observed-schema fallback

1. Gate schema inference on `structuredOutput === true && outputSchema == null`.
2. Normalize generated JSON through the standard structured-content envelope, while retaining the raw JSON branch only for wire-snapshot replay.
3. Store first valid unflagged `structuredContent.data` value as an observed baseline, subject to sensitivity/cache policy.
4. Compare only later eligible `structuredContent.data` values against that baseline; envelope metadata is not payload drift.
5. Emit neutral drift events, resolve policy severity from configuration, and make source/severity visible in Studio.
6. Ensure invalid, error, partial, or server-flagged outputs never establish or replace a baseline.

## 6. Verify

1. Unit-test annotation compilation for one, many, and interface output types.
2. Test `tools/list` output-schema publication and `tools/call` warn/enforce behavior.
3. Test validator diagnostic translation, diagnostics caps, and no-remote-reference policy.
4. Add Studio tests for precedence, non-duplication of server validation, observed-baseline lifecycle, event generation, and severity configuration.
5. Run focused Maven tests plus Studio lint/build; perform a live `tools/list` and repeated-call smoke check.

## Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Output schema describes a payload while runtime validation sees an envelope | Generate and test the envelope schema explicitly; never validate only a payload by accident. |
| Interface property discovery disagrees with serialization | Base property conventions on the actual JSON serialization contract and cover inherited accessors. |
| `oneOf` variants overlap | Require a discriminator or reject ambiguity during compilation. |
| Validation diagnostics become expensive or huge | Bound depth/diagnostic count and use policy-specific output formats. |
| Observed schemas become stale or poisonous | Keep them separate from declared schemas; key/invalidate them correctly and refuse invalid baselines. |
| Studio warnings become noisy | Emit facts only and let configurable per-event severity determine UI/log behavior. |
| Validation changes successful calls | Keep the current policy warn-only; preserve `structuredContent` exactly and place diagnostics under `_meta`. |
