package dev.mrk.meshingress.api.storage;

public record ToolStorageFile(
        String relativePath,
        String uri,
        String mimeType,
        long byteSize,
        String checksumSha256
) { }
