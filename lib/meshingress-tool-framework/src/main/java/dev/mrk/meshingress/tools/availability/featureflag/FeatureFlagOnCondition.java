package dev.mrk.meshingress.tools.availability.featureflag;

import dev.mrk.meshingress.api.tools.annotation.availability.AvailabilityCondition;
import dev.mrk.meshingress.api.tools.annotation.availability.AvailabilityValidationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class FeatureFlagOnCondition implements AvailabilityCondition<EnableWhenFeatureFlagOn> {

    @Override
    public List<String> validate(EnableWhenFeatureFlagOn annotation, AvailabilityValidationContext context) {
        if (annotation.value() == null || annotation.value().isBlank()) {
            return List.of(context.location() + " has a blank feature flag name");
        }
        return List.of();
    }
}

