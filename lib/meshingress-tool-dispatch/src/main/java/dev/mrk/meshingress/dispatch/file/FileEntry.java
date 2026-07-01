package dev.mrk.meshingress.dispatch.file;

public record FileEntry(
        String name,
        String path,
        String type,
        String mimeType,
        Long sizeBytes,
        String checksum,
        String checksumAlgorithm
) { }
