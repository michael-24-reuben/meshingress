package dev.mrk.meshingress.api.tools.annotation;

import tools.jackson.databind.node.ObjectNode;

public record McpAvailabilityConditionResult(
        String annotation,
        String condition,
        boolean available,
        String reason,
        ObjectNode details
) {
}