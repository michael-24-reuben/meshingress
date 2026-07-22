package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.api.McpCallContext;

import java.util.Objects;

/**
 * Transport-specific server context for one JSON-RPC request.
 */
public record McpInvocation(
        McpCallContext context,
        Transport transport,
        McpProgressLifecycle progressLifecycle
) {

    public McpInvocation {
        context = Objects.requireNonNull(context, "context must not be null");
        transport = Objects.requireNonNull(transport, "transport must not be null");
    }

    public static McpInvocation http(McpCallContext context) {
        return new McpInvocation(context, Transport.HTTP, null);
    }

    public static McpInvocation webSocket(McpCallContext context, McpProgressLifecycle progressLifecycle) {
        return new McpInvocation(context, Transport.WEBSOCKET, Objects.requireNonNull(progressLifecycle, "progressLifecycle must not be null"));
    }

    public enum Transport {
        HTTP,
        WEBSOCKET
    }
}
