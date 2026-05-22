package dev.mrk.meshingress.tools.availability.featureflag;

import dev.mrk.meshingress.api.tools.annotation.McpToolAvailabilityCondition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@McpToolAvailabilityCondition(FeatureFlagOnCondition.class)
public @interface EnableWhenFeatureFlagOn {

    String value();
}

