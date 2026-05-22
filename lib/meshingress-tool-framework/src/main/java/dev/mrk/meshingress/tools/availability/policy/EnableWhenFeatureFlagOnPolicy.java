package dev.mrk.meshingress.tools.availability.policy;

import dev.mrk.meshingress.route.api.AvailabilityDecision;
import dev.mrk.meshingress.route.api.AvailabilityPolicy;
import dev.mrk.meshingress.route.api.HTTPRequest;
import dev.mrk.meshingress.route.api.McpRouteExecutionContext;
import dev.mrk.meshingress.tools.availability.featureflag.EnableWhenFeatureFlagOn;

public class EnableWhenFeatureFlagOnPolicy implements AvailabilityPolicy<EnableWhenFeatureFlagOn> {

    public static final String FEATURE_FLAG_READER_ATTRIBUTE = "featureFlagReader";

    @Override
    public AvailabilityDecision evaluate(
            EnableWhenFeatureFlagOn annotation,
            HTTPRequest<?, ?, ?> request,
            McpRouteExecutionContext context
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
        boolean isEnabled(String flagName, McpRouteExecutionContext context);
    }
}
