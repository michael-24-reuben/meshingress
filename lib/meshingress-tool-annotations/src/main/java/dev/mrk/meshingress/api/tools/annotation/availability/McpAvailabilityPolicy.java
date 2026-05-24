package dev.mrk.meshingress.api.tools.annotation.availability;

import dev.mrk.meshingress.api.tools.annotation.McpAvailabilityConditionResult;
import dev.mrk.meshingress.api.tools.annotation.McpFunctionAvailabilityCondition;
import org.jetbrains.annotations.NotNull;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

public interface McpAvailabilityPolicy<A extends Annotation> {
    /**
     * A different evaluation state can trigger a different availability outcome. This enum exists to allow policies to differentiate
     * between a simple availability check (e.g. for tool listing) and an actual invocation attempt, which may have different requirements or side effects.
     */
    enum PolicyEvaluationState {
        INVOKE_TOOL,
        SCAN_AVAILABILITY
    }

    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    AvailabilityDecision evaluate(A annotation, ToolAvailabilityContext context, PolicyEvaluationState state);

    default McpAvailabilityConditionResult evaluateCondition(@NotNull A annotation, ToolAvailabilityContext context, PolicyEvaluationState state) {
        McpFunctionAvailabilityCondition condition = annotation.annotationType().getAnnotation(McpFunctionAvailabilityCondition.class);

        if (condition == null) {
            throw new IllegalArgumentException(
                    "Annotation @" + annotation.annotationType().getName() + " is not annotated with @" + McpFunctionAvailabilityCondition.class.getSimpleName()
            );
        }

        AvailabilityDecision evaluation = evaluate(annotation, context, state);

        return new McpAvailabilityConditionResult(
                annotation.annotationType().getSimpleName(),
                condition.value().getSimpleName(),
                evaluation.allowed(),
                new String[]{evaluation.reason()},
                gatherAnnotationDetails(annotation)
        );
    }

    private ObjectNode gatherAnnotationDetails(@NotNull A annotation) {
        ObjectNode details = OBJECT_MAPPER.createObjectNode();

        Arrays.stream(annotation.annotationType().getDeclaredMethods())
                .forEach(method -> putAnnotationAttribute(details, annotation, method));

        return details;
    }

    private void putAnnotationAttribute(@NotNull ObjectNode details, A annotation, @NotNull Method method) {
        try {
            Object value = method.invoke(annotation);
            details.set(method.getName(), OBJECT_MAPPER.valueToTree(value));
        } catch (ReflectiveOperationException e) {
            details.put(
                    method.getName() + "Error",
                    "Failed to retrieve annotation value: " + e.getClass().getSimpleName()
            );
        }
    }
}