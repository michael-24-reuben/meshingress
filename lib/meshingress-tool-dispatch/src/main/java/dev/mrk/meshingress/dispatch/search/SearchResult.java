package dev.mrk.meshingress.dispatch.search;

import tools.jackson.databind.JsonNode;

public record SearchResult(
        String id,
        String title,
        String summary,
        Double score,
        SourceRef source,
        JsonNode payload
) { }
