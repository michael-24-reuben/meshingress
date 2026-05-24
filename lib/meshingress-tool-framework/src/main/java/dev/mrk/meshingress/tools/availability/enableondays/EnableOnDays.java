package dev.mrk.meshingress.tools.availability.enableondays;

import dev.mrk.meshingress.api.tools.annotation.McpFunctionAvailabilityCondition;
import dev.mrk.meshingress.api.tools.annotation.McpFunctionAvailabilityPolicy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.DayOfWeek;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@McpFunctionAvailabilityPolicy(EnableOnDaysPolicy.class)
@McpFunctionAvailabilityCondition(EnableOnDaysCondition.class)
public @interface EnableOnDays {

    DayOfWeek[] value();
}



