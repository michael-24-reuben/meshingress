package dev.mrk.meshingress.server.install;

import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class RuntimeToolCache {

    private final MeshingressProperties properties;

    public RuntimeToolCache(MeshingressProperties properties) {
        this.properties = properties;
    }

    public Path install(ArtifactPublicationRecord publication) {
        Path source = resolveRepositoryArtifact(publication);
        verifyChecksum(source, publication.artifactChecksum().value(), "repository artifact checksum mismatch");

        Path cacheRoot = Path.of(properties.repository().runtimeCacheRoot()).toAbsolutePath().normalize();
        Path targetDirectory = cacheRoot
                .resolve(publication.coordinate().groupId().replace('.', '/'))
                .resolve(publication.coordinate().artifactId())
                .resolve(publication.coordinate().version())
                .normalize();
        if (!targetDirectory.startsWith(cacheRoot)) {
            throw invalidParams("Runtime cache target escapes configured cache root.");
        }

        Path target = targetDirectory.resolve(source.getFileName()).normalize();
        if (!target.startsWith(targetDirectory)) {
            throw invalidParams("Runtime cache target file escapes coordinate directory.");
        }

        try {
            Files.createDirectories(targetDirectory);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception exception) {
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "Unable to copy artifact into runtime cache.");
        }
        verifyChecksum(target, publication.artifactChecksum().value(), "runtime cache artifact checksum mismatch");
        return target;
    }

    private Path resolveRepositoryArtifact(ArtifactPublicationRecord publication) {
        URI uri = URI.create(publication.artifactUri());
        if (!"meshingress-repository".equals(uri.getScheme()) || !"artifact".equals(uri.getAuthority())) {
            throw invalidParams("Publication artifactUri must use meshingress-repository://artifact/.");
        }

        String path = uri.getPath() == null ? "" : uri.getPath();
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == path.length() - 1) {
            throw invalidParams("Publication artifactUri is missing an artifact file name.");
        }
        String fileName = path.substring(lastSlash + 1);
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..") || !fileName.endsWith(".jar")) {
            throw invalidParams("Publication artifact file name is invalid.");
        }

        Path root = Path.of(properties.repository().root()).toAbsolutePath().normalize();
        Path artifact = root
                .resolve("artifacts")
                .resolve(publication.coordinate().groupId().replace('.', '/'))
                .resolve(publication.coordinate().artifactId())
                .resolve(publication.coordinate().version())
                .resolve(fileName)
                .normalize();
        if (!artifact.startsWith(root.resolve("artifacts").normalize())) {
            throw invalidParams("Publication artifact path escapes repository root.");
        }
        if (!Files.isRegularFile(artifact)) {
            throw invalidParams("Publication artifact does not exist in repository storage.");
        }
        return artifact;
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
            try (InputStream input = new DigestInputStream(Files.newInputStream(path), digest)) {
                input.transferTo(OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "Unable to calculate artifact checksum.");
        }
    }

    private JsonRpcException invalidParams(String message) {
        return new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, message);
    }
}
