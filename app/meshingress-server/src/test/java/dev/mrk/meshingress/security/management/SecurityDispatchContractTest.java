package dev.mrk.meshingress.security.management;

import dev.mrk.meshingress.controller.security.RolesSecurityMcpController;
import dev.mrk.meshingress.controller.security.SecurityMcpController;
import dev.mrk.meshingress.route.framework.dispatch.McpDispatchMethodScanner;
import dev.mrk.meshingress.route.framework.dispatch.McpDispatchRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityDispatchContractTest {
    @Test
    void exposesTheApprovedSecurityManagementMethodsAndNoCredentialVerificationRoute() {
        McpDispatchRegistry registry = new McpDispatchMethodScanner().scan(List.of(
                new SecurityMcpController(null), new RolesSecurityMcpController(null)
        ));
        for (String method : List.of(
                "security/whoami", "roles/security/profiles/list", "roles/security/profiles/create",
                "roles/security/profiles/credentials/bind", "roles/security/credential-bindings/list",
                "roles/security/profiles/limits/get", "roles/security/profiles/limits/update",
                "roles/security/profiles/usage/list", "roles/security/tool-policies/create",
                "roles/security/tool-policies/evaluate")) {
            assertTrue(registry.find(method).isPresent(), method);
        }
        assertFalse(registry.find("roles/security/credential-bindings/verify").isPresent());
    }
}
