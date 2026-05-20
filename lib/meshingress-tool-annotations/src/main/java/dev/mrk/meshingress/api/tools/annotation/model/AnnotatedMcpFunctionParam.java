package dev.mrk.meshingress.api.tools.annotation.model;

import java.lang.reflect.Parameter;

public record AnnotatedMcpFunctionParam(
        String name,
        String description,
        boolean required,
        Class<?> parameterType,
        Class<?> bindType,
        Parameter parameter
) {
}
