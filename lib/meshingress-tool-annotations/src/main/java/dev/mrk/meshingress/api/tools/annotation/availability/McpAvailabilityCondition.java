package dev.mrk.meshingress.api.tools.annotation.availability;

import java.lang.annotation.Annotation;
import java.util.List;

public interface McpAvailabilityCondition<A extends Annotation> {

    List<String> validate(A annotation, AvailabilityValidationContext context);
}

