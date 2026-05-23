package dev.mrk.meshingress.controller.roles;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.route.annotations.McpDispatchMapping;
import dev.mrk.meshingress.route.annotations.McpDispatchMethod;
import dev.mrk.meshingress.route.annotations.McpDispatchParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@McpDispatchMapping("roles/tools")
public class RolesMcpController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RolesMcpController.class);

    private final RoleToolService roleToolService;

    public RolesMcpController(RoleToolService roleToolService) {
        this.roleToolService = roleToolService;
    }

    @McpDispatchMethod("check")
    public JsonNode check(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        LOGGER.info("MCP roles/tools/check: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.check(context, params);
    }

    @McpDispatchMethod("register")
    public JsonNode register(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        LOGGER.info("MCP roles/tools/register: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.register(context, params);
    }

    @McpDispatchMethod("update")
    public JsonNode update(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        LOGGER.info("MCP roles/tools/update: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.update(context, params);
    }

    @McpDispatchMethod("delete")
    public JsonNode delete(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        LOGGER.info("MCP roles/tools/delete: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.delete(context, params);
    }

    @McpDispatchMethod("list")
    public JsonNode list(@McpDispatchParam("params") JsonNode params, McpCallContext context) {
        LOGGER.info("MCP roles/tools/list: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.list(context, params);
    }

    @McpDispatchMethod("reload")
    public JsonNode reload(McpCallContext context) {
        LOGGER.info("MCP roles/tools/reload: requestId={} sessionId={}", context.requestId(), context.sessionId());
        return roleToolService.reload(context);
    }
}
