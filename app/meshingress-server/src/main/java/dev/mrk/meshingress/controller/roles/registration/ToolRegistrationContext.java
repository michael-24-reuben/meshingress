package dev.mrk.meshingress.controller.roles.registration;

import dev.mrk.meshingress.api.McpCallContext;

import java.time.OffsetDateTime;

public record ToolRegistrationContext(
        McpCallContext call,
        String actor,
        OffsetDateTime requestedAt
) {
    public static ToolRegistrationContext from(McpCallContext call) {
        String actor = call == null ? "unknown" : call.principal().subject();
        return new ToolRegistrationContext(call, actor, OffsetDateTime.now());
    }
}
