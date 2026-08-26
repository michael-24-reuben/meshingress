# Plan

1. Inspect the current artifact-security dependencies and decide whether SpotBugs belongs in `lib/meshingress-artifact-security` or a narrower scanner module.
2. Spike the SpotBugs invocation API against a small fixture JAR without adding repository behavior first.
3. Add a scanner adapter only after the library invocation is deterministic.
4. Wire the adapter through existing scanner-pipeline configuration.
5. Verify with focused parser/adapter tests and one repository assessment wiring test.
