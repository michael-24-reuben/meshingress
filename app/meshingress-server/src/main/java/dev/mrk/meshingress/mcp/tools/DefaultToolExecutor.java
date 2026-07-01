package dev.mrk.meshingress.mcp.tools;

import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.tools.registry.ToolRegistry;
import dev.mrk.meshingress.scopes.McpToolScope;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import dev.mrk.meshingress.api.McpCallContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class DefaultToolExecutor implements ToolExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultToolExecutor.class);

    private final ToolRegistry toolRegistry;
    private final MeshingressProperties properties;
    private final ObjectMapper objectMapper;

    public DefaultToolExecutor(ToolRegistry toolRegistry, MeshingressProperties properties, ObjectMapper objectMapper) {
        this.toolRegistry = toolRegistry;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public DispatchExecutionResult execute(String functionName, ObjectNode arguments, McpCallContext context) {
        McpFunctionDescriptor function = toolRegistry.findEnabledFunction(functionName)
                .orElseThrow(() -> new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Tool function is not available."));
        McpToolDescriptor tool = toolRegistry.findOwningTool(function.name())
                .orElseThrow(() -> new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Tool descriptor is not available."));
        validateArguments(function, arguments);
        enforceScopePolicy(function);
        McpToolHandler handler = toolRegistry.findHandler(function.handlerKey())
                .orElseThrow(() -> new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Tool function handler is not available."));

        logCall(functionName, arguments, context);
        try {
            DispatchExecutionResult result = handler.call(arguments, context);
            enrichToolIdentity(result, tool, function);
            logResult(functionName, result, context);
            return result;
        } catch (JsonRpcException exception) {
            throw exception;
        } catch (Exception exception) {
            LOGGER.warn("MCP tool execution failed: name={} requestId={} sessionId={}", functionName, context.requestId(), context.sessionId(), exception);
            DispatchExecutionResult result = DispatchExecutionResult.builder()
                    .text("Tool execution failed: " + exception.getMessage())
                    .error(true)
                    .build();
            enrichToolIdentity(result, tool, function);
            return result;
        }
    }

    private void enrichToolIdentity(DispatchExecutionResult result, McpToolDescriptor tool, McpFunctionDescriptor function) {
        JsonNode currentMeta = result.toJson(objectMapper).path("_meta");
        ObjectNode meta = currentMeta.isObject() ? (ObjectNode) currentMeta.deepCopy() : objectMapper.createObjectNode();
        ObjectNode toolNode = objectMapper.createObjectNode();
        toolNode.put("id", tool.name());
        toolNode.put("name", function.name());
        toolNode.put("title", tool.title() == null ? "" : tool.title());
        toolNode.put("function", localFunctionName(tool, function));
        toolNode.put("functionTitle", function.title() == null ? "" : function.title());
        toolNode.put("version", tool.version());
        meta.set("tool", toolNode);
        result.setMeta(meta);
    }

    private String localFunctionName(McpToolDescriptor tool, McpFunctionDescriptor function) {
        String prefix = tool.name() + ".";
        if (function.name().startsWith(prefix)) {
            return function.name().substring(prefix.length());
        }
        return function.name();
    }

    private void validateArguments(McpFunctionDescriptor function, ObjectNode arguments) {
        if (function.inputSchema() == null || !function.inputSchema().isObject()) {
            return;
        }
        if (function.inputSchema().path("additionalProperties").isBoolean()
                && !function.inputSchema().path("additionalProperties").asBoolean()) {
            arguments.properties().forEach(field -> {
                String fieldName = field.getKey();
                if (!function.inputSchema().path("properties").has(fieldName)) {
                    throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Unsupported tool argument: " + fieldName);
                }
            });
        }
        for (JsonNode required : function.inputSchema().path("required")) {
            if (!arguments.has(required.asString())) {
                throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Missing required tool argument: " + required.asString());
            }
        }
    }

    private void enforceScopePolicy(McpFunctionDescriptor function) {
        if (!properties.security().enabled()) {
            return;
        }
        for (String scopeName : scopeNames(function)) {
            McpToolScope scope;
            try {
                scope = McpToolScope.valueOf(scopeName);
            } catch (IllegalArgumentException exception) {
                if (properties.security().denyUnknownScopes()) {
                    throw new JsonRpcException(JsonRpcErrorCodes.FORBIDDEN, "Tool function declares an unknown scope: " + scopeName);
                }
                continue;
            }

            if (scope == McpToolScope.SHELL_EXECUTE && !properties.scopes().allowShellExecute()) {
                throw new JsonRpcException(JsonRpcErrorCodes.FORBIDDEN, "Tool function requires disabled scope: SHELL_EXECUTE");
            }
            if (scope == McpToolScope.FILES_DELETE && !properties.scopes().allowFilesDelete()) {
                throw new JsonRpcException(JsonRpcErrorCodes.FORBIDDEN, "Tool function requires disabled scope: FILES_DELETE");
            }
            if (scope == McpToolScope.NETWORK_INBOUND && !properties.scopes().allowNetworkInbound()) {
                throw new JsonRpcException(JsonRpcErrorCodes.FORBIDDEN, "Tool function requires disabled scope: NETWORK_INBOUND");
            }
        }
    }

    private java.util.List<String> scopeNames(McpFunctionDescriptor function) {
        JsonNode scopes = function.annotations() == null ? null : function.annotations().path("scopes");
        if (scopes == null || !scopes.isArray()) {
            return java.util.List.of();
        }
        java.util.List<String> names = new java.util.ArrayList<>();
        for (JsonNode scope : (ArrayNode) scopes) {
            String name = scope.asString("");
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        return names;
    }

    private void logCall(String functionName, ObjectNode arguments, McpCallContext context) {
        if (!auditLogEnabled() || !properties.audit().logToolCalls()) {
            return;
        }
        if (properties.audit().logArguments()) {
            LOGGER.info(
                    "MCP tool call: name={} requestId={} sessionId={} arguments={}",
                    functionName,
                    context.requestId(),
                    context.sessionId(),
                    auditText(arguments)
            );
            return;
        }
        LOGGER.info("MCP tool call: name={} requestId={} sessionId={}", functionName, context.requestId(), context.sessionId());
    }

    private void logResult(String functionName, DispatchExecutionResult result, McpCallContext context) {
        if (!auditLogEnabled() || !properties.audit().logToolResults()) {
            return;
        }
        LOGGER.info(
                "MCP tool result: name={} requestId={} sessionId={} isError={} contentItems={}",
                functionName,
                context.requestId(),
                context.sessionId(),
                result.isError(),
                result.content().size()
        );
    }

    private boolean auditLogEnabled() {
        return properties.audit().enabled() && "log".equalsIgnoreCase(properties.audit().storage());
    }

    private String auditText(JsonNode node) {
        JsonNode auditNode = properties.audit().redactSecrets() ? redact(node) : node;
        String value = auditNode.toString();
        int maxLength = properties.audit().maxArgumentLength();
        if (maxLength > 0 && value.length() > maxLength) {
            return value.substring(0, maxLength) + "...";
        }
        return value;
    }

    private JsonNode redact(JsonNode node) {
        if (!node.isObject()) {
            return node;
        }
        ObjectNode redacted = objectMapper.createObjectNode();
        node.properties().forEach(field -> {
            String key = field.getKey();
            JsonNode value = field.getValue();
            if (isSecretKey(key)) {
                redacted.put(key, properties.secrets().redactionPlaceholder());
            } else if (value.isObject()) {
                redacted.set(key, redact(value));
            } else {
                redacted.set(key, value);
            }
        });
        return redacted;
    }

    private boolean isSecretKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.contains("secret")
                || normalized.contains("token")
                || normalized.contains("password")
                || normalized.contains("authorization")
                || normalized.contains("api_key")
                || normalized.contains("apikey")
                || normalized.endsWith("key");
    }
}
