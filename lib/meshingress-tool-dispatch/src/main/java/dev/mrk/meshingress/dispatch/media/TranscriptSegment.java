package dev.mrk.meshingress.dispatch.media;

public record TranscriptSegment(
        Long startMs,
        Long endMs,
        String speaker,
        String text
) { }
