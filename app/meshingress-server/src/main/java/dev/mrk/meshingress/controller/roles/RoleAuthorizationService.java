package dev.mrk.meshingress.controller.roles;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.auth.AuthStoreRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RoleAuthorizationService {

    private final String adminToken;
    private final AuthStoreRegistry authStoreRegistry;

    public RoleAuthorizationService(
            AuthStoreRegistry authStoreRegistry,
            @Value("${meshingress.mcp.roles.admin-token:}") String roleAdminToken,
            @Value("${meshingress.mcp.admin-token:dev-admin}") String legacyAdminToken
    ) {
        this.authStoreRegistry = authStoreRegistry;
        this.adminToken = roleAdminToken == null || roleAdminToken.isBlank() ? legacyAdminToken : roleAdminToken;
    }

    public boolean hasAdminRole(McpCallContext context) {
        if (context.authorizationHeader() != null
                && context.authorizationHeader().equals("Bearer " + adminToken)) {
            return true;
        }
        return authStoreRegistry.isAdminBearerToken(context.authorizationHeader());
    }
}
