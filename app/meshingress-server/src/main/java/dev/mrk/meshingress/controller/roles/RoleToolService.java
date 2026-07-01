package dev.mrk.meshingress.controller.roles;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.McpToolPatch;
import dev.mrk.meshingress.api.tools.ToolVisibility;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.controller.roles.params.RolesToolAliasParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolCheckParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolDeleteParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolInstallPublicationParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolListParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolUpdateParams;
import dev.mrk.meshingress.controller.roles.params.ToolDescriptorParams;
import dev.mrk.meshingress.controller.roles.params.ToolFunctionParams;
import dev.mrk.meshingress.controller.roles.params.ToolPatchParams;
import dev.mrk.meshingress.controller.roles.registration.ToolRegistrationParams;
import dev.mrk.meshingress.controller.roles.registration.ToolRegistrationService;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import dev.mrk.meshingress.mcp.tools.ToolAuditEvent;
import dev.mrk.meshingress.mcp.tools.ToolCheckResult;
import dev.mrk.meshingress.mcp.tools.registry.ToolRegistry;
import dev.mrk.meshingress.security.McpAccessPolicyService;
import dev.mrk.meshingress.server.install.ArtifactInstaller;
import dev.mrk.meshingress.server.install.RepositoryArtifactFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

@Service
public class RoleToolService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoleToolService.class);

    private final ObjectMapper objectMapper;
    private final ToolRegistry toolRegistry;
    private final McpAccessPolicyService accessPolicyService;
    private final ToolRegistrationService toolRegistrationService;
    private final ArtifactInstaller artifactInstaller;
    private final RepositoryArtifactFetcher repositoryArtifactFetcher;

    public RoleToolService(
            ObjectMapper objectMapper,
            ToolRegistry toolRegistry,
            McpAccessPolicyService accessPolicyService,
            ToolRegistrationService toolRegistrationService,
            ArtifactInstaller artifactInstaller,
            RepositoryArtifactFetcher repositoryArtifactFetcher
    ) {
        this.objectMapper = objectMapper;
        this.toolRegistry = toolRegistry;
        this.accessPolicyService = accessPolicyService;
        this.toolRegistrationService = toolRegistrationService;
        this.artifactInstaller = artifactInstaller;
        this.repositoryArtifactFetcher = repositoryArtifactFetcher;
    }

    public ObjectNode check(McpCallContext context, RolesToolCheckParams params) {
        accessPolicyService.requireAdmin(context);
        McpToolDescriptor descriptor = descriptorFromParams(required(params).tool(), true);
        boolean updateMode = "update".equalsIgnoreCase(params.mode());
        return checkResultToJson(toolRegistry.check(descriptor, updateMode));
    }

    public ObjectNode register(McpCallContext context, ToolRegistrationParams params) {
        accessPolicyService.requireAdmin(context);
        if (toolRegistrationService.isPhaseRegistration(params)) {
            return toolRegistrationService.register(context, params);
        }
        throw new JsonRpcException(
                JsonRpcErrorCodes.INVALID_PARAMS,
                "roles/tools/register requires phase-aware registration params."
        );
    }

    public ObjectNode installPublication(McpCallContext context, RolesToolInstallPublicationParams params) {
        accessPolicyService.requireAdmin(context);
        if (params == null) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "roles/tools/installPublication params must be an object");
        }
        if (params.publication() != null && params.coordinate() != null) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Provide either params.publication or params.coordinate, not both");
        }
        if (params.publication() == null && params.coordinate() == null) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "roles/tools/installPublication params.publication or params.coordinate is required");
        }
        var publication = params.publication() == null
                ? repositoryArtifactFetcher.fetchPublication(params.coordinate())
                : params.publication();
        return artifactInstaller.install(publication, params.toolId(), context).toJson(objectMapper);
    }

    public ObjectNode alias(McpCallContext context, RolesToolAliasParams params) {
        accessPolicyService.requireAdmin(context);
        McpToolDescriptor descriptor = descriptorFromParams(required(params).tool(), true);
        McpToolDescriptor registered = toolRegistry.register(descriptor, context);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("aliased", true);
        result.put("toolName", registered.name());
        result.put("version", registered.version());
        result.put("registryVersion", toolRegistry.registryVersion());
        return result;
    }

    public ObjectNode update(McpCallContext context, RolesToolUpdateParams params) {
        accessPolicyService.requireAdmin(context);
        if (params == null) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "roles/tools/update params must be an object");
        }
        String name = textOrEmpty(params.name());
        if (name.isBlank()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "roles/tools/update params.name is required");
        }

        McpToolPatch patch = patchFromParams(params.patch());
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

    public ObjectNode delete(McpCallContext context, RolesToolDeleteParams params) {
        accessPolicyService.requireAdmin(context);
        if (params == null) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "roles/tools/delete params must be an object");
        }
        String name = textOrEmpty(params.name());
        if (name.isBlank()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "roles/tools/delete params.name is required");
        }
        String mode = params.mode() == null ? "disable" : params.mode();
        if (!mode.equals("disable")) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "Only disable mode is supported in the MVP");
        }

        if (toolRegistrationService.hasActiveRegistration(name)) {
            return toolRegistrationService.delete(context, name, mode);
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

    public ObjectNode list(McpCallContext context, RolesToolListParams params) {
        accessPolicyService.requireAdmin(context);
        boolean includeDisabled = params != null && Boolean.TRUE.equals(params.includeDisabled());
        boolean includePrivate = params != null && Boolean.TRUE.equals(params.includePrivate());

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
        result.set("registrations", toolRegistrationService.registrationsToJson());
        return result;
    }

    public ObjectNode reload(McpCallContext context) {
        accessPolicyService.requireAdmin(context);
        return toolRegistrationService.reloadStatus();
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

    private McpToolDescriptor descriptorFromParams(ToolDescriptorParams tool, boolean dynamic) {
        if (tool == null) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "tool must be an object");
        }
        return new McpToolDescriptor(
                textOrEmpty(tool.name()),
                blankToNull(tool.title()),
                textOrEmpty(tool.description()),
                intOrDefault(tool.version(), 1),
                boolOrTrue(tool.enabled()),
                visibilityFromParams(tool.visibility()),
                functionsFromParams(tool, dynamic),
                optionalObject(tool.annotations(), "annotations"),
                dynamic
        );
    }

    private java.util.List<McpFunctionDescriptor> functionsFromParams(ToolDescriptorParams tool, boolean dynamic) {
        List<ToolFunctionParams> functions = tool.functions();
        if (functions != null && !functions.isEmpty()) {
            java.util.List<McpFunctionDescriptor> descriptors = new java.util.ArrayList<>();
            for (ToolFunctionParams function : functions) {
                descriptors.add(functionFromParams(function, tool, dynamic));
            }
            return java.util.List.copyOf(descriptors);
        }

        return java.util.List.of(new McpFunctionDescriptor(
                textOrEmpty(tool.name()),
                blankToNull(tool.title()),
                textOrEmpty(tool.description()),
                intOrDefault(tool.version(), 1),
                boolOrTrue(tool.enabled()),
                visibilityFromParams(tool.visibility()),
                textOrEmpty(tool.handlerKey()),
                requiredObject(tool.inputSchema(), "inputSchema"),
                optionalObject(tool.outputSchema(), "outputSchema"),
                optionalObject(tool.annotations(), "annotations"),
                dynamic
        ));
    }

    private McpFunctionDescriptor functionFromParams(ToolFunctionParams function, ToolDescriptorParams parentTool, boolean dynamic) {
        if (function == null) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "function must be an object");
        }
        return new McpFunctionDescriptor(
                textOrEmpty(function.name()),
                blankToNull(function.title()),
                textOrEmpty(defaultText(function.description(), parentTool.description())),
                intOrDefault(function.version(), intOrDefault(parentTool.version(), 1)),
                boolOrTrue(function.enabled()),
                function.visibility() == null || function.visibility().isBlank()
                        ? visibilityFromParams(parentTool.visibility())
                        : visibilityFromParams(function.visibility()),
                textOrEmpty(function.handlerKey()),
                requiredObject(function.inputSchema(), "inputSchema"),
                optionalObject(function.outputSchema(), "outputSchema"),
                optionalObject(function.annotations(), "annotations"),
                dynamic
        );
    }

    private McpToolPatch patchFromParams(ToolPatchParams patch) {
        if (patch == null) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "patch must be an object");
        }
        return new McpToolPatch(
                patch.title(),
                patch.description(),
                patch.enabled(),
                patch.visibility() == null ? null : visibilityFromParams(patch.visibility()),
                patch.handlerKey(),
                optionalObject(patch.inputSchema(), "inputSchema"),
                optionalObject(patch.outputSchema(), "outputSchema"),
                optionalObject(patch.annotations(), "annotations")
        );
    }

    private ToolVisibility visibilityFromParams(String value) {
        try {
            String visibility = value == null || value.isBlank() ? "public" : value;
            return ToolVisibility.fromWire(visibility);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Invalid tool visibility value: {}", value == null ? "" : value, exception);
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "visibility must be public, private, or admin");
        }
    }

    private JsonNode requiredObject(JsonNode value, String field) {
        if (value == null || !value.isObject()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, field + " must be an object");
        }
        return value;
    }

    private JsonNode optionalObject(JsonNode value, String field) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isObject()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, field + " must be an object");
        }
        return value;
    }

    private RolesToolCheckParams required(RolesToolCheckParams params) {
        if (params == null) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "roles/tools/check params must be an object");
        }
        return params;
    }

    private RolesToolAliasParams required(RolesToolAliasParams params) {
        if (params == null) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, "roles/tools/alias params must be an object");
        }
        return params;
    }

    private String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String defaultText(String primary, String fallback) {
        return (primary == null || primary.isBlank()) ? textOrEmpty(fallback) : primary;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private int intOrDefault(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private boolean boolOrTrue(Boolean value) {
        return value == null || value;
    }
}
