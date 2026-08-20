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

    public EnableWithinTimeRangesPolicy() {
        this(Clock.systemDefaultZone());
    }

    public EnableWithinTimeRangesPolicy(Clock clock) {
        this.clock = clock;
    }

    @Override
    public AvailabilityDecision evaluate(EnableWithinTimeRanges annotation, ToolAvailabilityContext context, PolicyEvaluationState state) {
        ZoneId zone = ZoneId.of(annotation.zone());
        LocalTime now = ZonedDateTime.now(clock).withZoneSameInstant(zone).toLocalTime();
        for (EnableWithinTimeRanges.TimeRange range : annotation.ranges()) {
            if (new TimeRange(LocalTime.parse(range.start()), LocalTime.parse(range.end())).contains(now)) {
                return AvailabilityDecision.allow("time range allowed");
            }
        }
        return AvailabilityDecision.deny("tool is outside configured time ranges");
    }

    record TimeRange(LocalTime start, LocalTime end) {
        boolean contains(LocalTime time) {
            if (start.equals(end)) {
                return true;
            }
            if (start.isBefore(end)) {
                return !time.isBefore(start) && !time.isAfter(end);
            }
            return !time.isBefore(start) || !time.isAfter(end);
        }
    }
}
