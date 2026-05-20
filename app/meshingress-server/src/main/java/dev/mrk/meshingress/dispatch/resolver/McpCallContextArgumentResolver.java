package dev.mrk.meshingress.dispatch.resolver;

import dev.mrk.meshingress.api.McpCallContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Parameter;

public class McpCallContextArgumentResolver implements McpDispatchArgumentResolver {

    @Override
    public boolean supports(Parameter parameter) {
        return McpCallContext.class.isAssignableFrom(parameter.getType());
    }

    @Override
    public Object resolve(
            String method,
            JsonNode params,
            McpCallContext context,
            Parameter parameter,
            ObjectMapper objectMapper
    ) {
        return context;
    }
}
