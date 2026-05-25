package dev.mrk.meshingress.security;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class McpAccessPolicyService {

    private final MeshingressProperties properties;
    private final String adminToken;

    public McpAccessPolicyService(MeshingressProperties properties, Environment environment) {
        this.properties = properties;
        this.adminToken = environment.getProperty(
                "meshingress.mcp.roles.admin-token",
                environment.getProperty("meshingress.mcp.admin-token", "dev-admin")
        );
    }

    public void requireAuthenticated(McpCallContext context) {
        if (!properties.security().enabled() || !properties.security().requireAuthentication()) {
            return;
        }
        if (!hasText(context.authorizationHeader())) {
            throw new JsonRpcException(JsonRpcErrorCodes.UNAUTHORIZED, "Authentication is required.");
        }
    }

    public boolean isAdmin(McpCallContext context) {
        if ("admin".equalsIgnoreCase(nullToBlank(context.roleHeader()))
                || "true".equalsIgnoreCase(nullToBlank(context.roleHeader()))) {
            return true;
        }
        String authorization = context.authorizationHeader();
        return hasText(adminToken)
                && authorization != null
                && authorization.strip().equals("Bearer " + adminToken);
    }

    public void requireAdmin(McpCallContext context) {
        if (!isAdmin(context)) {
            throw new JsonRpcException(JsonRpcErrorCodes.FORBIDDEN, "Admin role is required.");
        }
    }

    public boolean websocketAuthenticationRequired() {
        return properties.mcp().websocket().requireAuth()
                || (properties.security().enabled() && properties.security().requireAuthentication());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
