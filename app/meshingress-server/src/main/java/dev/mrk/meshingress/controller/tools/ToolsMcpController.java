package dev.mrk.meshingress.controller.tools;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.ToolExecutionResult;
import dev.mrk.meshingress.controller.McpMethodController;
import dev.mrk.meshingress.mcp.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.JsonRpcException;
import dev.mrk.meshingress.mcp.tools.ToolExecutor;
import dev.mrk.meshingress.mcp.tools.ToolRegistry;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.Set;

@Component
//@McpDispatchMapping("tools")
public class ToolsMcpController implements McpMethodController {
    private static final Logger log = LoggerFactory.getLogger(ToolsMcpController.class);
    private static final Set<String> METHODS = Set.of("tools/list", "tools/call");

    private final ObjectMapper objectMapper;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;

    public ToolsMcpController(ObjectMapper objectMapper, ToolRegistry toolRegistry, ToolExecutor toolExecutor) {
        this.objectMapper = objectMapper;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
    }

    @Override
    public Set<String> supportedMethods() {
        return METHODS;
    }

    @Override
    public boolean supports(String method) {
        return METHODS.contains(method);
    }

    @Override
    public JsonNode dispatch(String method, JsonNode params, McpCallContext context) {
        return switch (method) {
            case "tools/list" -> toolsList();
            case "tools/call" -> toolsCall(params, context);
            default -> throw new JsonRpcException(JsonRpcErrorCodes.METHOD_NOT_FOUND, "Method not found");
        };
    }

    // @McpDispatchMethod("/list")
    private @NonNull ObjectNode toolsList() {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode tools = objectMapper.createArrayNode();
        for (McpToolDescriptor descriptor : toolRegistry.listPublicEnabledTools()) {
            tools.add(descriptor.toMcpJson(objectMapper));
        }
        result.set("tools", tools);
        return result;
    }

    // @McpDispatchMethod("/call")
    private ObjectNode toolsCall(@NonNull JsonNode params, McpCallContext context) {
        if (!params.isObject()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "tools/call params must be an object");
        }
        String name = params.path("name").asString("");
        if (name.isBlank()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "tools/call params.name is required");
        }
        JsonNode arguments = params.path("arguments");
        if (arguments.isMissingNode() || arguments.isNull()) {
            arguments = objectMapper.createObjectNode();
        }
        if (!arguments.isObject()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "tools/call params.arguments must be an object");
        }

        ToolExecutionResult result = toolExecutor.execute(name, (ObjectNode) arguments, context);
        return result.toJson(objectMapper);
    }
}
/*
* Tools can then use annotations:
```
@McpDispatchMapping("tools")
public class HelloWorldTool implements McpToolHandler
```*/
