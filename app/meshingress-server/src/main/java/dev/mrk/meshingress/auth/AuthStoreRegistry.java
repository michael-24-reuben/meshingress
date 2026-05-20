package dev.mrk.meshingress.auth;

import java.util.Optional;

public interface AuthStoreRegistry {

    BootstrapAdminState bootstrapAdmin();

    boolean hasAdmin();

    FirstAdminRegistrationResult registerFirstAdmin(FirstAdminRegistrationCommand command);

    Optional<McpAuthenticatedSession> validateMcpCredentials(
            String authorization,
            String secretKey,
            String authToken
    );

    boolean isAdminBearerToken(String authorization);
}
