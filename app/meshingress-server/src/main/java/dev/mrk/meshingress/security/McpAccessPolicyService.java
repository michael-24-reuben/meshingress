package dev.mrk.meshingress.security;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.McpAuthenticationMethod;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import org.springframework.stereotype.Service;

/** Principal-based MCP transport and administration policy. */
@Service
public class McpAccessPolicyService {
    private final MeshingressProperties properties;

    public McpAccessPolicyService(MeshingressProperties properties) { this.properties = properties; }

    public void requireAuthenticated(McpCallContext context) {
        if (properties.security().enabled() && properties.security().requireAuthentication() && !context.principal().authenticated()) {
            throw new JsonRpcException(JsonRpcErrorCodes.UNAUTHORIZED, "Authentication is required.");
        }
    }

    public boolean isAdmin(McpCallContext context) { return context != null && context.principal().hasRole("admin"); }

    public void requireAdmin(McpCallContext context) {
        if (!isAdmin(context)) throw new JsonRpcException(JsonRpcErrorCodes.FORBIDDEN, "Admin role is required.");
    }

    /** Security-management routes require a verified non-development OIDC identity. */
    public void requireVerifiedIdentity(McpCallContext context) {
        if (context == null || context.principal().authenticationMethod() != McpAuthenticationMethod.OIDC_JWT
                || context.principal().expiresAt() == null || !context.principal().expiresAt().isAfter(java.time.Instant.now())) {
            throw new JsonRpcException(JsonRpcErrorCodes.UNAUTHORIZED, "A verified OIDC identity is required.");
        }
    }

    public void requireSecurityManager(McpCallContext context) {
        requireVerifiedIdentity(context);
        if (!context.principal().hasRole("admin") && !context.principal().grants().contains("security.manage")) {
            throw new JsonRpcException(JsonRpcErrorCodes.FORBIDDEN, "security.manage grant or admin role is required.");
        }
    }

    public boolean websocketAuthenticationRequired() {
        return properties.mcp().websocket().requireAuth() || (properties.security().enabled() && properties.security().requireAuthentication());
    }
}
