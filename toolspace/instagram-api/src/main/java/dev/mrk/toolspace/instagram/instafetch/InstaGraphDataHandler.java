package dev.mrk.toolspace.instagram.instafetch;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

public final class InstaGraphDataHandler {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JsonNode response;

    private InstaGraphDataHandler(JsonNode response) {
        this.response = response;
    }

    public static InstaGraphDataHandler from(JsonNode response) {
        return new InstaGraphDataHandler(response);
    }

    public static boolean isValidResponse(JsonNode response) {
        return response != null
                && response.isObject()
                && response.path("data").isObject();
    }

    public static boolean hasMedia(JsonNode response) {
        JsonNode media = mediaNode(response);
        return !media.isMissingNode() && !media.isNull();
    }

    public static boolean hasVideo(JsonNode response) {
        return hasMedia(response) && mediaNode(response).path("is_video").asBoolean(false);
    }

    public JsonNode getRawResponse() {
        return response;
    }

    public JsonNode getMedia() {
        JsonNode media = mediaNode(response);
        return media.isMissingNode() ? null : media;
    }

    public JsonNode requireMedia() {
        JsonNode media = getMedia();
        if (media == null || media.isNull()) {
            throw new InstaFetchException("Instagram response does not contain shortcode media");
        }
        return media;
    }

    public String getId() {
        return textOrNull(requireMedia().path("id"));
    }

    public String getShortcode() {
        return textOrNull(requireMedia().path("shortcode"));
    }

    public boolean isVideo() {
        JsonNode media = getMedia();
        return media != null && media.path("is_video").asBoolean(false);
    }

    public boolean hasAudio() {
        JsonNode media = getMedia();
        return media != null && media.path("has_audio").asBoolean(false);
    }

    public String getMediaUrl() {
        String videoUrl = getVideoUrl();
        return videoUrl == null ? getDisplayUrl() : videoUrl;
    }

    public String getVideoUrl() {
        JsonNode media = getMedia();
        return media == null ? null : textOrNull(media.path("video_url"));
    }

    public String getDisplayUrl() {
        JsonNode media = getMedia();
        return media == null ? null : textOrNull(media.path("display_url"));
    }

    public String getThumbnailUrl() {
        JsonNode media = getMedia();
        return media == null ? null : textOrNull(media.path("thumbnail_src"));
    }

    public List<JsonNode> getDisplayResources() {
        JsonNode resources = requireMedia().path("display_resources");
        List<JsonNode> values = new ArrayList<>();
        if (resources.isArray()) {
            resources.forEach(values::add);
        }
        return List.copyOf(values);
    }

    public JsonNode getDimensions() {
        JsonNode dimensions = requireMedia().path("dimensions");
        return dimensions.isMissingNode() || dimensions.isNull() ? null : dimensions;
    }

    public Double getDurationSeconds() {
        JsonNode value = requireMedia().path("video_duration");
        return value.isNumber() ? value.asDouble() : null;
    }

    public Long getViewCount() {
        return longOrNull(requireMedia().path("video_view_count"));
    }

    public Long getPlayCount() {
        return longOrNull(requireMedia().path("video_play_count"));
    }

    public long getLikeCount() {
        return requireMedia().path("edge_media_preview_like").path("count").asLong(0);
    }

    public long getCommentCount() {
        return requireMedia().path("edge_media_to_parent_comment").path("count").asLong(0);
    }

    public String getCaption() {
        JsonNode edges = requireMedia().path("edge_media_to_caption").path("edges");
        if (!edges.isArray() || edges.isEmpty()) {
            return null;
        }
        return textOrNull(edges.get(0).path("node").path("text"));
    }

    public JsonNode getOwner() {
        JsonNode owner = requireMedia().path("owner");
        return owner.isMissingNode() || owner.isNull() ? null : owner;
    }

    public ObjectNode toSummary() {
        JsonNode media = requireMedia();
        ObjectNode summary = OBJECT_MAPPER.createObjectNode();
        putNullable(summary, "id", textOrNull(media.path("id")));
        putNullable(summary, "shortcode", textOrNull(media.path("shortcode")));
        summary.put("isVideo", media.path("is_video").asBoolean(false));
        putNullable(summary, "mediaUrl", getMediaUrl());
        putNullable(summary, "videoUrl", getVideoUrl());
        putNullable(summary, "displayUrl", getDisplayUrl());
        putNullable(summary, "thumbnailUrl", getThumbnailUrl());
        putNullable(summary, "caption", getCaption());
        setNullable(summary, "owner", getOwner());
        setNullable(summary, "dimensions", getDimensions());
        putNullable(summary, "durationSeconds", getDurationSeconds());
        putNullable(summary, "viewCount", getViewCount());
        putNullable(summary, "playCount", getPlayCount());
        summary.put("likeCount", getLikeCount());
        summary.put("commentCount", getCommentCount());
        return summary;
    }

    private static JsonNode mediaNode(JsonNode response) {
        return response == null ? OBJECT_MAPPER.missingNode() : response.path("data").path("xdt_shortcode_media");
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asString();
    }

    private static Long longOrNull(JsonNode node) {
        return node == null || !node.isNumber() ? null : node.asLong();
    }

    private static void putNullable(ObjectNode object, String field, String value) {
        if (value == null) {
            object.putNull(field);
        } else {
            object.put(field, value);
        }
    }

    private static void putNullable(ObjectNode object, String field, Number value) {
        if (value == null) {
            object.putNull(field);
        } else if (value instanceof Double || value instanceof Float) {
            object.put(field, value.doubleValue());
        } else {
            object.put(field, value.longValue());
        }
    }

    private static void setNullable(ObjectNode object, String field, JsonNode value) {
        if (value == null) {
            object.putNull(field);
        } else {
            object.set(field, value);
        }
    }
}
