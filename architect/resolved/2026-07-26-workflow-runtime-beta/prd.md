# Product Requirements: Workflow Runtime Beta

## Product Boundary

An executable workflow is a published definition revision. Studio layout is optional companion metadata keyed by node `requestId`; it is not supplied to the compiler or runtime.

## Definition Contract

Each node has a stable `requestId`, a native `type` or `tool.call` configuration, typed `result`, explicit input bindings, and execution requirements. Human-facing `title` and node layout are display-only.

Each edge references `{ requestId, port }` at both ends. A definition-level `requestId` is distinct from an invocation's runtime or HTTP request ID. Runtime tracing derives a unique identifier from workflow run, node request ID, and attempt.

## Result and Routing Contract

- `result.as` binds the completed JSON value into execution context.
- `result.type` declares `boolean`, `string`, `number`, `object`, `array`, or `null`.
- Boolean results expose `true` and `false` ports and no default port.
- All other result types expose ordered cases plus a `default` port.
- Each case predicate must resolve to a boolean. Cases use first-match-wins semantics.
- String cases initially support equality and regular-expression matching.
- Number cases initially support `eq`, `neq`, `lt`, `lte`, `gt`, and `gte`.
- Object and array routing require an explicit JSON Pointer subject when a nested value is tested. Missing subjects and type mismatches are errors, not implicit coercions.
- Null behavior must be explicit: a `null` port or defined default behavior is required before implementation.

## Native Node Contract

Initial native nodes are:

- `trigger.manual`, `trigger.schedule`, and `trigger.webhook`
- `tool.call`
- `transform.compose`, `transform.assign`, and `data.select`
- `expression.true`
- `merge` and `join` as separate node types
- `delay`, `loop.for-each`, `flow.stop`, `flow.return`, and `workflow.call`

Routine result routing replaces a generic switch node. A future `control.switch` is permitted only if a concrete case requires routing an already-existing context value without a producing node.

## Merge and Join

- `merge` continues from the first accepted inbound branch. It is appropriate after mutually exclusive outcome ports.
- `join` waits for every declared required inbound branch and exposes named upstream results. It is appropriate after intentional parallel work.
- A compiler must reject or warn about joins that require mutually exclusive branches, because they would deadlock.

## Failure Handling

Executable nodes own retry and failure policy. A node may emit a standard error result through an `error` port after configured retries. The error edge is a normal, visible workflow branch; no standalone `try/catch` wrapper node is required.

## Security and Tool Invocation

The runtime must invoke tool nodes through the internal server execution layer, not through HTTP calls back into Meshingress. Declared node scopes are requirements for validation, review, and audit; they never grant permissions by themselves. Runtime policy must intersect workflow approval, tool descriptor requirements, and caller authority.

## Beta Acceptance Criteria

- The definition schema represents the sample manual -> tool -> typed-route -> transform -> tool flow without canvas properties.
- The compiler can validate stable endpoint references, port existence, input references, type compatibility, result routes, and unsafe join topology.
- The design preserves named output values for later node argument bindings.
- Open questions are recorded before runtime implementation begins.
