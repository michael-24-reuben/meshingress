package dev.mrk.meshingress.dispatch.media;

public record MediaItemRef(
        String kind,
        String id,
        String title,
        String url,
        String mimeType,
        ImageRef thumbnail,
        Long durationMs
) { }
