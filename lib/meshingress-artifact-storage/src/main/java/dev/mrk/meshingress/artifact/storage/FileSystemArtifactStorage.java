package dev.mrk.meshingress.artifact.storage;

import dev.mrk.meshingress.artifact.model.ArtifactChecksum;
import dev.mrk.meshingress.artifact.model.ArtifactCoordinate;
import dev.mrk.meshingress.artifact.model.ArtifactFileEntry;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileSystemArtifactStorage {

    private final RepositoryLayout layout;

    public FileSystemArtifactStorage(Path root) {
        this.layout = new RepositoryLayout(root);
    }

    public RepositoryLayout layout() {
        return layout;
    }

    public StoredArtifactBlob storeArtifact(ArtifactCoordinate coordinate, String fileName, InputStream input) {
        if (input == null) {
            throw new ArtifactStorageException("artifact input is required");
        }
        String safeName = safeFileName(fileName, coordinate.artifactId() + "-" + coordinate.version() + "." + coordinate.packaging());
        Path targetDirectory = layout.artifactDirectory(coordinate);
        Path target = targetDirectory.resolve(safeName).normalize();
        if (!target.startsWith(targetDirectory)) {
            throw new ArtifactStorageException("artifact file name escapes artifact directory");
        }

        try {
            Files.createDirectories(targetDirectory);
            Path temp = Files.createTempFile(targetDirectory, safeName, ".tmp");
            String sha256;
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                try (DigestInputStream digestInput = new DigestInputStream(input, digest);
                     OutputStream output = Files.newOutputStream(temp)) {
                    digestInput.transferTo(output);
                }
                sha256 = HexFormat.of().formatHex(digest.digest());
            } catch (Exception exception) {
                Files.deleteIfExists(temp);
                throw exception;
            }

            if (Files.isRegularFile(target)) {
                String existing = sha256(target);
                if (!existing.equalsIgnoreCase(sha256)) {
                    Files.deleteIfExists(temp);
                    throw new ArtifactStorageException("artifact coordinate already has different content");
                }
                Files.deleteIfExists(temp);
            } else {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            }
            return new StoredArtifactBlob(
                    target,
                    "meshingress-repository://artifact/" + coordinate.groupId() + "/" + coordinate.artifactId() + "/" + coordinate.version() + "/" + safeName,
                    ArtifactChecksum.sha256(sha256),
                    Files.size(target)
            );
        } catch (ArtifactStorageException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ArtifactStorageException("unable to store artifact: " + exception.getMessage(), exception);
        }
    }

    public List<ArtifactFileEntry> extractArchiveToQuarantine(ArtifactCoordinate coordinate, Path archive) {
        Path quarantineRoot = layout.quarantineDirectory(coordinate);
        List<ArtifactFileEntry> entries = new ArrayList<>();
        try {
            Files.createDirectories(quarantineRoot);
            try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    Path target = quarantineRoot.resolve(entry.getName()).normalize();
                    if (!target.startsWith(quarantineRoot)) {
                        throw new ArtifactStorageException("archive entry escapes quarantine root: " + entry.getName());
                    }
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                    entries.add(new ArtifactFileEntry(
                            quarantineRoot.relativize(target).toString().replace('\\', '/'),
                            Files.size(target),
                            ArtifactChecksum.sha256(sha256(target)),
                            isExecutableName(target.getFileName().toString())
                    ));
                }
            }
            return List.copyOf(entries);
        } catch (ArtifactStorageException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ArtifactStorageException("unable to extract artifact to quarantine: " + exception.getMessage(), exception);
        }
    }

    public void cleanQuarantine(ArtifactCoordinate coordinate) {
        Path quarantineRoot = layout.quarantineDirectory(coordinate);
        try {
            if (!Files.exists(quarantineRoot)) {
                return;
            }
            try (var stream = Files.walk(quarantineRoot)) {
                for (Path item : stream.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(item);
                }
            }
        } catch (Exception exception) {
            throw new ArtifactStorageException("unable to clean artifact quarantine: " + exception.getMessage(), exception);
        }
    }

    private String safeFileName(String supplied, String fallback) {
        String fileName = supplied == null || supplied.isBlank() ? fallback : Path.of(supplied).getFileName().toString();
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new ArtifactStorageException("artifact file name is invalid");
        }
        return fileName;
    }

    private boolean isExecutableName(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".exe")
                || lower.endsWith(".bat")
                || lower.endsWith(".cmd")
                || lower.endsWith(".ps1")
                || lower.endsWith(".sh")
                || lower.endsWith(".dll")
                || lower.endsWith(".so")
                || lower.endsWith(".dylib");
    }

    private String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new DigestInputStream(Files.newInputStream(path), digest)) {
            input.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
