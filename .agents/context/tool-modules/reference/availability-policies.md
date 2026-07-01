
```java
package dev.mrk.meshingress.tools.availability.enableondays;

import dev.mrk.meshingress.api.tools.annotation.availability.McpAvailabilityCondition;
import dev.mrk.meshingress.api.tools.annotation.availability.AvailabilityValidationContext;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public final class EnableOnDaysCondition implements McpAvailabilityCondition<EnableOnDays> {
    @Override
    public List<String> validate(EnableOnDays annotation, AvailabilityValidationContext context) {
        List<String> violations = new ArrayList<>();
        if (annotation.value() == null || annotation.value().length == 0) {violations.add(context.location() + " must declare at least one day");}
        return List.copyOf(violations);
    }
}
```

[EnableOnDaysCondition.java](../../../../lib/meshingress-tool-framework/src/main/java/dev/mrk/meshingress/tools/availability/enableondays/EnableOnDaysCondition.java)

---

```java
package dev.mrk.meshingress.tools.availability.enableondays;

import dev.mrk.meshingress.api.tools.annotation.McpFunctionAvailabilityCondition;
import dev.mrk.meshingress.api.tools.annotation.McpFunctionAvailabilityPolicy;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.DayOfWeek;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@McpFunctionAvailabilityPolicy(EnableOnDaysPolicy.class)
@McpFunctionAvailabilityCondition(EnableOnDaysCondition.class)
public @interface EnableOnDays {
    DayOfWeek[] value();
}
```

[EnableOnDays.java](../../../../lib/meshingress-tool-framework/src/main/java/dev/mrk/meshingress/tools/availability/enableondays/EnableOnDays.java)

---

```java
package dev.mrk.meshingress.tools.availability.featureflag;

import dev.mrk.meshingress.api.tools.annotation.McpFunctionAvailabilityCondition;
import dev.mrk.meshingress.api.tools.annotation.McpFunctionAvailabilityPolicy;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@McpFunctionAvailabilityPolicy(EnableWhenFeatureFlagOnPolicy.class)
@McpFunctionAvailabilityCondition(FeatureFlagOnCondition.class)
public @interface EnableWhenFeatureFlagOn {
    String value();
}
```

[EnableWhenFeatureFlagOn.java](../../../../lib/meshingress-tool-framework/src/main/java/dev/mrk/meshingress/tools/availability/featureflag/EnableWhenFeatureFlagOn.java)

---

```java
package dev.mrk.meshingress.tools.availability.featureflag;

import dev.mrk.meshingress.api.tools.annotation.availability.AvailabilityDecision;
import dev.mrk.meshingress.api.tools.annotation.availability.McpAvailabilityPolicy;
import dev.mrk.meshingress.api.tools.annotation.availability.ToolAvailabilityContext;

public class EnableWhenFeatureFlagOnPolicy implements McpAvailabilityPolicy<EnableWhenFeatureFlagOn> {
    public static final String FEATURE_FLAG_READER_ATTRIBUTE = "featureFlagReader";
    @Override
    public AvailabilityDecision evaluate(EnableWhenFeatureFlagOn annotation, ToolAvailabilityContext context, PolicyEvaluationState state) {
        Object reader = context.attributes().get(FEATURE_FLAG_READER_ATTRIBUTE);
        if (!(reader instanceof FeatureFlagReader featureFlagReader)) {return AvailabilityDecision.deny("feature flag reader is unavailable");}
        return featureFlagReader.isEnabled(annotation.value(), context) ? AvailabilityDecision.allow("feature flag enabled") : AvailabilityDecision.deny("feature flag is disabled: " + annotation.value());
    }
    public interface FeatureFlagReader { boolean isEnabled(String flagName, ToolAvailabilityContext context);}
}
```

[EnableWhenFeatureFlagOnPolicy.java](../../../../lib/meshingress-tool-framework/src/main/java/dev/mrk/meshingress/tools/availability/featureflag/EnableWhenFeatureFlagOnPolicy.java)

---

```java
package dev.mrk.meshingress.tools.availability.withintimeranges;

import dev.mrk.meshingress.api.tools.annotation.McpFunctionAvailabilityCondition;
import dev.mrk.meshingress.api.tools.annotation.McpFunctionAvailabilityPolicy;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@McpFunctionAvailabilityPolicy(EnableWithinTimeRangesPolicy.class)
@McpFunctionAvailabilityCondition(WithinTimeRangesCondition.class)
public @interface EnableWithinTimeRanges {
    String zone();
    String[] ranges();
}
```

[EnableWithinTimeRanges.java](../../../../lib/meshingress-tool-framework/src/main/java/dev/mrk/meshingress/tools/availability/withintimeranges/EnableWithinTimeRanges.java)

---

```java
package dev.mrk.meshingress.tools.availability.withintimeranges;

import dev.mrk.meshingress.api.tools.annotation.availability.AvailabilityDecision;
import dev.mrk.meshingress.api.tools.annotation.availability.McpAvailabilityPolicy;
import dev.mrk.meshingress.api.tools.annotation.availability.ToolAvailabilityContext;
import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class EnableWithinTimeRangesPolicy implements McpAvailabilityPolicy<EnableWithinTimeRanges> {
    private final Clock clock;
    public EnableWithinTimeRangesPolicy() {this(Clock.systemDefaultZone());}
    public EnableWithinTimeRangesPolicy(Clock clock) {this.clock = clock;}
    @Override
    public AvailabilityDecision evaluate(EnableWithinTimeRanges annotation, ToolAvailabilityContext context, PolicyEvaluationState state) {
        ZoneId zone = ZoneId.of(annotation.zone());
        LocalTime now = ZonedDateTime.now(clock).withZoneSameInstant(zone).toLocalTime();
        for (String range : annotation.ranges()) {
            TimeRange parsedRange = TimeRange.parse(range);
            if (parsedRange.contains(now)) {return AvailabilityDecision.allow("time range allowed");}
        }
        return AvailabilityDecision.deny("tool is outside configured time ranges");
    }
    record TimeRange(LocalTime start, LocalTime end) {
        static TimeRange parse(String value) {
            String[] parts = value.split("-", 2);
            if (parts.length != 2) {throw new IllegalArgumentException("Time range must use HH:mm-HH:mm format: " + value);}
            return new TimeRange(LocalTime.parse(parts[0]), LocalTime.parse(parts[1]));
        }
        boolean contains(LocalTime time) {
            if (start.equals(end)) {return true;}
            if (start.isBefore(end)) {return !time.isBefore(start) && !time.isAfter(end);}
            return !time.isBefore(start) || !time.isAfter(end);
        }
    }
}
```

[EnableWithinTimeRangesPolicy.java](../../../../lib/meshingress-tool-framework/src/main/java/dev/mrk/meshingress/tools/availability/withintimeranges/EnableWithinTimeRangesPolicy.java)
