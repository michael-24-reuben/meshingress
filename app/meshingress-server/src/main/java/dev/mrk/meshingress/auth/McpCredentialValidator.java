package dev.mrk.meshingress.auth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class McpCredentialValidator {

    private final AuthStoreRegistry authStoreRegistry;

    public McpCredentialValidator(AuthStoreRegistry authStoreRegistry) {
        this.authStoreRegistry = authStoreRegistry;
    }

    public McpAuthenticatedSession validate(String authorization, String secretKey, String authToken) {
        requirePresent(authorization, "Authorization");
        requirePresent(secretKey, "X-Secret-Key");
        requirePresent(authToken, "X-Auth-Token");

        return authStoreRegistry
                .validateMcpCredentials(authorization, secretKey, authToken)
                .orElseThrow(() -> new InvalidMcpCredentialException(
                        HttpStatus.FORBIDDEN,
                        "Invalid MCP WebSocket credentials"
                ));
    }

    private void requirePresent(String value, String headerName) {
        if (value == null || value.isBlank()) {
            throw new InvalidMcpCredentialException(HttpStatus.UNAUTHORIZED, "Missing " + headerName);
        }
    }
}
