package dev.mrk.meshingress.tools.availability.withintimeranges;

import dev.mrk.meshingress.api.tools.annotation.availability.AvailabilityValidationContext;
import dev.mrk.meshingress.api.tools.annotation.availability.McpAvailabilityPolicy;
import dev.mrk.meshingress.api.tools.annotation.availability.ToolAvailabilityContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnableWithinTimeRangesPolicyTests {

    private final EnableWithinTimeRangesPolicy policy = new EnableWithinTimeRangesPolicy(
            Clock.fixed(Instant.parse("2026-08-09T14:00:00Z"), ZoneOffset.UTC)
    );
    private final WithinTimeRangesCondition condition = new WithinTimeRangesCondition();

    @Test
    void evaluatesStructuredTimeRangesInTheConfiguredZone() throws NoSuchMethodException {
        Method method = ScheduledTool.class.getDeclaredMethod("businessHoursReport");
        EnableWithinTimeRanges annotation = method.getAnnotation(EnableWithinTimeRanges.class);

        boolean allowed = policy.evaluate(
                annotation,
                new ToolAvailabilityContext("scheduled", "businessHoursReport", null, null, Map.of()),
                McpAvailabilityPolicy.PolicyEvaluationState.INVOKE_TOOL
        ).allowed();

        assertTrue(allowed);
        assertEquals(
                List.of(),
                condition.validate(annotation, new AvailabilityValidationContext(ScheduledTool.class, method))
        );
    }

    @Test
    void rejectsAReversedStructuredTimeRange() throws NoSuchMethodException {
        Method method = ScheduledTool.class.getDeclaredMethod("invalidHoursReport");
        EnableWithinTimeRanges annotation = method.getAnnotation(EnableWithinTimeRanges.class);

        assertEquals(
                List.of(ScheduledTool.class.getName() + "#invalidHoursReport has invalid time range order: 17:00-09:00 (start must be before end)"),
                condition.validate(annotation, new AvailabilityValidationContext(ScheduledTool.class, method))
        );
    }

    static class ScheduledTool {

        @EnableWithinTimeRanges(
                zone = "America/New_York",
                ranges = @EnableWithinTimeRanges.TimeRange(start = "09:00", end = "17:00")
        )
        void businessHoursReport() {
        }

        @EnableWithinTimeRanges(
                zone = "America/New_York",
                ranges = @EnableWithinTimeRanges.TimeRange(start = "17:00", end = "09:00")
        )
        void invalidHoursReport() {
        }
    }
}
