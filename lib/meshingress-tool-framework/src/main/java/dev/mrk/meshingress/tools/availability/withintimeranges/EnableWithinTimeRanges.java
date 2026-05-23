package dev.mrk.meshingress.tools.availability.withintimeranges;


import dev.mrk.meshingress.api.tools.annotation.McpFunctionAvailabilityCondition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@McpFunctionAvailabilityCondition(WithinTimeRangesCondition.class)
public @interface EnableWithinTimeRanges {

    String zone();

    String[] ranges();
}

