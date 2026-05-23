package dev.mrk.meshingress.tools.availability.featureflag;

import dev.mrk.meshingress.api.tools.annotation.availability.AvailabilityDecision;
import dev.mrk.meshingress.api.tools.annotation.availability.AvailabilityPolicy;
import dev.mrk.meshingress.api.tools.annotation.availability.ToolAvailabilityContext;

public class EnableWhenFeatureFlagOnPolicy implements AvailabilityPolicy<EnableWhenFeatureFlagOn> {

    public static final String FEATURE_FLAG_READER_ATTRIBUTE = "featureFlagReader";

    @Override
    public AvailabilityDecision evaluate(
            EnableWhenFeatureFlagOn annotation,
            ToolAvailabilityContext context
    ) {
        Object reader = context.attributes().get(FEATURE_FLAG_READER_ATTRIBUTE);
        if (!(reader instanceof FeatureFlagReader featureFlagReader)) {
            return AvailabilityDecision.deny("feature flag reader is unavailable");
        }
        return featureFlagReader.isEnabled(annotation.value(), context)
                ? AvailabilityDecision.allow("feature flag enabled")
                : AvailabilityDecision.deny("feature flag is disabled: " + annotation.value());
    }

    public interface FeatureFlagReader {
        boolean isEnabled(String flagName, ToolAvailabilityContext context);
    }
}
