# Notes

## 2026-08-28 usage sweep

The supplied 21-result inventory identified 18 production and 3 test references. The bridge POM and its root module declaration are compatibility references. The remaining values are canonical defaults, registration examples, test fixtures, API snapshots, or contributor guidance and must point to `meshingress-tool-distribution`.

The full server-reactor test baseline remains blocked by the pre-existing `DispatchExecutionResultTests` wire-shape assertion. Packaging and focused tests will be used to prove this migration without changing that unrelated failure.
