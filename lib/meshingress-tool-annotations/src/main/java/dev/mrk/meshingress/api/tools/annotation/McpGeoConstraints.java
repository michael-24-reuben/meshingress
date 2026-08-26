package dev.mrk.meshingress.api.tools.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares inclusive latitude and longitude bounds for a geographic object. */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface McpGeoConstraints {

    double minimumLatitude() default Double.NaN;

    double maximumLatitude() default Double.NaN;

    double minimumLongitude() default Double.NaN;

    double maximumLongitude() default Double.NaN;
}
