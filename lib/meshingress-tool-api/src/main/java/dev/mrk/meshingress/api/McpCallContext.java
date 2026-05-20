package dev.mrk.meshingress.api;

public record McpCallContext(
        String authorizationHeader,
        String roleHeader,
        String sessionId,
        String requestId
) {

    @Deprecated
    public String adminHeader() {
        return roleHeader;
    }
}
