package dev.mrk.meshingress.storage.workspace;

public record WorkspaceFileRecord(String relativePath, String mimeType, long byteSize, String checksumSha256) { }
