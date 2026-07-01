package dev.mrk.meshingress.dispatch.media;

public record ImageRef(
        String url,
        String mimeType,
        Integer width,
        Integer height,
        String alt
) { }
