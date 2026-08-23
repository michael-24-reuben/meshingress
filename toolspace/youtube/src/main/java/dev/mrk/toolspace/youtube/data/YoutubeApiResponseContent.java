package dev.mrk.toolspace.youtube.data;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import tools.jackson.databind.JsonNode;

import java.util.Objects;

/** Preserves the official Data API response while marking the requested resource. */
public final class YoutubeApiResponseContent extends StructuredContent {
    private final String resource;
    private final JsonNode response;

    public YoutubeApiResponseContent(String resource, JsonNode response) {
        super(StructuredContentKind.Network.API_RESPONSE);
        this.resource = Objects.requireNonNull(resource, "resource must not be null");
        this.response = Objects.requireNonNull(response, "response must not be null").deepCopy();
    }

    public String getResource() {
        return resource;
    }

    public JsonNode getResponse() {
        return response.deepCopy();
    }
}
