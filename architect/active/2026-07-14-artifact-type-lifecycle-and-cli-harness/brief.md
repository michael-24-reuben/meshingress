# Scope Policy Lifecycle and Artifact Type Cleanup

## Goal

Make scope review a revisioned policy lifecycle while simplifying artifact types to roles the repository can actually preserve.

## Desired Outcome

- Declared and detected scopes are retained as immutable evidence.
- Review decisions explicitly enable or disable scopes, including justified manual additions, without deleting detected evidence.
- Each amendment creates a new signed publication revision and requires runtime reload before it takes effect.
- The enum contains only the five retained lifecycle roles; the legacy generated-module label migrates to `TOOL_MODULE`.

## Non-Goals

- Do not implement or activate CLI harness, availability annotation, or availability policy handling in this slice.
- Do not implement caller authentication or authorization here; that pending architect is deliberately last in the user's current todo order.
- Do not overwrite the existing dirty launcher work, runtime data, stored artifacts, or user-managed secrets.
