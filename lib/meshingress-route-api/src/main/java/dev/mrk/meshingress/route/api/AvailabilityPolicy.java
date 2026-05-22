package dev.mrk.meshingress.route.api;

import java.lang.annotation.Annotation;

public interface AvailabilityPolicy<A extends Annotation> {

    AvailabilityDecision evaluate(
            A annotation,
            HTTPRequest<?, ?, ?> request,
            McpRouteExecutionContext context
    );
}
