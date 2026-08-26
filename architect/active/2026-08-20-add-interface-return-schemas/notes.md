# Notes

## 2026-08-21 unified runtime lane allocator

- Replaced the separate `firstOpenLane` running-node path and completed-only interval path with one interval allocator. Unfinished spans have an open end and therefore reserve their lane until a completion event supplies the server `completedAt` value.
- The runtime trace origin, terminal end, and live display clock now use the server lifecycle time axis. The browser clock is retained only as an offset from the last server event while a node remains running.
- The supplied serial capture produces `r-001:0, r-002:0, r-004:0`; a concurrent control assigns distinct active lanes.

## 2026-08-21 serial runtime lane model

- Replaced the interval allocator entirely. `WorkflowRuntime` currently executes one serial ready queue, so Studio now renders exactly one lane and no longer stores a client-generated lane number.
- Multiple lanes are deferred until a future concurrent runtime emits an authoritative worker/executor identity. Timing data remains server-authoritative for span position and duration.

## 2026-08-21 typed workflow-output schema refinement

- `WorkflowRun.NodeResult.outputSchema` is reserved for a generated JSON Schema representing the actual typed `StructuredContent` implementation used for the result envelope; it must not carry validator facts.
- Runtime observations use a separate extensible `diagnostics` collection with stable type identifiers such as `output.schema.violation`. The raw/anonymous `structuredContent(JsonNode)` path is deliberately excluded from schema generation.
- `StructuredContentSchemaGenerator` produces a Draft 2020-12 envelope schema with constant `kind`, `schema`, and `version` values and a reflective `data` property schema from the concrete content class. `GeneratedJsonContent` returns no schema, so its particular JSON fields are never treated as a contract.
- `WorkflowRuntime` translates invalid declared-schema validator metadata into `diagnostics` (`output.schema.violation` or `output.schema.unavailable`) and leaves a successful validation unreported. Studio now reads these diagnostics for warning badges and avoids observed-schema inference when a typed output schema is supplied.
- Focused verification passed: `mvnw.cmd -pl app\\meshingress-server -am test -Dtest=WorkflowRuntimeTests,WorkflowWebSocketHandlerTests -Dsurefire.failIfNoSpecifiedTests=false` (9 tests). Studio type checking still reports the five pre-existing baseline errors in `VariablesPanel`, `SurfaceFrame`, `SampleWorkflowTestPage`, and `tool-result-store`; no errors came from this change.
- Feature audit: `architect/audit/features/2026-08-21-feature-workflow-typed-output-schemas-v1/` records the implementation boundary, evidence, tests, and known constraint without replacing live source as authority.

## 2026-08-20 activation

- The owner requested implementation to begin.
- The record is now active; the prior root assignment/handoff remains an unrelated Studio-login objective and must not be overwritten merely by this activation.
- The final todo item records the requested Antigravity CLI / Gemini handoff for a feature audit after implementation has source and test evidence.
- `architect/audit/features/README.md` was read before the planned handoff. It requires an implemented capability plus source/test evidence; it is not a planning artifact. The expected `audit.schema.json` file was not present in this checkout when inspected, so the audit worker must report that condition rather than fabricate schema validation.

## 2026-08-20 implementation slice

- Implemented the source-compatible `outputTypes` declaration, interface/record payload-schema compilation, variant `oneOf`, full structured-content envelope publication, and explicit `structuredOutput` precedence in Studio.
- Focused scanner verification passed 10/10 tests. Studio lint/build were attempted, but existing unrelated lint failures and a TypeScript `baseUrl` deprecation configuration error prevent a clean whole-app result; no live MCP smoke check has run yet.
- Antigravity CLI / Gemini generated the requested capability snapshot in its isolated scratch workspace. Its content was independently checked, corrected for its Studio path, normalized to the local feature-audit contract, and promoted to `architect/audit/features/2026-08-20-add-interface-return-schemas-v1/`.

## 2026-08-20 local schema cache extension

