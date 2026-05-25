package dev.mrk.meshingress.controller.roles;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.McpToolPatch;
import dev.mrk.meshingress.api.tools.ToolVisibility;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import dev.mrk.meshingress.mcp.tools.ToolAuditEvent;
import dev.mrk.meshingress.mcp.tools.ToolCheckResult;
import dev.mrk.meshingress.mcp.tools.ToolRegistry;
import dev.mrk.meshingress.security.McpAccessPolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Service
public class RoleToolService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoleToolService.class);

    private final ObjectMapper objectMapper;
    private final ToolRegistry toolRegistry;
    private final McpAccessPolicyService accessPolicyService;

    public RoleToolService(ObjectMapper objectMapper, ToolRegistry toolRegistry, McpAccessPolicyService accessPolicyService) {
        this.objectMapper = objectMapper;
        this.toolRegistry = toolRegistry;
        this.accessPolicyService = accessPolicyService;
    }

    public ObjectNode check(McpCallContext context, JsonNode params) {
        accessPolicyService.requireAdmin(context);
        McpToolDescriptor descriptor = descriptorFromParams(params.path("tool"), true);
        boolean updateMode = params.path("mode").asString("").equals("update");
        return checkResultToJson(toolRegistry.check(descriptor, updateMode));
    }

    public ObjectNode register(McpCallContext context, JsonNode params) {
        accessPolicyService.requireAdmin(context);
        McpToolDescriptor descriptor = descriptorFromParams(params.path("tool"), true);
        McpToolDescriptor registered = toolRegistry.register(descriptor, context);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("registered", true);
        result.put("toolName", registered.name());
        result.put("version", registered.version());
        result.put("registryVersion", toolRegistry.registryVersion());
        return result;
    }

    public ObjectNode update(McpCallContext context, JsonNode params) {
        accessPolicyService.requireAdmin(context);
        if (!params.isObject()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "roles/tools/update params must be an object");
        }
        String name = params.path("name").asString("");
        if (name.isBlank()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "roles/tools/update params.name is required");
        }
        McpToolPatch patch = patchFromJson(params.path("patch"));
        int previousVersion = toolRegistry.findTool(name)
                .map(McpToolDescriptor::version)
                .orElse(0);
        McpToolDescriptor updated = toolRegistry.update(name, patch, context);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("updated", true);
        result.put("toolName", updated.name());
        result.put("previousVersion", previousVersion);
        result.put("version", updated.version());
        result.put("registryVersion", toolRegistry.registryVersion());
        return result;
    }

    public ObjectNode delete(McpCallContext context, JsonNode params) {
        accessPolicyService.requireAdmin(context);
        if (!params.isObject()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "roles/tools/delete params must be an object");
        }
        String name = params.path("name").asString("");
        if (name.isBlank()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "roles/tools/delete params.name is required");
        }
        String mode = params.path("mode").asString("disable");
        if (!mode.equals("disable")) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Only disable mode is supported in the MVP");
        }

        int previousVersion = toolRegistry.findTool(name)
                .map(McpToolDescriptor::version)
                .orElse(0);
        McpToolDescriptor disabled = toolRegistry.disable(name, context);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("deleted", false);
        result.put("disabled", true);
        result.put("toolName", disabled.name());
        result.put("previousVersion", previousVersion);
        result.put("version", disabled.version());
        result.put("registryVersion", toolRegistry.registryVersion());
        return result;
    }

    public ObjectNode list(McpCallContext context, JsonNode params) {
        accessPolicyService.requireAdmin(context);
        boolean includeDisabled = params.path("includeDisabled").asBoolean(false);
        boolean includePrivate = params.path("includePrivate").asBoolean(false);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("registryVersion", toolRegistry.registryVersion());
        ArrayNode tools = objectMapper.createArrayNode();
        for (McpToolDescriptor descriptor : toolRegistry.listRoleVisibleTools(includeDisabled, includePrivate)) {
            ObjectNode tool = descriptor.toMcpJson(objectMapper);
            tool.put("version", descriptor.version());
            tool.put("enabled", descriptor.enabled());
            tool.put("visibility", descriptor.visibility().toWire());
            tool.put("dynamic", descriptor.dynamic());
            tools.add(tool);
        }
        result.set("tools", tools);
        ArrayNode audit = objectMapper.createArrayNode();
        for (ToolAuditEvent event : toolRegistry.auditEvents()) {
            ObjectNode eventJson = objectMapper.createObjectNode();
            eventJson.put("at", event.at().toString());
            eventJson.put("actor", event.actor());
            eventJson.put("action", event.action());
            eventJson.put("toolName", event.toolName());
            eventJson.put("previousVersion", event.previousVersion());
            eventJson.put("newVersion", event.newVersion());
            eventJson.put("registryVersion", event.registryVersion());
            if (event.requestId() != null) {
                eventJson.put("requestId", event.requestId());
            }
            audit.add(eventJson);
        }
        result.set("auditEvents", audit);
        return result;
    }

    public ObjectNode reload(McpCallContext context) {
        accessPolicyService.requireAdmin(context);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("reloaded", true);
        result.put("registryVersion", toolRegistry.registryVersion());
        return result;
    }


    private ObjectNode checkResultToJson(ToolCheckResult check) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("valid", check.valid());
        ArrayNode errors = objectMapper.createArrayNode();
        check.errors().forEach(errors::add);
        result.set("errors", errors);
        ArrayNode warnings = objectMapper.createArrayNode();
        check.warnings().forEach(warnings::add);
        result.set("warnings", warnings);
        result.set("normalized", check.normalized());
        return result;
    }

    private McpToolDescriptor descriptorFromParams(JsonNode tool, boolean dynamic) {
        if (!tool.isObject()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "tool must be an object");
        }
        return new McpToolDescriptor(
                tool.path("name").asString(""),
                tool.path("title").asString(null),
                tool.path("description").asString(""),
                tool.path("version").asInt(1),
                !tool.has("enabled") || tool.path("enabled").asBoolean(),
                visibilityFromJson(tool.path("visibility")),
                functionsFromParams(tool, dynamic),
                optionalObject(tool, "annotations"),
                dynamic
        );
    }

    private java.util.List<McpFunctionDescriptor> functionsFromParams(JsonNode tool, boolean dynamic) {
        JsonNode functions = tool.path("functions");
        if (functions.isArray()) {
            java.util.List<McpFunctionDescriptor> descriptors = new java.util.ArrayList<>();
            for (JsonNode function : functions) {
                descriptors.add(functionFromJson(function, tool, dynamic));
            }
            return java.util.List.copyOf(descriptors);
        }

        return java.util.List.of(new McpFunctionDescriptor(
                tool.path("name").asString(""),
                tool.path("title").asString(null),
                tool.path("description").asString(""),
                tool.path("version").asInt(1),
                !tool.has("enabled") || tool.path("enabled").asBoolean(),
                visibilityFromJson(tool.path("visibility")),
                tool.path("handlerKey").asString(""),
                requiredObject(tool, "inputSchema"),
                optionalObject(tool, "outputSchema"),
                optionalObject(tool, "annotations"),
                dynamic
        ));
    }

    private McpFunctionDescriptor functionFromJson(JsonNode function, JsonNode parentTool, boolean dynamic) {
        if (!function.isObject()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "function must be an object");
        }
        return new McpFunctionDescriptor(
                function.path("name").asString(""),
                function.path("title").asString(null),
                function.path("description").asString(parentTool.path("description").asString("")),
                function.path("version").asInt(parentTool.path("version").asInt(1)),
                !function.has("enabled") || function.path("enabled").asBoolean(),
                function.has("visibility") ? visibilityFromJson(function.path("visibility")) : visibilityFromJson(parentTool.path("visibility")),
                function.path("handlerKey").asString(""),
                requiredObject(function, "inputSchema"),
                optionalObject(function, "outputSchema"),
                optionalObject(function, "annotations"),
                dynamic
        );
    }

    private McpToolPatch patchFromJson(JsonNode patch) {
        if (!patch.isObject()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "patch must be an object");
        }
        return new McpToolPatch(
                patch.has("title") ? patch.path("title").asString() : null,
                patch.has("description") ? patch.path("description").asString() : null,
                patch.has("enabled") ? patch.path("enabled").asBoolean() : null,
                patch.has("visibility") ? visibilityFromJson(patch.path("visibility")) : null,
                patch.has("handlerKey") ? patch.path("handlerKey").asString() : null,
                optionalObject(patch, "inputSchema"),
                optionalObject(patch, "outputSchema"),
                optionalObject(patch, "annotations")
        );
    }

    private ToolVisibility visibilityFromJson(JsonNode value) {
        try {
            return ToolVisibility.fromWire(value.asString("public"));
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Invalid tool visibility value: {}", value.asString(""), exception);
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "visibility must be public, private, or admin");
        }
    }

    private JsonNode requiredObject(JsonNode parent, String field) {
        JsonNode value = parent.path(field);
        if (!value.isObject()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, field + " must be an object");
        }
        return value;
    }

    private JsonNode optionalObject(JsonNode parent, String field) {
        JsonNode value = parent.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (!value.isObject()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, field + " must be an object");
        }
        return value;
    }
}
