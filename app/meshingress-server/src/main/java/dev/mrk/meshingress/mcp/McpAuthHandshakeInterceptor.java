package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.auth.InvalidMcpCredentialException;
import dev.mrk.meshingress.auth.McpAuthenticatedSession;
import dev.mrk.meshingress.auth.McpCredentialValidator;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class McpAuthHandshakeInterceptor implements HandshakeInterceptor {

    public static final String AUTHENTICATED_SESSION_ATTRIBUTE = "mcp.authenticatedSession";
    public static final String AUTHORIZATION_ATTRIBUTE = "mcp.authorization";
    public static final String ROLE_ATTRIBUTE = "mcp.role";
    public static final String SESSION_ID_ATTRIBUTE = "mcp.sessionId";
    public static final String REQUEST_ID_ATTRIBUTE = "mcp.requestId";

    private final McpCredentialValidator credentialValidator;

    public McpAuthHandshakeInterceptor(McpCredentialValidator credentialValidator) {
        this.credentialValidator = credentialValidator;
    }

    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes
    ) {
        HttpHeaders headers = request.getHeaders();
        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        String roleHeader = headers.getFirst("X-Mcp-Role");
        String legacyAdminHeader = headers.getFirst("X-Mcp-Admin");

        try {
            McpAuthenticatedSession authenticatedSession = credentialValidator.validate(
                    authorization,
                    headers.getFirst("X-Secret-Key"),
                    headers.getFirst("X-Auth-Token")
            );
            attributes.put(AUTHENTICATED_SESSION_ATTRIBUTE, authenticatedSession);
            attributes.put(AUTHORIZATION_ATTRIBUTE, authorization);
            putIfPresent(attributes, ROLE_ATTRIBUTE, roleHeader == null ? legacyAdminHeader : roleHeader);
            putIfPresent(attributes, SESSION_ID_ATTRIBUTE, headers.getFirst("Mcp-Session-Id"));
            putIfPresent(attributes, REQUEST_ID_ATTRIBUTE, headers.getFirst("X-Request-Id"));
            return true;
        } catch (InvalidMcpCredentialException exception) {
            response.setStatusCode(exception.status());
            return false;
        }
    }

    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            Exception exception
    ) {
    }

    private void putIfPresent(Map<String, Object> attributes, String name, String value) {
        if (value != null) {
            attributes.put(name, value);
        }
    }
}
