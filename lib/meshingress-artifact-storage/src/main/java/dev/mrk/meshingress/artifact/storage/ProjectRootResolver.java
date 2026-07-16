package dev.mrk.meshingress.artifact.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ProjectRootResolver {

    private static final String ROOT_ARTIFACT_ID = "meshingress";

    private ProjectRootResolver() {
    }

    public static Path resolve(Class<?> applicationClass, Path configuredRoot) {
        if (configuredRoot != null) {
            return validate(configuredRoot);
        }

        Path workingDirectory = Path.of(System.getProperty("user.dir"));
        Path rootFromWorkingDirectory = findRoot(workingDirectory);

        if (rootFromWorkingDirectory != null) {
            return rootFromWorkingDirectory;
        }

        Path codeLocation = resolveCodeLocation(applicationClass);
        Path rootFromCodeLocation = findRoot(codeLocation);

        if (rootFromCodeLocation != null) {
            return rootFromCodeLocation;
        }

        throw new IllegalStateException(
                "Could not locate the Meshingress project root. " +
                "Configure the project root explicitly."
        );
    }

    /**
     * Converts a path below the Meshingress project root into the portable form persisted in
     * registration metadata. Absolute paths must never be stored because a registration can be
     * restored on another machine or from another working directory.
     */
    public static String relativize(Path projectRoot, Path path) {
        Path root = validate(projectRoot);
        Path target = path.toAbsolutePath().normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Path is outside the Meshingress project root: " + target);
        }
        return root.relativize(target).toString().replace('\\', '/');
    }

    /**
     * Resolves an already-persisted project-relative path without allowing it to escape the
     * Meshingress project root. Legacy absolute values are deliberately rejected rather than
     * treated as authority to access an arbitrary host location.
     */
    public static Path resolveRelative(Path projectRoot, String persistedPath) {
        Path root = validate(projectRoot);
        if (persistedPath == null || persistedPath.isBlank()) {
            throw new IllegalArgumentException("Persisted project-relative path is required.");
        }
        Path relative = Path.of(persistedPath);
        if (relative.isAbsolute()) {
            throw new IllegalArgumentException("Persisted path must be project-relative: " + persistedPath);
        }
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Persisted path escapes the Meshingress project root: " + persistedPath);
        }
        return resolved;
    }

    private static Path resolveCodeLocation(Class<?> applicationClass) {
        try {
            Path location = Path.of(
                    applicationClass
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            );

            return Files.isRegularFile(location)
                    ? location.getParent()
                    : location;
        } catch (URISyntaxException exception) {
            throw new IllegalStateException(
                    "Could not resolve application code location",
                    exception
            );
        }
    }

    private static Path findRoot(Path start) {
        Path current = start.toAbsolutePath().normalize();

        while (current != null) {
            Path pom = current.resolve("pom.xml");

            if (Files.isRegularFile(pom) && isMeshingressRootPom(pom)) {
                return current;
            }

            current = current.getParent();
        }

        return null;
    }

    private static Path validate(Path candidate) {
        Path normalized = candidate.toAbsolutePath().normalize();
        Path pom = normalized.resolve("pom.xml");

        if (!Files.isRegularFile(pom) || !isMeshingressRootPom(pom)) {
            throw new IllegalArgumentException(
                    "Configured project root is not the Meshingress root: " +
                    normalized
            );
        }

        return normalized;
    }

    private static boolean isMeshingressRootPom(Path pom) {
        try {
            String xml = Files.readString(pom);

            return xml.contains(
                    "<artifactId>" + ROOT_ARTIFACT_ID + "</artifactId>"
            );
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Failed to inspect root POM: " + pom,
                    exception
            );
        }
    }
}
