package dev.mrk.meshingress.controller.tools;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import dev.mrk.meshingress.mcp.tools.ToolExecutor;
import dev.mrk.meshingress.mcp.tools.registry.ToolRegistry;
import dev.mrk.meshingress.route.annotations.McpDispatchMapping;
import dev.mrk.meshingress.route.annotations.McpDispatchMethod;
import dev.mrk.meshingress.route.annotations.McpDispatchParam;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Component
@McpDispatchMapping("tools")
public class ToolsMcpController {
    private static final Logger log = LoggerFactory.getLogger(ToolsMcpController.class);

    private final ObjectMapper objectMapper;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final MeshingressProperties properties;

    public ToolsMcpController(ObjectMapper objectMapper, ToolRegistry toolRegistry, ToolExecutor toolExecutor, MeshingressProperties properties) {
        this.objectMapper = objectMapper;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.properties = properties;
    }

    @McpDispatchMethod("list")
    public @NonNull ObjectNode toolsList() {
        ensureRegistryEnabled();
        log.debug("MCP tools/list requested");
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode tools = objectMapper.createArrayNode();
        for (McpFunctionDescriptor function : toolRegistry.listPublicEnabledFunctions()) {
            tools.add(function.toMcpJson(objectMapper));
        }
        result.set("tools", tools);
        return result;
    }

    @McpDispatchMethod("call")
    public ObjectNode toolsCall(@McpDispatchParam("params") @NonNull JsonNode params, McpCallContext context) {
        ensureRegistryEnabled();
        if (!params.isObject()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "tools/call params must be an object");
        }
        String name = params.path("name").asString("");
        if (name.isBlank()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "tools/call params.name is required");
        }
        log.info("MCP tools/call: name={} requestId={} sessionId={}", name, context.requestId(), context.sessionId());
        JsonNode arguments = params.path("arguments");
        if (arguments.isMissingNode() || arguments.isNull()) {
            arguments = objectMapper.createObjectNode();
        }
        if (!arguments.isObject()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "tools/call params.arguments must be an object");
        }

        DispatchExecutionResult result = toolExecutor.execute(name, (ObjectNode) arguments, context);
        ObjectNode response = result.toJson(objectMapper);
        if (!properties.dispatch().includeGeneratedAt() && response.has("_meta")) {
            ObjectNode meta = (ObjectNode) response.get("_meta");
            meta.remove("generatedAt");
            if (meta.isEmpty()) {
                response.remove("_meta");
            }
        }
        return response;
    }

    private void ensureRegistryEnabled() {
        if (!properties.tools().registry().enabled()) {
            throw new JsonRpcException(JsonRpcErrorCodes.METHOD_NOT_FOUND, "Tool registry is disabled.");
        }
    }
}
