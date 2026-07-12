package dev.mrk.meshingress.toolmetadata;

import java.net.URI;

public record ToolLink(
        String kind,
        String url,
        String label
) {
    public ToolLink {
        kind = cleanRequired(kind, "tool link kind");
        url = cleanRequired(url, "tool link URL");
        URI.create(url);
        label = label == null ? "" : label.strip();
    }

    public static ToolLink documentation(String url) {
        return new ToolLink("documentation", url, "");
    }

    public static ToolLink source(String url) {
        return new ToolLink("source", url, "");
    }

    public static ToolLink download(String url) {
        return new ToolLink("download", url, "");
    }

    public static ToolLink apiDocs(String url) {
        return new ToolLink("apiDocs", url, "");
    }

    public static ToolLink terms(String url) {
        return new ToolLink("terms", url, "");
    }

    public ToolLink label(String value) {
        return new ToolLink(kind, url, value);
    }

    private static String cleanRequired(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }
}
