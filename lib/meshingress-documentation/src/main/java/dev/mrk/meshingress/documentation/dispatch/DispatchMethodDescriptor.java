package dev.mrk.meshingress.documentation.dispatch;

public record DispatchMethodDescriptor(
        String method,
        String className,
        String javaMethod,
        Class<?> paramsType,
        Class<?> returnType
) {
}
