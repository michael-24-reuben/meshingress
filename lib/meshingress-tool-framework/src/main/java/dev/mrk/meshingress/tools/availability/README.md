# Availability annotations

These annotations make an annotated MCP function dynamically available or unavailable. Put them on the same method as `@McpFunction`; use `@McpConfigureMapping` to choose how multiple availability annotations combine.

The framework evaluates them while scanning availability and again immediately before invoking the function. A denied function is not available for the corresponding operation. Static `enabled = false` still overrides every dynamic condition.

## Combining conditions

`@McpConfigureMapping` defaults to `McpAvailabilityMode.ALL`: every attached availability annotation must allow the function. Use `ANY` only when one of several alternatives should be sufficient.

```java
import dev.mrk.meshingress.api.tools.annotation.McpAvailabilityMode;
import dev.mrk.meshingress.api.tools.annotation.McpConfigureMapping;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.tools.availability.enableondays.EnableOnDays;

import java.time.DayOfWeek;

@EnableOnDays({DayOfWeek.SATURDAY, DayOfWeek.SUNDAY})
@McpConfigureMapping(availabilityMode = McpAvailabilityMode.ALL)
@McpFunction(value = "weekend_report", description = "Return the weekend report.")
public DispatchExecutionResult weekendReport(McpCallContext context) {
    // ...
}
```

With `ALL`, the function must satisfy both the day condition above and any other availability condition on the method. With `ANY`, it needs only one allowing condition. A method with no availability annotations has no dynamic availability restriction.

## `@EnableOnDays`

Use `@EnableOnDays` to allow a function on one or more `java.time.DayOfWeek` values. Evaluation uses the server's default time zone.

```java
import dev.mrk.meshingress.tools.availability.enableondays.EnableOnDays;

@EnableOnDays({DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY})
@McpFunction(value = "weekday_summary", description = "Return the weekday summary.")
public DispatchExecutionResult weekdaySummary(McpCallContext context) {
    // ...
}
```

At least one day is required.

## `@EnableWithinTimeRanges`

Use `@EnableWithinTimeRanges` to allow a function during one or more clock ranges in a named IANA/Java zone such as `"America/New_York"` or `"UTC"`.

```java
import dev.mrk.meshingress.tools.availability.withintimeranges.EnableWithinTimeRanges;

@EnableWithinTimeRanges(
        zone = "America/New_York",
        ranges = {
                @EnableWithinTimeRanges.TimeRange(start = "09:00", end = "12:00"),
                @EnableWithinTimeRanges.TimeRange(start = "13:00", end = "17:00")
        }
)
@McpFunction(value = "business_hours_report", description = "Return a business-hours report.")
public DispatchExecutionResult businessHoursReport(McpCallContext context) {
    // ...
}
```

- Each `TimeRange` has `start` and `end` strings in `HH:mm` form.
- Both endpoints are inclusive at evaluation time.
- Supply at least one range and a valid `ZoneId`.
- The current validator requires the start to be strictly before the end, so do not use an overnight range such as `"22:00-02:00"`; split it into `"22:00-23:59"` and `"00:00-02:00"` instead.

## `@EnableWhenFeatureFlagOn`

Use `@EnableWhenFeatureFlagOn` when the host supplies a feature-flag reader in the function's `ToolAvailabilityContext`.

```java
import dev.mrk.meshingress.tools.availability.featureflag.EnableWhenFeatureFlagOn;

@EnableWhenFeatureFlagOn("reports.v2")
@McpFunction(value = "report_v2", description = "Return the v2 report.")
public DispatchExecutionResult reportV2(McpCallContext context) {
    // ...
}
```

The host must place an implementation of `EnableWhenFeatureFlagOnPolicy.FeatureFlagReader` in the availability context under `EnableWhenFeatureFlagOnPolicy.FEATURE_FLAG_READER_ATTRIBUTE` (`"featureFlagReader"`). Its `isEnabled(flagName, context)` result controls availability. If that attribute is absent or has the wrong type, this annotation denies the function with `feature flag reader is unavailable`.

## Choosing the right annotation

Use `@EnableOnDays` for a weekly schedule, `@EnableWithinTimeRanges` for a local-time window, and `@EnableWhenFeatureFlagOn` for a host-controlled rollout. They may be used together; add `@McpConfigureMapping(availabilityMode = McpAvailabilityMode.ANY)` only when that is intentional.
