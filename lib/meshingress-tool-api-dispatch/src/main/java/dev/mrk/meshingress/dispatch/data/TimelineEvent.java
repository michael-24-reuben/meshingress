package dev.mrk.meshingress.dispatch.data;

import tools.jackson.databind.JsonNode;

public record TimelineEvent(
        String id,
        String title,
        String timestamp,
        String summary,
        JsonNode payload
) { }
