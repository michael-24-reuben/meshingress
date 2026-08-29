# Plan

1. Classify every `meshingress-tool-bundle` reference as either canonical behavior or the intentionally retained compatibility bridge.
2. Change canonical runtime defaults, registration defaults, API examples, tests, generated API snapshot, and contributor documentation to `meshingress-tool-distribution`.
3. Preserve the legacy module declaration and POM as an explicit compatibility bridge.
4. Package the server reactor, inspect the executable archive, and rerun the focused registration tests where the existing baseline permits.
