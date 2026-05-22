package dev.mrk.meshingress.route.annotations.availability;

import java.lang.annotation.Annotation;
import java.util.List;

public interface AvailabilityCondition<A extends Annotation> {

    List<String> validate(A annotation, AvailabilityValidationContext context);
}