- Workflow runs currently retain `results` only in the Studio `lastRun` state; `WorkflowCanvas.tsx` is a descriptor/UI consumer and never receives execution payloads. The schema cache is therefore attached to `finishRun(...)` in `WorkflowStudioPage`.
- Declared `outputSchema` values cache under `var/cache/tool-output-schemas/declared/` from the descriptor, refresh on later runs, and remain authoritative. They are not inferred from output values.
- An observed baseline writes once under `var/cache/tool-output-schemas/observed/` only for a successful tool node with `structuredOutput` true, no declared schema, and no supplied server validation failure/violations. The cache record keeps schema metadata and a deterministic schema fingerprint, not result content.
- The current workflow response has no runtime output-validation metadata because the NetworkNT execution slice remains pending. The client supports the future `nodeOutcomes[].outputSchema.{valid,violations}` gate, but current server responses cannot yet supply it.

## 2026-08-20 YouTube declared payloads

- `youtube.catalog.capabilities` and `youtube.catalog.providers` now declare typed `{ records, total }` payloads rather than an array type that did not match the existing catalog result envelope.
- The four implemented public Data API reads (`search`, `videos-get`, `channels-get`, and `playlists-get`) now declare a typed `{ resource, response }` payload and return it as `structuredContent`; `response` intentionally remains an object because the provider controls its nested JSON shape.
- The capability catalog now marks only the six callable functions as `available`: both catalog functions plus those four API-key-backed public reads. Creator, transcript, comment, subscription, and playlist-item capabilities remain planned.
- Focused verification passed: `mvnw.cmd -pl toolspace/youtube -am test` (7 module tests) and `mvnw.cmd -q -pl app/meshingress-server -am -Dtest=McpYoutubeToolMvcTests -Dsurefire.failIfNoSpecifiedTests=false test` (live `tools/list` schema discovery and blank-key failure behavior).

## 2026-08-21 declared output-schema validation

- Added NetworkNT JSON Schema Validator 3.0.6 to `app/meshingress-server`. `OutputSchemaValidator` uses the local schema registry with Draft 2020-12 and disables remote resource fetching.
- `DefaultToolExecutor` is the common boundary for MCP and workflow tool execution. After a handler returns, it validates only when the selected `McpFunctionDescriptor` has an object `outputSchema` and the result has `structuredContent`.
- This slice is warn-only: it never changes `structuredContent` and never turns an otherwise successful call into an error. It preserves existing `_meta` and adds `_meta.meshingress.outputSchema` with `policy`, `validated`, `valid`, and up to 20 bounded violation facts (`instanceLocation`, `schemaLocation`, `keyword`, `message`). An invalid/unusable declaration is reported as `validated: false` rather than failing the call.
- Focused tests passed: `OutputSchemaValidatorTests` (2 unit tests) and `McpOutputSchemaValidationMvcTests` (real `/mcp` call to `youtube.catalog.capabilities`, asserting the diagnostic path).
- Deferred by owner direction: anonymous/generated structured-content handling, policy configuration/enforcement, and workflow response projection of diagnostic metadata. General workflow `_meta` propagation remains intentionally untouched.

## 2026-08-21 rejected typed declared-output variant selection

- Removed the short-lived `McpOutputSchemaVariant` descriptor metadata and typed-class selection layer before it became part of the API. The owner identified the incorrect premise: module-defined `StructuredContent` can expose deliberately anonymous/flexible fields, such as `YoutubeApiResponseContent#getResponse()` returning `JsonNode`.
- Aggregate declared-schema validation remains the only runtime path. It preserves the flexible payload contract rather than incorrectly treating a `StructuredContent` class as a closed schema discriminator.

## 2026-08-21 generated JSON structured content

