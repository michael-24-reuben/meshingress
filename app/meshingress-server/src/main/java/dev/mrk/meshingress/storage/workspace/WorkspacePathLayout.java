package dev.mrk.meshingress.storage.workspace;

import dev.mrk.meshingress.api.storage.ToolStorageException;

import java.nio.file.Path;
import java.util.regex.Pattern;

public final class WorkspacePathLayout {
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");
    private final Path root;

    public WorkspacePathLayout(String root) { this.root = Path.of(root).toAbsolutePath().normalize(); }

    Path stagingDirectory(String sessionId, String requestId) { return contained(root.resolve(".staging").resolve(id(sessionId)).resolve(id(requestId))); }
    Path publishedDirectory(String sessionId, String requestId) { return contained(root.resolve("tools").resolve(id(sessionId)).resolve(id(requestId))); }
    Path stagingFile(String sessionId, String requestId, String relativePath) { return relative(stagingDirectory(sessionId, requestId).resolve("files"), relativePath); }
    Path publishedFile(String sessionId, String requestId, String relativePath) { return relative(publishedDirectory(sessionId, requestId).resolve("files"), relativePath); }
    Path stagingManifest(String sessionId, String requestId) { return stagingFile(sessionId, requestId, "manifest.json"); }

    String relativePath(String value) {
        if (value == null || value.isBlank()) throw new ToolStorageException("A relative file path is required.");
        Path path;
        try { path = Path.of(value.replace('\\', '/')).normalize(); } catch (Exception exception) { throw new ToolStorageException("Invalid relative file path.", exception); }
        if (path.isAbsolute() || path.getNameCount() == 0 || path.startsWith("..") || path.toString().equals(".")) {
            throw new ToolStorageException("File paths must stay inside the workspace.");
        }
        return path.toString().replace('\\', '/');
    }

    private Path relative(Path base, String relativePath) { return contained(base.resolve(relativePath(relativePath))); }
    private String id(String value) {
        if (value == null || !ID.matcher(value).matches()) throw new ToolStorageException("Storage session and request IDs must use letters, digits, dots, underscores, or hyphens.");
        return value;
    }
    private Path contained(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) throw new ToolStorageException("Storage path escapes its root.");
        return normalized;
    }
}
