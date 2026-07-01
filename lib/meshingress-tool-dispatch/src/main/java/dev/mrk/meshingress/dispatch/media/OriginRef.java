package dev.mrk.meshingress.dispatch.media;

public record OriginRef(
        String platform,
        String url,
        String externalId
) { }
