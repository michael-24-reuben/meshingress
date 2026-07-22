package dev.mrk.meshingress.storage.workspace;

import dev.mrk.meshingress.api.storage.ToolStorageException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;

public final class WorkspaceFiles {
    record WrittenFile(long byteSize, String checksumSha256) {
    }

    private final WorkspacePathLayout paths;

    public WorkspaceFiles(WorkspacePathLayout paths) {
        this.paths = paths;
    }

    WrittenFile write(String sessionId, String requestId, String relativePath, InputStream content, long byteLimit) throws IOException {
        if (content == null)
            throw new ToolStorageException("File content is required.");
        var target = paths.stagingFile(sessionId, requestId, relativePath);
        Files.createDirectories(target.getParent());
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception exception) {
            throw new IOException("SHA-256 is unavailable.", exception);
        }

        long size = 0;
        try (var output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[16 * 1024];
            for (int read; (read = content.read(buffer)) != -1; ) {
                size += read;
                if (size > byteLimit) throw new ToolStorageException("Workspace content exceeds the configured size limit.");
                digest.update(buffer, 0, read);
                output.write(buffer, 0, read);
            }
        } catch (Exception exception) {
            Files.deleteIfExists(target);
            throw exception;
        }
        return new WrittenFile(size, HexFormat.of().formatHex(digest.digest()));
    }

    void publish(String sessionId, String requestId) throws IOException {
        var source = paths.stagingDirectory(sessionId, requestId);
        var target = paths.publishedDirectory(sessionId, requestId);
        Files.createDirectories(target.getParent());
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    }

    InputStream open(String sessionId, String requestId, String relativePath) throws IOException {
        return Files.newInputStream(paths.publishedFile(sessionId, requestId, relativePath), StandardOpenOption.READ);
    }

    InputStream openStaging(String sessionId, String requestId, String relativePath) throws IOException {
        return Files.newInputStream(paths.stagingFile(sessionId, requestId, relativePath), StandardOpenOption.READ);
    }

    long size(String sessionId, String requestId, String relativePath) throws IOException {
        return Files.size(paths.publishedFile(sessionId, requestId, relativePath));
    }

    void deleteStaging(String sessionId, String requestId) throws IOException {
        deleteTree(paths.stagingDirectory(sessionId, requestId));
    }

    void deletePublished(String sessionId, String requestId) throws IOException {
        deleteTree(paths.publishedDirectory(sessionId, requestId));
    }

    private void deleteTree(java.nio.file.Path directory) throws IOException {
        if (!Files.exists(directory)) return;
        try (var stream = Files.walk(directory)) {
            for (var path : stream.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
