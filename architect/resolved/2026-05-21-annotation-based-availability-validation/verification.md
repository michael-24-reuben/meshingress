# Verification
## Automated Checks
- `mvnw.cmd -pl lib/meshingress-route-framework -am test`
## Results
- Route framework tests passed.
- Availability validation now covers:
  - invalid time zone metadata
  - invalid time range metadata
  - blank feature flag metadata via direct condition validation
  - missing `@RouteAvailabilityCondition`
  - missing Spring bean for a declared condition
  - collective error reporting across multiple violations
## Notes
A broader `app/meshingress-server` test run still reports unrelated pre-existing auth test compilation failures outside the scope of this change.
