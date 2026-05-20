package dev.mrk.meshingress.mcp.tools;

import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.api.tools.ToolExecutionResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import dev.mrk.meshingress.mcp.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.JsonRpcException;
import dev.mrk.meshingress.api.McpCallContext;
import org.springframework.stereotype.Service;

@Service
public class DefaultToolExecutor implements ToolExecutor {

    private final ObjectMapper objectMapper;
    private final ToolRegistry toolRegistry;

    public DefaultToolExecutor(ObjectMapper objectMapper, ToolRegistry toolRegistry) {
        this.objectMapper = objectMapper;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public ToolExecutionResult execute(String toolName, ObjectNode arguments, McpCallContext context) {
        McpToolDescriptor descriptor = toolRegistry.findEnabledTool(toolName)
                .orElseThrow(() -> new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Tool is not available."));
        validateArguments(descriptor, arguments);
        McpToolHandler handler = toolRegistry.findHandler(descriptor.handlerKey())
                .orElseThrow(() -> new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Tool handler is not available."));

        try {
            return handler.call(arguments, context);
        } catch (JsonRpcException exception) {
            throw exception;
        } catch (Exception exception) {
            return ToolExecutionResult.error(objectMapper, "Tool execution failed: " + exception.getMessage());
        }
    }

    private void validateArguments(McpToolDescriptor descriptor, ObjectNode arguments) {
        if (descriptor.inputSchema() == null || !descriptor.inputSchema().isObject()) {
            return;
        }
        if (descriptor.inputSchema().path("additionalProperties").isBoolean()
                && !descriptor.inputSchema().path("additionalProperties").asBoolean()) {
            arguments.properties().forEach(field -> {
                String fieldName = field.getKey();
                if (!descriptor.inputSchema().path("properties").has(fieldName)) {
                    throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Unsupported tool argument: " + fieldName);
                }
            });
        }
        for (JsonNode required : descriptor.inputSchema().path("required")) {
            if (!arguments.has(required.asString())) {
                throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Missing required tool argument: " + required.asString());
            }
        }
    }
}
