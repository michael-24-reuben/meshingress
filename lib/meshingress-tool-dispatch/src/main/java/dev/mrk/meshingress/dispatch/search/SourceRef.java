package dev.mrk.meshingress.dispatch.search;

public record SourceRef(
        String type,
        String id,
        String path,
        String url,
        String title
) { }
