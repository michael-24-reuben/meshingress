package dev.mrk.meshingress.provisioning.python;

import dev.mrk.meshingress.provisioning.ProvisionedResource;
import dev.mrk.meshingress.provisioning.ProvisioningEvidence;
import dev.mrk.meshingress.provisioning.ProvisioningResult;
import dev.mrk.meshingress.provisioning.ProvisioningStatus;
import dev.mrk.meshingress.provisioning.ToolProvisioner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Provisions a local Python source tree without writing into that source tree. Package installation
 * always runs from a disposable copy, which protects the uploaded/vendor repository from setup.py
 * build artefacts such as egg-info directories.
 */
public final class PythonVenvProvisioner implements ToolProvisioner<PythonVenvProvisioningRequest> {
    private static final int MAX_OUTPUT = 12_000;

    @Override
    public Class<PythonVenvProvisioningRequest> requestType() {
        return PythonVenvProvisioningRequest.class;
    }

    @Override
    public ProvisioningResult provision(PythonVenvProvisioningRequest request) {
        if (request == null || request.sourceRoot() == null || request.virtualEnvironmentRoot() == null) {
            return failed("validate", "sourceRoot and virtualEnvironmentRoot are required", List.of());
        }
        Path sourceRoot = request.sourceRoot().toAbsolutePath().normalize();
        Path virtualEnvironmentRoot = request.virtualEnvironmentRoot().toAbsolutePath().normalize();
        request = new PythonVenvProvisioningRequest(
                request.requirementId(),
                sourceRoot,
                virtualEnvironmentRoot,
                request.bootstrapPython(),
                request.requiredImports(),
                request.explicitAdditionalDistributions(),
                request.requirementsFile(),
                request.installProject(),
                request.timeout()
        );
        if (!Files.isDirectory(request.sourceRoot())) {
            return failed("validate", "Python source root does not exist: " + request.sourceRoot(), List.of());
        }
        if (request.requirementsFile() != null
                && (!request.requirementsFile().toAbsolutePath().normalize().startsWith(request.sourceRoot())
                || !Files.isRegularFile(request.requirementsFile()))) {
            return failed("validate", "Python requirements file must be a regular file inside sourceRoot.", List.of());
        }

        Path interpreter = venvPython(request.virtualEnvironmentRoot());
        List<ProvisioningEvidence> evidence = new ArrayList<>();
        Path staging = null;
        try {
            if (Files.isRegularFile(interpreter) && importsWork(interpreter, request, evidence)) {
                return ready(ProvisioningStatus.REUSED, request, interpreter, evidence, "Verified existing virtual environment.");
            }

            staging = request.virtualEnvironmentRoot().getParent()
                    .resolve(".provisioning")
                    .resolve("staging")
                    // Keep this path short: Windows ProcessBuilder still rejects a working directory
                    // whose expanded path exceeds the legacy process-creation limit. The requirement
                    // identity is already represented by the enclosing runtime directory.
                    .resolve("stage-" + UUID.randomUUID());
            copySource(request.sourceRoot(), staging);
            evidence.add(new ProvisioningEvidence("stage", "Copied source into " + staging, Duration.ZERO, true));

            run("create-venv", List.of(bootstrapPython(request), "-m", "venv", request.virtualEnvironmentRoot().toString()),
                    request.sourceRoot(), request.timeout(), evidence);
            run("upgrade-pip", List.of(interpreter.toString(), "-m", "pip", "install", "--upgrade", "pip"),
                    staging, request.timeout(), evidence);
            if (request.requirementsFile() != null) {
                Path stagedRequirements = staging.resolve(request.sourceRoot().relativize(request.requirementsFile().toAbsolutePath().normalize()));
                run("install-requirements", List.of(interpreter.toString(), "-m", "pip", "install", "--no-cache-dir", "-r", stagedRequirements.toString()),
                        staging, request.timeout(), evidence);
            }
            if (request.installProject()) {
                run("install-project", List.of(interpreter.toString(), "-m", "pip", "install", "--no-cache-dir", staging.toString()),
                        staging, request.timeout(), evidence);
            }
            if (!request.explicitAdditionalDistributions().isEmpty()) {
                List<String> command = new ArrayList<>(List.of(interpreter.toString(), "-m", "pip", "install", "--no-cache-dir"));
                command.addAll(request.explicitAdditionalDistributions());
                run("install-explicit-additions", command, staging, request.timeout(), evidence);
            }
            if (!importsWork(interpreter, request, evidence)) {
                return failed("verify", "Required Python imports did not pass verification.", evidence);
            }
            return ready(ProvisioningStatus.READY, request, interpreter, evidence, "Created and verified virtual environment.");
        } catch (Exception exception) {
            evidence.add(new ProvisioningEvidence("failed", exception.getMessage(), Duration.ZERO, false));
            return failed("failed", "Python environment provisioning failed: " + exception.getMessage(), evidence);
        } finally {
            if (staging != null) {
                deleteTree(staging);
            }
        }
    }

