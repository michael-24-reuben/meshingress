package dev.mrk.meshingress.documentation.dispatch;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.progress.McpProgressReporter;
import dev.mrk.meshingress.documentation.MeshingressDocumentationProperties;
import dev.mrk.meshingress.route.annotations.McpDispatchMapping;
import dev.mrk.meshingress.route.annotations.McpDispatchMethod;
import dev.mrk.meshingress.route.annotations.McpDispatchParam;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DispatchAnnotationScanner {

    private final MeshingressDocumentationProperties properties;

    public DispatchAnnotationScanner(MeshingressDocumentationProperties properties) {
        this.properties = properties;
    }

    public List<DispatchMethodDescriptor> scan() {
        ClassPathScanningCandidateComponentProvider provider =
                new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(McpDispatchMapping.class));

        Map<String, DispatchMethodDescriptor> methods = new LinkedHashMap<>();
        for (String basePackage : properties.dispatch().scanBasePackages()) {
            for (BeanDefinition beanDefinition : provider.findCandidateComponents(basePackage)) {
                Class<?> candidate = resolveClass(beanDefinition.getBeanClassName());
                if (candidate == null) {
                    continue;
                }
                scanCandidate(candidate, methods);
            }
        }
        return methods.values().stream()
                .sorted(Comparator.comparing(DispatchMethodDescriptor::method))
                .toList();
    }

    private void scanCandidate(Class<?> candidate, Map<String, DispatchMethodDescriptor> methods) {
        McpDispatchMapping mapping = candidate.getAnnotation(McpDispatchMapping.class);
        if (mapping == null) {
            return;
        }
        for (Method method : candidate.getDeclaredMethods()) {
            McpDispatchMethod dispatchMethod = method.getAnnotation(McpDispatchMethod.class);
            if (dispatchMethod == null) {
                continue;
            }
            String composedMethod = compose(mapping.value(), dispatchMethod.value());
            methods.putIfAbsent(
                    composedMethod,
                    new DispatchMethodDescriptor(
                            composedMethod,
                            candidate.getName(),
                            method.getName(),
                            resolveParamsType(method),
                            method.getReturnType()
                    )
            );
        }
    }

    private Class<?> resolveClass(String className) {
        if (className == null || className.isBlank()) {
            return null;
        }
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }

    private Class<?> resolveParamsType(Method method) {
        List<Parameter> mapped = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            if (McpCallContext.class.isAssignableFrom(parameter.getType())
                    || McpProgressReporter.class.equals(parameter.getType())) {
                continue;
            }
            McpDispatchParam dispatchParam = parameter.getAnnotation(McpDispatchParam.class);
            if (dispatchParam == null) {
                continue;
            }
            if ("params".equals(dispatchParam.value())) {
                return parameter.getType();
            }
            mapped.add(parameter);
        }
        if (!mapped.isEmpty()) {
            return mapped.getFirst().getType();
        }
        return JsonNode.class;
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