- Added `DispatchExecutionResult.Builder.generatedStructuredContent(JsonNode)` for normal tool-authored parsed JSON that has no declared payload schema. It emits the standard `kind`, `schema`, `version`, and `data` envelope using generated root-shape kinds; the supplied JSON is exactly `data`, not nested under an additional wrapper.
- Preserved deprecated `structuredContent(JsonNode)` as the raw wire-snapshot/replay escape hatch. It remains byte-for-shape compatible so cached or legacy serialized structured content is not normalized unexpectedly.
- Studio's observed-schema cache now accepts only a canonical structured-content envelope and infers its baseline from `data`. Future drift comparison therefore excludes envelope metadata.
- Focused API verification passed: `mvnw.cmd -pl lib/meshingress-tool-api -am test -Dtest=DispatchExecutionResultTests -Dsurefire.failIfNoSpecifiedTests=false` (2 tests). Studio's normal build remains blocked before source checking by the existing TypeScript 6 `baseUrl` deprecation configuration error.

## 2026-08-21 workflow result-contract refactor

- A workflow has two deliberately distinct result views: an internal compiled `results` map containing payload values for bindings, routing, and type checks; and ordered `nodeResults` records for transport and Studio persistence.
- A tool-node record contains `nodeId`, `nodePath`, `variable`, the execution outcome, and the original structured-content envelope. It therefore preserves the generated/declared kind and schema identity without accidentally feeding those envelope fields into workflow dataflow.
- `workflow.node.completed` streams the complete node record after the payload has been committed. Studio persists tool-node records when they arrive, and the terminal workflow record reconciles missed events or the HTTP fallback.
- Output-schema diagnostics travel with the tool-node record. Studio must trust a server-supplied invalid/mismatch status and must compare a locally observed baseline only when structured output is enabled, no schema was declared, and the server supplied no mismatch status.
- Focused Maven verification passed: `mvnw.cmd -pl app/meshingress-server -am test -Dtest=WorkflowRuntimeTests,WorkflowWebSocketHandlerTests,WorkflowDefinitionControllerTests,OutputSchemaValidatorTests -Dsurefire.failIfNoSpecifiedTests=false` (11 tests). It proves `data` drives downstream bindings while the streamed record retains `kind`, `schema`, `version`, and `data`.
- Studio source checking completed with no errors from this refactor. The remaining four baseline errors are the existing `VariablesPanel` nullability issue, two unused `SurfaceFrame` imports, and `CacheType` enum incompatibility with `erasableSyntaxOnly`; the standard build also remains blocked by the TypeScript 6 `baseUrl` deprecation setting.

## 2026-08-21 runtime timeline lane correction

- Replaced the state-dependent first-open-lane calculation with a deterministic interval allocator. It recomputes every lane after node start, node completion, and terminal reconciliation, using `startedAt` and `completedAt` instead of a potentially batched React `running` snapshot.
- Direct allocator verification confirmed three adjacent spans resolve to lanes `0,0,0`, while a true overlap resolves to `0,1,0` and a later adjacent span reuses lane `0`.
- The Studio type check still reports only the same four baseline errors documented above; the new allocator introduces none.

## 2026-08-21 runtime timeline lane correction reverted

- The owner requested that the interval-based lane allocator resolution be undone. Restored the prior `firstOpenLane` implementation and removed the added `runtime-timeline-lanes.ts` helper. The underlying lane behavior remains open for a separately directed correction.
## 2026-08-21 authoritative workflow lifecycle timing

- `WorkflowRuntime` now captures epoch-millisecond `startedAt` and `completedAt` boundaries around every node execution. The persisted `WorkflowRun.NodeResult` and streamed completion event retain both values.
- The WebSocket listener emits `startedAt` with `workflow.node.started`, `completedAt` with `workflow.node.completed`, and an in-order `sequence` beginning at one for every lifecycle event in a run.
- Studio uses server timing for completed node spans and deterministically recomputes their lanes by interval. A running node keeps the existing provisional lane until its completed interval arrives. A sequence discontinuity is logged for diagnosis; it does not discard results.
- Focused Maven workflow runtime/WebSocket tests pass. Studio build/typecheck remains blocked only by pre-existing TypeScript configuration and source errors recorded in the implementation handoff.
