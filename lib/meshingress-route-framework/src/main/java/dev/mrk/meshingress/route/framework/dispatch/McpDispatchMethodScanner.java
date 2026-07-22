package dev.mrk.meshingress.route.framework.dispatch;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.progress.McpProgressReporter;
import dev.mrk.meshingress.route.annotations.McpDispatchMapping;
import dev.mrk.meshingress.route.annotations.McpDispatchMethod;
import dev.mrk.meshingress.route.annotations.McpDispatchParam;
import dev.mrk.meshingress.route.annotations.McpSchema;
import dev.mrk.meshingress.route.framework.dispatch.schema.McpSchemaDescriptor;
import org.springframework.util.ClassUtils;
import tools.jackson.databind.JsonNode;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class McpDispatchMethodScanner {

    public McpDispatchRegistry scan(Collection<?> beans) {
        Map<String, McpDispatchHandlerMethod> handlers = new LinkedHashMap<>();
        for (Object bean : beans) {
            Class<?> beanClass = ClassUtils.getUserClass(bean);
            McpDispatchMapping mapping = beanClass.getAnnotation(McpDispatchMapping.class);
            if (mapping == null) {
                continue;
            }
            scanBean(bean, beanClass, mapping, handlers);
        }
        return new McpDispatchRegistry(handlers);
    }

    private void scanBean(
            Object bean,
            Class<?> beanClass,
            McpDispatchMapping mapping,
            Map<String, McpDispatchHandlerMethod> handlers
    ) {
        for (Method method : beanClass.getDeclaredMethods()) {
            McpDispatchMethod dispatchMethod = method.getAnnotation(McpDispatchMethod.class);
            if (dispatchMethod == null) {
                continue;
            }
            validateMethod(method);
            validateParameters(method);
            method.setAccessible(true);
            String methodName = compose(mapping.value(), dispatchMethod.value());
            McpDispatchHandlerMethod handler = new McpDispatchHandlerMethod(
                    methodName,
                    bean,
                    method,
                    extractSchemas(methodName, beanClass, method)
            );
            McpDispatchHandlerMethod existing = handlers.putIfAbsent(methodName, handler);
            if (existing != null) {
                throw new IllegalStateException("Duplicate MCP annotation mapping '%s' on %s and %s"
                        .formatted(methodName, existing.method().toGenericString(), method.toGenericString()));
            }
        }
    }

    private void validateMethod(Method method) {
        if (method.isBridge() || method.isSynthetic() || Modifier.isStatic(method.getModifiers())) {
            throw new IllegalStateException("MCP dispatch method must be a concrete instance method: " + method);
        }
        if (method.getReturnType().equals(Void.TYPE)) {
            throw new IllegalStateException("MCP dispatch method must not return void: " + method);
        }
    }

    private void validateParameters(Method method) {
        for (Parameter parameter : method.getParameters()) {
            if (McpCallContext.class.isAssignableFrom(parameter.getType())
                    || McpProgressReporter.class.equals(parameter.getType())) {
                continue;
            }
            McpDispatchParam dispatchParam = parameter.getAnnotation(McpDispatchParam.class);
            if (dispatchParam == null) {
                throw new IllegalStateException("MCP dispatch parameter requires @McpDispatchParam, McpCallContext, or McpProgressReporter type: "
                        + method.toGenericString());
            }
            Class<?> implementation = dispatchParam.implementation();
            if (!implementation.equals(Void.class) && !parameter.getType().isAssignableFrom(implementation)) {
                throw new IllegalStateException("MCP dispatch implementation %s is not assignable to %s on %s"
                        .formatted(implementation.getName(), parameter.getType().getName(), method.toGenericString()));
            }
            if (parameter.getType().isInterface()
                    && implementation.equals(Void.class)
                    && !JsonNode.class.isAssignableFrom(parameter.getType())) {
                throw new IllegalStateException("Interface MCP dispatch parameter requires an implementation: "
                        + method.toGenericString());
            }
        }
    }

    private List<McpSchemaDescriptor> extractSchemas(String methodName, Class<?> beanClass, Method method) {
        List<McpSchemaDescriptor> descriptors = new ArrayList<>();
        McpSchema typeSchema = beanClass.getAnnotation(McpSchema.class);
        if (typeSchema != null) {
            descriptors.add(new McpSchemaDescriptor(methodName, "type", typeSchema.value()));
        }
        McpSchema methodSchema = method.getAnnotation(McpSchema.class);
        if (methodSchema != null) {
            descriptors.add(new McpSchemaDescriptor(methodName, "method", methodSchema.value()));
        }
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            McpSchema parameterSchema = parameters[i].getAnnotation(McpSchema.class);
            if (parameterSchema != null) {
                descriptors.add(new McpSchemaDescriptor(methodName, "parameter[" + i + "]", parameterSchema.value()));
            }
        }
        return descriptors;
    }

    private String compose(String mapping, String method) {
        String left = normalize(mapping, true);
        String right = normalize(method, false);
        return left.isBlank() ? right : left + "/" + right;
    }

    private String normalize(String value, boolean allowBlank) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!allowBlank && normalized.isBlank()) {
            throw new IllegalStateException("MCP dispatch method segment must not be blank");
        }
        if (normalized.contains("//")) {
            throw new IllegalStateException("MCP dispatch path segment must not contain empty path parts: " + value);
        }
        return normalized;
    }
}
