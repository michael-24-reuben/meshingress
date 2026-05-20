package dev.mrk.toolspace.instagram.instafetch;

import tools.jackson.databind.JsonNode;

public final class InstaFetchResult {
    private final JsonNode raw;
    private final InstaGraphDataHandler handler;
    private final JsonNode media;
    private final String videoUrl;
    private final String caption;
    private final JsonNode owner;
    private final boolean video;

    private InstaFetchResult(JsonNode response) {
        this.raw = response;
        this.handler = InstaGraphDataHandler.from(response);
        this.media = handler.getMedia();
        this.videoUrl = handler.getVideoUrl();
        this.caption = handler.getCaption();
        this.owner = handler.getOwner();
        this.video = handler.isVideo();
    }

    public static InstaFetchResult from(JsonNode response) {
        return new InstaFetchResult(response);
    }

    public JsonNode raw() {
        return raw;
    }

    public InstaGraphDataHandler getHandler() {
        return handler;
    }

    public JsonNode media() {
        return media;
    }

    public String videoUrl() {
        return videoUrl;
    }

    public String caption() {
        return caption;
    }

    public JsonNode owner() {
        return owner;
    }

    public boolean isVideo() {
        return video;
    }
}
