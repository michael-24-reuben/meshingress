package dev.mrk.meshingress.tools.availability.withintimeranges;

import dev.mrk.meshingress.api.tools.annotation.availability.McpAvailabilityCondition;
import dev.mrk.meshingress.api.tools.annotation.availability.AvailabilityValidationContext;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public final class WithinTimeRangesCondition implements McpAvailabilityCondition<EnableWithinTimeRanges> {

    @Override
    public @NonNull @Unmodifiable List<String> validate(EnableWithinTimeRanges annotation, AvailabilityValidationContext context) {
        List<String> violations = new ArrayList<>();
        String location = context.location();

        if (annotation.zone() == null || annotation.zone().isBlank()) {
            violations.add(location + " has a blank time zone");
        } else {
            try {
                ZoneId.of(annotation.zone());
            } catch (RuntimeException exception) {
                violations.add(location + " has invalid time zone: " + annotation.zone());
            }
        }

        if (annotation.ranges() == null || annotation.ranges().length == 0) {
            violations.add(location + " must define at least one time range");
        }

        if (annotation.ranges() != null) {
            for (EnableWithinTimeRanges.TimeRange range : annotation.ranges()) {
                if (range == null) {
                    violations.add(location + " has a null time range");
                    continue;
                }

                String startValue = range.start();
                String endValue = range.end();
                String formattedRange = startValue + "-" + endValue;
                if (startValue == null || startValue.isBlank() || endValue == null || endValue.isBlank()) {
                    violations.add(location + " has a blank time range endpoint: " + formattedRange);
                    continue;
                }

                try {
                    LocalTime start = LocalTime.parse(startValue);
                    LocalTime end = LocalTime.parse(endValue);
                    if (!start.isBefore(end)) {
                        violations.add(location + " has invalid time range order: " + formattedRange + " (start must be before end)");
                    }
                } catch (DateTimeParseException exception) {
                    violations.add(location + " has invalid time range: " + formattedRange);
                }
            }
        }

        return List.copyOf(violations);
    }
}

