# Verification

## Automated Checks

- `.\mvnw.cmd -pl app/meshingress-server -am "-Dtest=McpToolRegistrationPhaseApiSampleTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Result: passed.
  - Coverage: 5 tests in `McpToolRegistrationPhaseApiSampleTests`, including the direct experimental local-JAR checksum mismatch case.

## Failed/Corrected Check

- The first focused run failed because the new checksum mismatch test used `helloworld.text`, which hit `TOOL_REGISTRATION_BUNDLE_OVERRIDE_DENIED` before checksum verification.
- The test was corrected to use `checksum.mismatch`, then the same focused Maven command passed.

## Remaining Risks

- Existing sample methods print some MCP responses instead of asserting every positive registration outcome. That is pre-existing sample-test behavior and was not expanded in this hardening slice.
- Direct registration intentionally does not gain repository publication-record requirements in this objective.
