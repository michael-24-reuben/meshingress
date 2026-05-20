package dev.mrk.meshingress.dispatch.resolver;

import dev.mrk.meshingress.api.McpCallContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Parameter;

public interface McpDispatchArgumentResolver {

    boolean supports(Parameter parameter);

    Object resolve(
            String method,
            JsonNode params,
            McpCallContext context,
            Parameter parameter,
            ObjectMapper objectMapper
    );
}
