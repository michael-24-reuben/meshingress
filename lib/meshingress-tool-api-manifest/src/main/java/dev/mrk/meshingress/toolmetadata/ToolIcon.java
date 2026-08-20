package dev.mrk.meshingress.toolmetadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/** An optional, artifact-local Studio icon. Remote URLs are intentionally not supported. */
public record ToolIcon(String resourcePath, MimeType mimeType, String accessibleLabel) {
    public enum MimeType {
        SVG_IMAGE("image/svg+xml", ".svg"),
        PNG_IMAGE("image/png", ".png"),
        WEBP_IMAGE("image/webp", ".webp");
        private final String mediaType;
        private final String fileExtension;

        MimeType(String mediaType, String fileExtension) {
            this.mediaType = mediaType;
            this.fileExtension = fileExtension;
        }
        public String value() {
            return mediaType;
        }
        public String fileExtension() {
            return fileExtension;
        }
    }

    public static final long MAX_BYTES = 256 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/svg+xml", "image/png", "image/webp");

    public ToolIcon {
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new IllegalArgumentException("tool icon resource path must not be blank");
        }
        resourcePath = resourcePath.replace('\\', '/').strip();
        Path normalized = Path.of(resourcePath).normalize();
        if (normalized.isAbsolute() || resourcePath.startsWith("/") || normalized.startsWith("..")) {
            throw new IllegalArgumentException("tool icon resource path must stay within artifact resources");
        }
        resourcePath = normalized.toString().replace('\\', '/');
        mimeType = mimeType == null ? ToolIcon.MimeType.SVG_IMAGE : mimeType;
        if (!ALLOWED_MIME_TYPES.contains(mimeType.value())) {
            throw new IllegalArgumentException("tool icon MIME type is not supported: " + mimeType);
        }
        accessibleLabel = accessibleLabel == null ? "" : accessibleLabel.strip();
    }

    public Path resolveWithin(Path resourcesDirectory) {
        Path root = resourcesDirectory.toAbsolutePath().normalize();
        Path candidate = root.resolve(resourcePath).normalize();
        if (!candidate.startsWith(root)) {
            throw new IllegalArgumentException("tool icon resource path escapes artifact resources");
        }
        return candidate;
    }

    public Path requireValidResource(Path resourcesDirectory) {
        Path candidate = resolveWithin(resourcesDirectory);
        try {
            if (!Files.isRegularFile(candidate) || Files.size(candidate) > MAX_BYTES) {
                throw new IllegalArgumentException("tool icon resource is missing or exceeds " + MAX_BYTES + " bytes");
            }
            return candidate;
        } catch (IOException exception) {
            throw new IllegalArgumentException("unable to inspect tool icon resource", exception);
        }
    }

    /**
     * Checks the convention that an icon file is named after its module namespace.
     * Directories are deliberately not prescribed, so both {@code youtube.svg} and
     * {@code branding/youtube.svg} satisfy the convention for {@code youtube}.
     */
    public boolean hasNamespaceFileName(String namespace) {
        if (namespace == null || namespace.isBlank()) return false;
        return Path.of(resourcePath).getFileName().toString().equals(namespace + mimeType.fileExtension());
    }
}
