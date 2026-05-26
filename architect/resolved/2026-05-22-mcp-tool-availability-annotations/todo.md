# Todo

## Inventory

- [ ] Inventory current tool availability annotations and framework classes.
- [ ] Confirm package names and dependency direction.
- [ ] Identify duplicate or stale route-side imports.

## Annotation Contract

- [ ] Define final tool availability annotation names and packages.
- [ ] Decide whether annotations apply to tool classes, tool functions, or both.
- [ ] Keep validation condition metadata in tool annotation-owned APIs.

## Runtime Contract

- [ ] Define runtime evaluation boundary in `lib/meshingress-tool-framework`.
- [ ] Validate feature flag names, time ranges, zones, and day sets.
- [ ] Ensure runtime denials produce MCP tool results or JSON-RPC errors consistently.

## Verification

- [ ] Add focused unit tests for annotation scanning.
- [ ] Add focused unit tests for validation conditions.
- [ ] Add focused runtime tests for enabled and denied tool calls.
- [ ] Confirm route modules contain no tool availability code.

