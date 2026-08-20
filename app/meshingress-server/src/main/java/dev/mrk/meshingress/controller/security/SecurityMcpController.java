package dev.mrk.meshingress.controller.security;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.route.annotations.McpDispatchMapping;
import dev.mrk.meshingress.route.annotations.McpDispatchMethod;
import dev.mrk.meshingress.security.management.SecurityManagementService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.node.ObjectNode;

/** Authenticated caller introspection. It returns normalized claims only. */
@Component
@McpDispatchMapping("security")
public final class SecurityMcpController {
    private final SecurityManagementService security;
    public SecurityMcpController(SecurityManagementService security) { this.security = security; }

    @McpDispatchMethod("whoami")
    public ObjectNode whoami(McpCallContext context) { return security.whoami(context); }
}
