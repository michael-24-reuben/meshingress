# Plan

1. Add focused, type-specific companion annotations in `meshingress-tool-annotations`.
2. Compile them through a shared constraint compiler into standard JSON Schema keywords.
3. Refactor the annotation scanner to preserve generic collection element types.
4. Apply the initial numeric and array constraints to Toonverse search arguments.
5. Centralize Studio parameter-type resolution while retaining its current behavior and intentionally ignoring constraint-specific UI work.
6. Verify backend schema output and the Studio build/lint.
7. **Current unresolved slice:** synchronize compiled constraint keywords and `x-mcp-*` metadata into the matching Studio parameter controls, visible constraint hints, and client-side editing behavior. The opt-in integer and decimal slider control is implemented; the remaining type-specific controls and hints are still open.

## Deferred Work

- Runtime request validation against every compiled constraint.
- Cross-field rules such as `minRating <= maxRating`.
- Backend fallback/default resolution semantics and source precedence.
