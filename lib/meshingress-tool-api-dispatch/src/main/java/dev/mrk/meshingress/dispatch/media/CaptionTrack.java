package dev.mrk.meshingress.dispatch.media;

public record CaptionTrack(
        String language,
        String label,
        String url,
        String mimeType,
        String text
) { }
