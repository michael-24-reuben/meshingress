package dev.mrk.meshingress.config;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.security.McpAccessPolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class McpWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpWebSocketHandshakeInterceptor.class);

    private final McpAccessPolicyService accessPolicyService;

    public McpWebSocketHandshakeInterceptor(McpAccessPolicyService accessPolicyService) {
        this.accessPolicyService = accessPolicyService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        HttpHeaders headers = request.getHeaders();
        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        String role = firstHeader(headers, "X-Mcp-Role", "X-Mcp-Admin");
        String sessionId = firstHeader(headers, "Mcp-Session-Id", "X-Mcp-Session-Id");
        String requestId = headers.getFirst("X-Request-Id");

        McpCallContext context = new McpCallContext(authorization, role, sessionId, requestId);
        if (accessPolicyService.websocketAuthenticationRequired() && authorization == null) {
            LOGGER.warn("Rejected unauthenticated MCP WebSocket handshake: requestId={} sessionId={}", requestId, sessionId);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put("mcp.authorization", authorization);
        attributes.put("mcp.role", role);
        attributes.put("mcp.sessionId", sessionId);
        attributes.put("mcp.requestId", requestId);
        attributes.put("mcp.admin", accessPolicyService.isAdmin(context));
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }

    private static String firstHeader(HttpHeaders headers, String first, String second) {
        String value = headers.getFirst(first);
        return value == null ? headers.getFirst(second) : value;
    }
}
