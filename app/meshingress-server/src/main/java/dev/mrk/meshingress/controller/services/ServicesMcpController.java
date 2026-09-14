package dev.mrk.meshingress.controller.services;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.mcp.services.McpServiceRegistry;
import dev.mrk.meshingress.route.annotations.McpDispatchMapping;
import dev.mrk.meshingress.route.annotations.McpDispatchMethod;
import dev.mrk.meshingress.security.ProfileLimitService;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** MCP discovery boundary for typed provider nodes; providers are never callable through this controller. */
@Component
@McpDispatchMapping("services")
public final class ServicesMcpController {

    private final ObjectMapper objectMapper;
    private final McpServiceRegistry serviceRegistry;
    private final ProfileLimitService profileLimits;

    public ServicesMcpController(ObjectMapper objectMapper, McpServiceRegistry serviceRegistry, ProfileLimitService profileLimits) {
        this.objectMapper = objectMapper;
        this.serviceRegistry = serviceRegistry;
        this.profileLimits = profileLimits;
    }

    @McpDispatchMethod("list")
    public @NonNull ObjectNode servicesList(McpCallContext context) {
        try (ProfileLimitService.Reservation ignored = profileLimits.reserveRequest(context)) {
            ObjectNode result = objectMapper.createObjectNode();
            ArrayNode services = objectMapper.createArrayNode();
            serviceRegistry.listPublicEnabledServices().forEach(service -> services.add(service.toMcpJson(objectMapper)));
            result.set("services", services);
            return result;
        }
    }
}
