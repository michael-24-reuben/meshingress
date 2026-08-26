# Beta Design Plan

## 1. Freeze the Definition Model

Create workflow model types for definitions, nodes, endpoint references, ports, result declarations, input bindings, routing cases, failure policy, and optional display layout. Keep executable and display data in distinct model boundaries.

## 2. Register Native Node Semantics

Introduce a native-node registry for trigger, data, control, and flow nodes. Keep tool calls as a dedicated bridge to the existing internal tool execution layer.

## 3. Compile Before Running

Compile a published definition into an execution plan. Validate endpoint `requestId` values, port names, reachability, data references, declared types, predicate compatibility, scope requirements, and incompatible merge/join topology.

## 4. Define Instance Lifecycle Before Scheduler Work

Decide the workflow-instance model before building a durable scheduler. The decision must cover creation, run identity, definition revision pinning, state persistence, retry, cancellation, timeout, resumption, terminal status, and audit retention.

## 5. Add the Runtime in Vertical Slices

Start with a manual trigger and deterministic in-process execution. Add tool calls, context bindings, boolean routing, non-boolean routing, merge, join, and node-owned error paths in separately verified slices. Do not add schedules, webhooks, loops, or subworkflows before instance behavior is defined.

## Risks and Deferred Decisions

- Workflow instances are not yet designed; durable execution cannot be finalized without that contract.
- Regular-expression matching needs safety limits or a non-backtracking engine decision.
- Tool metadata must eventually expose stable output schemas for compile-time type validation.
- Publication, draft, revision, ownership, and authorization lifecycle remain undecided.
