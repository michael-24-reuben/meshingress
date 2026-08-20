package dev.mrk.meshingress.config;

import dev.mrk.meshingress.mcp.McpWebSocketHandler;
import dev.mrk.meshingress.workflow.web.WorkflowWebSocketHandler;
import dev.mrk.meshingress.config.MeshingressProperties.Mcp.WebSocket;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class McpWebSocketConfig implements WebSocketConfigurer {

    private final McpWebSocketHandler webSocketHandler;
    private final WorkflowWebSocketHandler workflowWebSocketHandler;
    private final McpWebSocketHandshakeInterceptor handshakeInterceptor;
    private final WebSocket websocketProperties;

    public McpWebSocketConfig(
            McpWebSocketHandler webSocketHandler,
            WorkflowWebSocketHandler workflowWebSocketHandler,
            McpWebSocketHandshakeInterceptor handshakeInterceptor,
            MeshingressProperties properties
    ) {
        this.webSocketHandler = webSocketHandler;
        this.workflowWebSocketHandler = workflowWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
        this.websocketProperties = properties.mcp().websocket();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        if (!websocketProperties.enabled()) {
            return;
        }
        registry.addHandler(webSocketHandler, websocketProperties.path())
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns(websocketProperties.allowedOrigins().toArray(String[]::new));
        registry.addHandler(workflowWebSocketHandler, WorkflowWebSocketHandler.PATH)
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns(websocketProperties.allowedOrigins().toArray(String[]::new));
    }
}
