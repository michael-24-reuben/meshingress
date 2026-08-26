# Context

Upstream revision inspected: `ritz078/transform` `ff7557939be351706f4dc6f71cc375e3bc64c225`.

The upstream repository is a Next/React browser application. Conversion behavior is split between inline page callbacks, browser Web Workers, and a few Next API handlers. The new module contains no copied React/Next pages, components, assets, styles, or UI dependencies. Its backend bridge receives a single JSON request on standard input and emits one JSON response.

The active record remains open because the enum intentionally names the full upstream route surface while the first extraction has only verified the JSON-to-Java and basic text/data transformations. Do not claim that every listed type is executable until every upstream converter has been ported and covered.
