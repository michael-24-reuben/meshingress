package dev.mrk.meshingress.tools.availability.policy;


import dev.mrk.meshingress.route.api.AvailabilityDecision;
import dev.mrk.meshingress.route.api.AvailabilityPolicy;
import dev.mrk.meshingress.route.api.HTTPRequest;
import dev.mrk.meshingress.route.api.McpRouteExecutionContext;
import dev.mrk.meshingress.tools.availability.withintimeranges.EnableWithinTimeRanges;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class EnableWithinTimeRangesPolicy implements AvailabilityPolicy<EnableWithinTimeRanges> {

    private final Clock clock;

    public EnableWithinTimeRangesPolicy() {
        this(Clock.systemDefaultZone());
    }

    public EnableWithinTimeRangesPolicy(Clock clock) {
        this.clock = clock;
    }

    @Override
    public AvailabilityDecision evaluate(
            EnableWithinTimeRanges annotation,
            HTTPRequest<?, ?, ?> request,
            McpRouteExecutionContext context
    ) {
        ZoneId zone = ZoneId.of(annotation.zone());
        LocalTime now = ZonedDateTime.now(clock).withZoneSameInstant(zone).toLocalTime();
        for (String range : annotation.ranges()) {
            TimeRange parsedRange = TimeRange.parse(range);
            if (parsedRange.contains(now)) {
                return AvailabilityDecision.allow("time range allowed");
            }
        }
        return AvailabilityDecision.deny("route is outside configured time ranges");
    }

    record TimeRange(LocalTime start, LocalTime end) {

        static TimeRange parse(String value) {
            String[] parts = value.split("-", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Time range must use HH:mm-HH:mm format: " + value);
            }
            return new TimeRange(LocalTime.parse(parts[0]), LocalTime.parse(parts[1]));
        }

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
