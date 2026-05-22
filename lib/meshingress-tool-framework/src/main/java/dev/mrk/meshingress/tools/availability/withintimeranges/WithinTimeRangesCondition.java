package dev.mrk.meshingress.tools.availability.withintimeranges;

import dev.mrk.meshingress.api.tools.annotation.availability.AvailabilityCondition;
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
public final class WithinTimeRangesCondition implements AvailabilityCondition<EnableWithinTimeRanges> {

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
            for (String range : annotation.ranges()) {
                if (range == null || range.isBlank()) {
                    violations.add(location + " has a blank time range");
                    continue;
                }

                String[] parts = range.split("-", 2);
                if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                    violations.add(location + " has invalid time range: " + range);
                    continue;
                }

                try {
                    LocalTime start = LocalTime.parse(parts[0]);
                    LocalTime end = LocalTime.parse(parts[1]);
                    if (!start.isBefore(end)) {
                        violations.add(location + " has invalid time range order: " + range + " (start must be before end)");
                    }
                } catch (DateTimeParseException exception) {
                    violations.add(location + " has invalid time range: " + range);
                }
            }
        }

        return List.copyOf(violations);
    }
}

