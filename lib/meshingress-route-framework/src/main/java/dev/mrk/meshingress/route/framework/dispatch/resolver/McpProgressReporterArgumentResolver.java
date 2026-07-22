package dev.mrk.meshingress.route.framework.dispatch.resolver;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.progress.McpProgressReporter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Parameter;

/**
 * Supplies the invocation-scoped progress reporter.
 */
public class McpProgressReporterArgumentResolver implements McpDispatchArgumentResolver {

    @Override
    public boolean supports(Parameter parameter) {
        return McpProgressReporter.class.equals(parameter.getType());
    }

    @Override
    public Object resolve(
            String method,
            JsonNode params,
            McpCallContext context,
            Parameter parameter,
            ObjectMapper objectMapper
    ) {
        return context.progressReporter();
    }
}
