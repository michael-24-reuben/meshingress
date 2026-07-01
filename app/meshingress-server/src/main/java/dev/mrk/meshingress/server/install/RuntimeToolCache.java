package dev.mrk.meshingress.server.install;

import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class RuntimeToolCache {

    private final MeshingressProperties properties;
    private final RepositoryArtifactFetcher repositoryArtifactFetcher;

    public RuntimeToolCache(MeshingressProperties properties, RepositoryArtifactFetcher repositoryArtifactFetcher) {
        this.properties = properties;
        this.repositoryArtifactFetcher = repositoryArtifactFetcher;
    }

    public Path install(ArtifactPublicationRecord publication) {
        Path cacheRoot = Path.of(properties.repository().runtimeCacheRoot()).toAbsolutePath().normalize();
        Path targetDirectory = cacheRoot
                .resolve(publication.coordinate().groupId().replace('.', '/'))
                .resolve(publication.coordinate().artifactId())
                .resolve(publication.coordinate().version())
                .normalize();
        if (!targetDirectory.startsWith(cacheRoot)) {
            throw invalidParams("Runtime cache target escapes configured cache root.");
        }

        Path target = targetDirectory.resolve(repositoryArtifactFetcher.artifactFileName(publication)).normalize();
        if (!target.startsWith(targetDirectory)) {
            throw invalidParams("Runtime cache target file escapes coordinate directory.");
        }

        try {
            Files.createDirectories(targetDirectory);
            Path temp = Files.createTempFile(targetDirectory, target.getFileName().toString(), ".tmp");
            try {
                repositoryArtifactFetcher.fetch(publication, temp);
                verifyChecksum(temp, publication.artifactChecksum().value(), "repository artifact checksum mismatch");
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                deleteResources(targetDirectory, cacheRoot);
                repositoryArtifactFetcher.fetchResources(publication, targetDirectory.resolve("resources"));
            } catch (RuntimeException exception) {
                Files.deleteIfExists(temp);
                throw exception;
            } catch (Exception exception) {
                Files.deleteIfExists(temp);
                throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "Unable to copy artifact into runtime cache.");
            }
        } catch (JsonRpcException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "Unable to copy artifact into runtime cache.");
        }
        verifyChecksum(target, publication.artifactChecksum().value(), "runtime cache artifact checksum mismatch");
        return target;
    }

    public void remove(Path cachedJar) {
        Path cacheRoot = Path.of(properties.repository().runtimeCacheRoot()).toAbsolutePath().normalize();
        Path target = cachedJar.toAbsolutePath().normalize();
        if (!target.startsWith(cacheRoot)) {
            throw invalidParams("Runtime cache path escapes configured cache root.");
        }

        try {
            Files.deleteIfExists(target);
            deleteResources(target.getParent(), cacheRoot);
            pruneEmptyParents(target.getParent(), cacheRoot);
        } catch (Exception exception) {
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "Unable to remove artifact from runtime cache.");
        }
    }

    private void verifyChecksum(Path path, String expectedSha256, String message) {
        if (expectedSha256 == null || expectedSha256.isBlank()) {
            throw invalidParams("Publication artifact checksum is required.");
        }
        String actual = sha256(path);
        if (!actual.equalsIgnoreCase(expectedSha256)) {
            throw new JsonRpcException(JsonRpcErrorCodes.FORBIDDEN, message);
        }
    }

    private String sha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = new DigestInputStream(Files.newInputStream(path), digest)) {
                input.transferTo(OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "Unable to calculate artifact checksum.");
        }
    }

    private void pruneEmptyParents(Path current, Path cacheRoot) throws Exception {
        while (current != null && !current.equals(cacheRoot) && current.startsWith(cacheRoot)) {
            if (!isDirectoryEmpty(current)) {
                return;
            }
            Files.deleteIfExists(current);
            current = current.getParent();
        }
    }

    private void deleteResources(Path artifactDirectory, Path cacheRoot) throws Exception {
        if (artifactDirectory == null || !artifactDirectory.startsWith(cacheRoot)) {
            throw invalidParams("Runtime cache resource path escapes configured cache root.");
        }
        Path resources = artifactDirectory.resolve("resources").normalize();
        if (!resources.startsWith(artifactDirectory) || !resources.startsWith(cacheRoot) || !Files.exists(resources)) {
            return;
        }
        try (var stream = Files.walk(resources)) {
            for (Path item : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(item);
            }
        }
    }

    private boolean isDirectoryEmpty(Path directory) throws Exception {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            return !stream.iterator().hasNext();
        }
    }

    private JsonRpcException invalidParams(String message) {
        return new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, message);
    }
}
