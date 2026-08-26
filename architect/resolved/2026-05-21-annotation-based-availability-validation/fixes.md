# Fixes
## Files Changed
- `lib/meshingress-route-annotations/src/main/java/dev/mrk/meshingress/route/annotations/availability/RouteAvailabilityCondition.java`
- `lib/meshingress-route-annotations/src/main/java/dev/mrk/meshingress/route/annotations/availability/AvailabilityCondition.java`
- `lib/meshingress-route-annotations/src/main/java/dev/mrk/meshingress/route/annotations/availability/AvailabilityValidationContext.java`
- `lib/meshingress-route-annotations/src/main/java/dev/mrk/meshingress/route/annotations/availability/WithinTimeRangesCondition.java`
- `lib/meshingress-route-annotations/src/main/java/dev/mrk/meshingress/route/annotations/availability/FeatureFlagOnCondition.java`
- `lib/meshingress-route-annotations/src/main/java/dev/mrk/meshingress/route/annotations/availability/EnableOnDaysCondition.java`
- `lib/meshingress-route-annotations/src/main/java/dev/mrk/meshingress/route/annotations/availability/EnableWithinTimeRanges.java`
- `lib/meshingress-route-annotations/src/main/java/dev/mrk/meshingress/route/annotations/availability/EnableWhenFeatureFlagOn.java`
- `lib/meshingress-route-annotations/src/main/java/dev/mrk/meshingress/route/annotations/availability/EnableOnDays.java`
- `lib/meshingress-route-framework/src/main/java/dev/mrk/meshingress/route/framework/McpRouteValidator.java`
- `lib/meshingress-route-framework/src/test/java/dev/mrk/meshingress/route/framework/McpRouteFrameworkTests.java`
## Behavioral Changes
- Availability annotations now declare their validation condition class with `@RouteAvailabilityCondition`.
- `McpRouteValidator` resolves the declared condition bean from Spring and aggregates returned violations.
- Missing policy declarations, missing condition annotations, and missing Spring beans are reported as validation messages.
- Built-in availability validation rules now live in dedicated condition classes instead of in the central validator.
