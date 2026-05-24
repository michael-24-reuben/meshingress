package dev.mrk.meshingress.mcp.tools;

import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.McpToolHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import dev.mrk.meshingress.api.McpCallContext;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DefaultToolExecutor implements ToolExecutor {

    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
            "^(?<tool>[a-z][a-z0-9-]*(?:\\.[a-z][a-z0-9-]+)*)(/(?<method>[a-z][a-z0-9-]*))*$"
    );
    private final ToolRegistry toolRegistry;

    public DefaultToolExecutor(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public DispatchExecutionResult execute(String toolName, ObjectNode arguments, McpCallContext context) {
        Matcher matcher = TOOL_CALL_PATTERN.matcher(toolName);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid tool request");
        }

        String toolClass = matcher.group("tool");
        /*String method = matcher.group("method");*/

        McpToolDescriptor descriptor = toolRegistry.findEnabledTool(toolClass)
                .orElseThrow(() -> new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Tool is not available."));
        validateArguments(descriptor, arguments);
        McpToolHandler handler = toolRegistry.findHandler(descriptor.handlerKey())
                .orElseThrow(() -> new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Tool handler is not available."));

        try {
            return handler.call(arguments, context);
        } catch (JsonRpcException exception) {
            throw exception;
        } catch (Exception exception) {
            return DispatchExecutionResult.builder()
                    .text("Tool execution failed: " + exception.getMessage())
                    .error(true)
                    .build();
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
