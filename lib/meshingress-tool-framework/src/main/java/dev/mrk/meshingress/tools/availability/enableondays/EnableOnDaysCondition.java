package dev.mrk.meshingress.tools.availability.enableondays;

import dev.mrk.meshingress.api.tools.annotation.availability.AvailabilityCondition;
import dev.mrk.meshingress.api.tools.annotation.availability.AvailabilityValidationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public final class EnableOnDaysCondition implements AvailabilityCondition<EnableOnDays> {

    @Override
    public List<String> validate(EnableOnDays annotation, AvailabilityValidationContext context) {
        List<String> violations = new ArrayList<>();
        if (annotation.value() == null || annotation.value().length == 0) {
            violations.add(context.location() + " must declare at least one day");
        }
        return List.copyOf(violations);
    }
}

