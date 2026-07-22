package dev.mrk.meshingress.api;

import dev.mrk.meshingress.api.result.progress.McpProgressReporter;

import java.util.Objects;

public record McpCallContext(
        String authorizationHeader,
        String roleHeader,
        String sessionId,
        String requestId,
        McpProgressReporter progressReporter
) {
    public McpCallContext(String authorizationHeader, String roleHeader, String sessionId, String requestId) {
        this(authorizationHeader, roleHeader, sessionId, requestId, new McpProgressReporter());
    }

    public McpCallContext {
        progressReporter = Objects.requireNonNullElseGet(progressReporter, McpProgressReporter::new);
    }

    public McpCallContext withProgressReporter(McpProgressReporter reporter) {
        return new McpCallContext(authorizationHeader, roleHeader, sessionId, requestId, reporter);
    }

    @Deprecated
    public String adminHeader() {
        return roleHeader;
    }
}
