package dev.mrk.meshingress.config;

import dev.mrk.meshingress.mcp.McpAuthHandshakeInterceptor;
import dev.mrk.meshingress.mcp.McpWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class McpWebSocketConfig implements WebSocketConfigurer {

    private final McpWebSocketHandler webSocketHandler;
    private final McpAuthHandshakeInterceptor handshakeInterceptor;
    private final String path;
    private final String allowedOrigins;

    public McpWebSocketConfig(
            McpWebSocketHandler webSocketHandler,
            McpAuthHandshakeInterceptor handshakeInterceptor,
            @Value("${meshingress.mcp.websocket.path:/mcp/ws}") String path,
            @Value("${meshingress.mcp.websocket.allowed-origins:*}") String allowedOrigins
    ) {
        this.webSocketHandler = webSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
        this.path = path;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, path)
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns(originPatterns());
    }

    private String[] originPatterns() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);
    }
}
