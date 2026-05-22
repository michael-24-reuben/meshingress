package dev.mrk.meshingress.tools.availability.policy;

import dev.mrk.meshingress.route.api.AvailabilityDecision;
import dev.mrk.meshingress.route.api.AvailabilityPolicy;
import dev.mrk.meshingress.route.api.HTTPRequest;
import dev.mrk.meshingress.route.api.McpRouteExecutionContext;
import dev.mrk.meshingress.tools.availability.enableondays.EnableOnDays;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.Set;

public class EnableOnDaysPolicy implements AvailabilityPolicy<EnableOnDays> {

    private final Clock clock;

    public EnableOnDaysPolicy() {
        this(Clock.systemDefaultZone());
    }

    public EnableOnDaysPolicy(Clock clock) {
        this.clock = clock;
    }

    @Override
    public AvailabilityDecision evaluate(
            EnableOnDays annotation,
            HTTPRequest<?, ?, ?> request,
            McpRouteExecutionContext context
    ) {
        Set<DayOfWeek> allowedDays = Set.of(annotation.value());
        DayOfWeek currentDay = ZonedDateTime.now(clock).getDayOfWeek();
        return allowedDays.contains(currentDay)
                ? AvailabilityDecision.allow("day allowed")
                : AvailabilityDecision.deny("route is not available on " + currentDay);
    }
}
