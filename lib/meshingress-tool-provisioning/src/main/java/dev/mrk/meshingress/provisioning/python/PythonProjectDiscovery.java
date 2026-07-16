package dev.mrk.meshingress.provisioning.python;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Recognizes the small, safe pip-compatible Python project surface supported by this provider. */
public final class PythonProjectDiscovery {

    private PythonProjectDiscovery() {
    }

    public static Optional<PythonProjectDescriptor> discover(Path root) {
        if (root == null || !Files.isDirectory(root)) {
            return Optional.empty();
        }
        Path sourceRoot = root.toAbsolutePath().normalize();
        Path projectMetadata = firstRegular(sourceRoot, "pyproject.toml", "setup.py", "setup.cfg");
        Path requirements = regular(sourceRoot.resolve("requirements.txt"));
        boolean installProject = projectMetadata != null;
        if (!installProject && requirements == null) {
            return Optional.empty();
        }
        // A package owns its declared dependencies. Script-only projects use requirements.txt.
        Path selectedRequirements = installProject ? null : requirements;
        return Optional.of(new PythonProjectDescriptor(
                sourceRoot,
                projectMetadata,
                selectedRequirements,
                firstRegular(sourceRoot, "uv.lock", "poetry.lock", "Pipfile.lock"),
                installProject,
                importPackages(sourceRoot)
        ));
    }

    private static Path firstRegular(Path root, String... names) {
        for (String name : names) {
            Path candidate = regular(root.resolve(name));
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private static Path regular(Path candidate) {
        return Files.isRegularFile(candidate) ? candidate : null;
    }

    private static List<String> importPackages(Path root) {
        Path packageRoot = Files.isDirectory(root.resolve("src")) ? root.resolve("src") : root;
        try (var paths = Files.list(packageRoot)) {
            return paths
                    .filter(Files::isDirectory)
                    .filter(path -> Files.isRegularFile(path.resolve("__init__.py")))
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.matches("[A-Za-z_][A-Za-z0-9_]*"))
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException ignored) {
            return List.of();
        }
    }
}
