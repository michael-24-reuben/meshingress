package dev.mrk.toolspace.storage.workspace;

public record WorkspaceFileRecord(String relativePath, String mimeType, long byteSize, String checksumSha256) { }
