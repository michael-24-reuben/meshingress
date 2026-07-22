package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.api.McpCallContext;
import tools.jackson.databind.JsonNode;

import java.util.Objects;

@FunctionalInterface
public interface McpInvocationFactory {

    McpInvocation create(JsonNode request);

    static McpInvocationFactory http(McpCallContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return ignored -> McpInvocation.http(context);
    }
}
