package dev.mrk.meshingress.config;

import dev.mrk.meshingress.security.McpAccessPolicyService;
import dev.mrk.meshingress.security.McpTransportContextFactory;
import dev.mrk.meshingress.security.McpTransportEvidence;
import dev.mrk.meshingress.api.McpPrincipal;
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
    private final McpTransportContextFactory contextFactory;

    public McpWebSocketHandshakeInterceptor(McpAccessPolicyService accessPolicyService, McpTransportContextFactory contextFactory) {
        this.accessPolicyService = accessPolicyService;
        this.contextFactory = contextFactory;
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
        String sessionId = firstHeader(headers, "Mcp-Session-Id", "X-Mcp-Session-Id");
        String requestId = headers.getFirst("X-Request-Id");

        McpTransportEvidence evidence = new McpTransportEvidence(authorization, sessionId, requestId, McpTransportEvidence.Transport.WEBSOCKET);
        McpPrincipal principal = contextFactory.resolve(evidence);
        if (accessPolicyService.websocketAuthenticationRequired() && !principal.authenticated()) {
            LOGGER.warn("Rejected unauthenticated MCP WebSocket handshake: requestId={} sessionId={}", requestId, sessionId);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put("mcp.transportEvidence", evidence);
        attributes.put("mcp.principal", principal);
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