    private static boolean importsWork(
            Path interpreter,
            PythonVenvProvisioningRequest request,
            List<ProvisioningEvidence> evidence
    ) throws IOException, InterruptedException {
        for (String requiredImport : request.requiredImports()) {
            if (requiredImport == null || requiredImport.isBlank()) {
                continue;
            }
            try {
                run("verify-import", List.of(interpreter.toString(), "-c", "import " + requiredImport.trim()),
                        request.sourceRoot(), request.timeout(), evidence);
            } catch (IllegalStateException failure) {
                return false;
            }
        }
        return true;
    }

    private static void copySource(Path source, Path destination) throws IOException {
        try (var paths = Files.walk(source)) {
            paths.forEach(path -> {
                Path relative = source.relativize(path);
                if (relative.getNameCount() > 0 && ".git".equals(relative.getName(0).toString())) {
                    return;
                }
                Path target = destination.resolve(relative);
                try {
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    }
                } catch (IOException exception) {
                    throw new ProvisioningCopyException(exception);
                }
            });
        } catch (ProvisioningCopyException exception) {
            throw exception.getCause();
        }
    }

    private static void run(
            String phase,
            List<String> command,
            Path workingDirectory,
            Duration timeout,
            List<ProvisioningEvidence> evidence
    ) throws IOException, InterruptedException {
        Instant started = Instant.now();
        Path output = Files.createTempFile("meshingress-provisioning-", ".log");
        try {
            Process process = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(output.toFile())
                    .start();
            boolean completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            String log = truncate(Files.readString(output, StandardCharsets.UTF_8));
            if (!completed) {
                process.destroyForcibly();
                evidence.add(new ProvisioningEvidence(phase, "Timed out after " + timeout, Duration.between(started, Instant.now()), false));
                throw new IllegalStateException(phase + " timed out");
            }
            boolean successful = process.exitValue() == 0;
            evidence.add(new ProvisioningEvidence(phase, log, Duration.between(started, Instant.now()), successful));
            if (!successful) {
                throw new IllegalStateException(phase + " exited with " + process.exitValue() + ": " + log);
            }
        } finally {
            Files.deleteIfExists(output);
        }
    }

    private static String bootstrapPython(PythonVenvProvisioningRequest request) {
        return request.bootstrapPython() == null || request.bootstrapPython().isBlank() ? "python" : request.bootstrapPython().trim();
    }

    private static Path venvPython(Path root) {
        return root.resolve(System.getProperty("os.name").toLowerCase().contains("win")
                ? "Scripts/python.exe"
                : "bin/python");
    }

    private static ProvisioningResult ready(
            ProvisioningStatus status,
            PythonVenvProvisioningRequest request,
            Path interpreter,
            List<ProvisioningEvidence> evidence,
            String summary
    ) {
        return new ProvisioningResult(status, new ProvisionedResource(
                request.sourceRoot(), request.virtualEnvironmentRoot(), interpreter, request.requirementId(),
                Map.of("runtime", "python-venv")
        ), evidence, List.of(), summary);
    }

    private static ProvisioningResult failed(String phase, String summary, List<ProvisioningEvidence> evidence) {
        return new ProvisioningResult(ProvisioningStatus.FAILED, null, evidence, List.of(), phase + ": " + summary);
    }

    private static String truncate(String value) {
        return value.length() <= MAX_OUTPUT ? value : value.substring(0, MAX_OUTPUT);
    }

    private static void deleteTree(Path root) {
        try (var paths = Files.walk(root)) {
            paths.sorted((left, right) -> right.getNameCount() - left.getNameCount()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // The incomplete workspace has no ready marker and therefore cannot be reused.
                }
            });
        } catch (IOException ignored) {
            // Cleanup is best effort; readiness remains fail-closed.
        }
    }

    private static final class ProvisioningCopyException extends RuntimeException {
        private ProvisioningCopyException(IOException cause) {
            super(cause);
        }

        @Override
        public IOException getCause() {
            return (IOException) super.getCause();
        }
    }
}
