package dev.mrk.meshingress.dispatch.media;

public record PersonRef(
        String id,
        String username,
        String displayName,
        String avatarUrl,
        String url
) { }
