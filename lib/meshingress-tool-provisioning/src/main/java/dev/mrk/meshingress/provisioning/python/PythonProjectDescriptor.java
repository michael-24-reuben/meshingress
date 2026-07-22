package dev.mrk.meshingress.provisioning.python;

import java.nio.file.Path;
import java.util.List;

/**
 * Deterministic, side-effect-free description of a Python source tree.
 * Lockfiles are recorded for lifecycle evidence; the first pip provider does not pretend it can
 * consume uv, Poetry, or Pipenv lock formats.
 */
public record PythonProjectDescriptor(
        Path sourceRoot,
        Path projectMetadataFile,
        Path requirementsFile,
        Path lockFile,
        boolean installProject,
        List<String> requiredImports
) {
    public PythonProjectDescriptor {
        requiredImports = requiredImports == null ? List.of() : List.copyOf(requiredImports);
    }

    public boolean provisionable() {
        return installProject || requirementsFile != null;
    }
}
