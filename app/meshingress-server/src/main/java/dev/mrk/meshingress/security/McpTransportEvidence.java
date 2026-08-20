package dev.mrk.meshingress.security;

/**
 * Server-only request evidence. This type must not cross into a tool module or
 * be stored in workflow/audit metadata.
 */
public record McpTransportEvidence(String authorization, String sessionId, String correlationId, Transport transport) {
    public enum Transport { HTTP, WEBSOCKET, WORKFLOW }
}
