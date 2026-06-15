package dev.mrk.meshingress.artifact.scope;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class EmbeddedCycloneDxJarSbomGenerator implements CycloneDxSbomGenerator {
    @Override
    public CycloneDxSbom generate(Path artifact) throws IOException {
        Objects.requireNonNull(artifact, "artifact");

        CycloneDxSbom.Component root = new CycloneDxSbom.Component(
                "file",
                "artifact:" + artifact.getFileName(),
                artifact.getFileName().toString(),
                Files.size(artifact),
                sha256(artifact),
                List.of(
                        new CycloneDxSbom.Property("meshingress:component-role", "root-artifact"),
                        new CycloneDxSbom.Property("meshingress:artifact-kind", "jar")
                )
        );

        return new CycloneDxSbom(root, inventoryComponents(artifact));
    }

    private List<CycloneDxSbom.Component> inventoryComponents(Path artifact) throws IOException {
        List<CycloneDxSbom.Component> components = new ArrayList<>();
        try (JarFile jarFile = new JarFile(artifact.toFile())) {
            List<JarEntry> entries = jarFile.stream()
                    .filter(entry -> !entry.isDirectory())
                    .sorted(Comparator.comparing(JarEntry::getName))
                    .toList();
            for (JarEntry entry : entries) {
                components.add(toComponent(jarFile, entry));
            }
        }
        return List.copyOf(components);
    }

    private CycloneDxSbom.Component toComponent(JarFile jarFile, JarEntry entry) throws IOException {
        return new CycloneDxSbom.Component(
                "file",
                "jar-entry:" + entry.getName(),
                entry.getName(),
                entry.getSize(),
                sha256(jarFile, entry),
                List.of(
                        new CycloneDxSbom.Property("meshingress:component-role", "jar-entry"),
                        new CycloneDxSbom.Property("meshingress:jar-entry:name", entry.getName())
                )
        );
    }

    private String sha256(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return sha256(inputStream);
        }
    }

    private String sha256(JarFile jarFile, JarEntry entry) throws IOException {
        try (InputStream inputStream = jarFile.getInputStream(entry)) {
            return sha256(inputStream);
        }
    }

    private String sha256(InputStream inputStream) throws IOException {
        MessageDigest digest = sha256Digest();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }
}
