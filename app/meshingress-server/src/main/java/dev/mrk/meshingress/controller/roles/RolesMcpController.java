package dev.mrk.meshingress.controller.roles;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.route.annotations.McpDispatchMapping;
import dev.mrk.meshingress.route.annotations.McpDispatchMethod;
import dev.mrk.meshingress.route.annotations.McpDispatchParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import dev.mrk.meshingress.controller.roles.params.RolesToolAliasParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolCheckParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolDeleteParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolListParams;
import dev.mrk.meshingress.controller.roles.params.RolesToolUpdateParams;
import dev.mrk.meshingress.controller.roles.registration.ToolRegistrationParams;

@Component
@McpDispatchMapping("roles/tools")
public class RolesMcpController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RolesMcpController.class);

    private final RoleToolService roleToolService;
    private final ObjectMapper objectMapper;

    public RolesMcpController(RoleToolService roleToolService, ObjectMapper objectMapper) {
        this.roleToolService = roleToolService;
        this.objectMapper = objectMapper;
    }

    @McpDispatchMethod("check")
    public JsonNode check(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        LOGGER.info("MCP roles/tools/check: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.check(context, toParams(params, RolesToolCheckParams.class));
    }

    @McpDispatchMethod("register")
    public JsonNode register(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        LOGGER.info("MCP roles/tools/register: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.register(context, toParams(params, ToolRegistrationParams.class));
    }

    @McpDispatchMethod("alias")
    public JsonNode alias(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        LOGGER.info("MCP roles/tools/alias: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.alias(context, toParams(params, RolesToolAliasParams.class));
    }

    @McpDispatchMethod("update")
    public JsonNode update(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        LOGGER.info("MCP roles/tools/update: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.update(context, toParams(params, RolesToolUpdateParams.class));
    }

    @McpDispatchMethod("delete")
    public JsonNode delete(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        LOGGER.info("MCP roles/tools/delete: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.delete(context, toParams(params, RolesToolDeleteParams.class));
    }

    @McpDispatchMethod("list")
    public JsonNode list(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        LOGGER.info("MCP roles/tools/list: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.list(context, toParams(params, RolesToolListParams.class));
    }

    @McpDispatchMethod("reload")
    public JsonNode reload(McpCallContext context) {
        LOGGER.info("MCP roles/tools/reload: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.reload(context);
    }

    private <T> T toParams(JsonNode params, Class<T> type) {
        if (params == null || params.isMissingNode() || params.isNull()) {
            return objectMapper.convertValue(objectMapper.createObjectNode(), type);
        }
        return objectMapper.convertValue(params, type);
    }
}
