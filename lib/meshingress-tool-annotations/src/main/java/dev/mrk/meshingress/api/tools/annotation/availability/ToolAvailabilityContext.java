package dev.mrk.meshingress.api.tools.annotation.availability;

import dev.mrk.meshingress.api.McpCallContext;
import tools.jackson.databind.JsonNode;

import java.util.Map;

public record ToolAvailabilityContext(
        String toolName,
        String functionName,
        JsonNode arguments,
        McpCallContext callContext,
        Map<String, Object> attributes
) {

    public ToolAvailabilityContext {
        toolName = toolName == null ? "" : toolName;
        functionName = functionName == null ? "" : functionName;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public ToolAvailabilityContext withAttribute(String name, Object value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tool availability attribute name must not be blank");
        }
        Map<String, Object> nextAttributes = new java.util.LinkedHashMap<>(attributes);
        nextAttributes.put(name, value);
        return new ToolAvailabilityContext(toolName, functionName, arguments, callContext, nextAttributes);
    }
}
