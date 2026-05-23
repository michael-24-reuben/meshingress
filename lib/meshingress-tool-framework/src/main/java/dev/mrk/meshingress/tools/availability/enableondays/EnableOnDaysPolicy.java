package dev.mrk.meshingress.tools.availability.enableondays;

import dev.mrk.meshingress.api.tools.annotation.availability.AvailabilityDecision;
import dev.mrk.meshingress.api.tools.annotation.availability.AvailabilityPolicy;
import dev.mrk.meshingress.api.tools.annotation.availability.ToolAvailabilityContext;

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
    public AvailabilityDecision evaluate(EnableOnDays annotation, ToolAvailabilityContext context) {
        Set<DayOfWeek> allowedDays = Set.of(annotation.value());
        DayOfWeek currentDay = ZonedDateTime.now(clock).getDayOfWeek();
        return allowedDays.contains(currentDay)
                ? AvailabilityDecision.allow("day allowed")
                : AvailabilityDecision.deny("tool is not available on " + currentDay);
    }
}
