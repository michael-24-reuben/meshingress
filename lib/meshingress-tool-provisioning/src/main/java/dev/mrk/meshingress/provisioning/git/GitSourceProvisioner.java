package dev.mrk.meshingress.provisioning.git;

import dev.mrk.meshingress.provisioning.ProvisionedResource;
import dev.mrk.meshingress.provisioning.ProvisioningEvidence;
import dev.mrk.meshingress.provisioning.ProvisioningResult;
import dev.mrk.meshingress.provisioning.ProvisioningStatus;
import dev.mrk.meshingress.provisioning.ToolProvisioner;
import dev.mrk.meshingress.artifact.storage.RepositoryLayout;

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
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Materializes a trusted source requirement under {@code repository/vendor/<canonical-identity>}.
 * A ready marker is written only after a detached commit checkout and optional subdirectory check
 * have succeeded. Mutable refs are fetched again rather than being reused from a stale checkout.
 */
public final class GitSourceProvisioner implements ToolProvisioner<GitSourceProvisioningRequest> {
    private static final String MARKER_FILE = ".meshingress-source.properties";
    private static final String MARKER_SCHEMA_VERSION = "1";
    private static final int MAX_OUTPUT = 4_000;

    @Override
    public Class<GitSourceProvisioningRequest> requestType() {
        return GitSourceProvisioningRequest.class;
    }

    @Override
    public ProvisioningResult provision(GitSourceProvisioningRequest request) {
        if (request == null) {
            return failed("validate", "Git source request is required.", List.of());
        }

        List<ProvisioningEvidence> evidence = new ArrayList<>();
        Path repositoryRoot = request.repositoryRoot().toAbsolutePath().normalize();
        Path vendorRoot = new RepositoryLayout(repositoryRoot).vendorRoot();
        Path checkoutRoot;
        try {
            checkoutRoot = checkoutRoot(vendorRoot, request.canonicalIdentity());
        } catch (IllegalArgumentException exception) {
            return failed("validate", exception.getMessage(), evidence);
        }

        try {
            if (canReuse(checkoutRoot, request, evidence)) {
                return ready(ProvisioningStatus.REUSED, request, checkoutRoot, marker(checkoutRoot), evidence,
                        "Reused verified immutable Git checkout.");
            }
            if (Files.exists(checkoutRoot) && marker(checkoutRoot).isEmpty()) {
                return failed("prepare", "Managed checkout exists without a ready marker: " + checkoutRoot, evidence);
            }

            Path staging = repositoryRoot.resolve(".provisioning").resolve("staging")
                    .resolve("git-" + UUID.randomUUID()).normalize();
            if (!staging.startsWith(repositoryRoot)) {
                return failed("validate", "Git staging path escapes the repository root.", evidence);
            }
            try {
                Files.createDirectories(staging.getParent());
                run("clone", List.of("git", "clone", "--quiet", request.cloneUrl(), staging.toString()), repositoryRoot, request.timeout(), evidence);
                run("fetch", gitCommand(staging, "fetch", "--quiet", "--tags", "origin"), staging, request.timeout(), evidence);
                String commit = output("resolve-ref", gitCommand(staging, "rev-parse", "--verify", request.checkoutRef() + "^{commit}"), staging,
                        request.timeout(), evidence);
                run("checkout", gitCommand(staging, "checkout", "--quiet", "--detach", commit), staging, request.timeout(), evidence);
                commit = output("resolved-commit", gitCommand(staging, "rev-parse", "HEAD"), staging, request.timeout(), evidence);
                Path sourceRoot = sourceRoot(staging, request.subdirectory());
                if (!Files.isDirectory(sourceRoot)) {
                    return failed("verify", "Requested source subdirectory does not exist: " + request.subdirectory(), evidence);
                }
                writeMarker(staging, request, commit);
                replaceCheckout(staging, checkoutRoot, repositoryRoot);
                evidence.add(new ProvisioningEvidence("ready", "Prepared managed Git source " + request.canonicalIdentity(), Duration.ZERO, true));
                return ready(ProvisioningStatus.READY, request, checkoutRoot, marker(checkoutRoot), evidence,
                        "Resolved and verified Git source checkout.");
            } finally {
                deleteTree(staging);
            }
        } catch (Exception exception) {
            evidence.add(new ProvisioningEvidence("failed", exception.getMessage(), Duration.ZERO, false));
            return failed("source", "Git source provisioning failed: " + exception.getMessage(), evidence);
        }
    }

    private static boolean canReuse(
            Path checkoutRoot,
            GitSourceProvisioningRequest request,
            List<ProvisioningEvidence> evidence
    ) throws IOException, InterruptedException {
        if (!Files.isDirectory(checkoutRoot) || !isImmutableCommit(request.checkoutRef())) {
            return false;
        }
        Properties marker = marker(checkoutRoot);
        if (!MARKER_SCHEMA_VERSION.equals(marker.getProperty("schemaVersion"))
                || !request.canonicalIdentity().equals(marker.getProperty("canonicalIdentity"))
                || !request.checkoutRef().equalsIgnoreCase(marker.getProperty("requestedRef", ""))) {
            return false;
        }
        String expectedCommit = marker.getProperty("resolvedCommit", "");
        if (expectedCommit.isBlank()) {
            return false;
        }
        String actualCommit = output("reuse-verify", gitCommand(checkoutRoot, "rev-parse", "HEAD"), checkoutRoot, request.timeout(), evidence);
        boolean matches = expectedCommit.equalsIgnoreCase(actualCommit);
        evidence.add(new ProvisioningEvidence("reuse", matches ? "Verified immutable managed checkout." : "Ready marker commit mismatch.", Duration.ZERO, matches));
        return matches;
    }

    private static Path checkoutRoot(Path vendorRoot, String canonicalIdentity) {
        Path target = vendorRoot;
        for (String segment : canonicalIdentity.split("/")) {
            if (!segment.matches("[A-Za-z0-9._-]+") || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("Canonical source identity contains an unsafe path segment.");
            }
            target = target.resolve(segment);
        }
        target = target.normalize();
        if (!target.startsWith(vendorRoot)) {
            throw new IllegalArgumentException("Canonical source identity escapes the vendor root.");
        }
        return target;
    }

    private static Path sourceRoot(Path checkoutRoot, Path subdirectory) {
        if (subdirectory == null) {
            return checkoutRoot;
        }
        if (subdirectory.isAbsolute()) {
            throw new IllegalArgumentException("Source subdirectory must be relative.");
        }
        Path sourceRoot = checkoutRoot.resolve(subdirectory).normalize();
        if (!sourceRoot.startsWith(checkoutRoot)) {
            throw new IllegalArgumentException("Source subdirectory escapes the managed checkout.");
        }
        return sourceRoot;
    }

    private static void writeMarker(Path checkoutRoot, GitSourceProvisioningRequest request, String commit) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("schemaVersion", MARKER_SCHEMA_VERSION);
        properties.setProperty("canonicalIdentity", request.canonicalIdentity());
        properties.setProperty("requestedRef", request.checkoutRef());
        properties.setProperty("resolvedCommit", commit);
        try (var output = Files.newOutputStream(checkoutRoot.resolve(MARKER_FILE))) {
            properties.store(output, "Meshingress managed source checkout");
        }
    }

    private static Properties marker(Path checkoutRoot) throws IOException {
        Properties properties = new Properties();
        Path marker = checkoutRoot.resolve(MARKER_FILE);
        if (Files.isRegularFile(marker)) {
            try (var input = Files.newInputStream(marker)) {
                properties.load(input);
            }
        }
        return properties;
    }

    private static ProvisioningResult ready(
            ProvisioningStatus status,
            GitSourceProvisioningRequest request,
            Path checkoutRoot,
            Properties marker,
            List<ProvisioningEvidence> evidence,
            String summary
    ) {
        Path sourceRoot = sourceRoot(checkoutRoot, request.subdirectory());
        return new ProvisioningResult(status, new ProvisionedResource(
                sourceRoot,
                checkoutRoot,
                null,
                request.canonicalIdentity() + "@" + marker.getProperty("resolvedCommit", ""),
                Map.of(
                        "canonicalIdentity", request.canonicalIdentity(),
                        "requestedRef", request.checkoutRef(),
                        "resolvedCommit", marker.getProperty("resolvedCommit", "")
                )
        ), evidence, List.of(), summary);
    }

    private static ProvisioningResult failed(String phase, String summary, List<ProvisioningEvidence> evidence) {
        return new ProvisioningResult(ProvisioningStatus.FAILED, null, evidence, List.of(), phase + ": " + summary);
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException unsupportedAtomicMove) {
            Files.move(source, target);
        }
    }

    private static void replaceCheckout(Path staging, Path target, Path repositoryRoot) throws IOException {
        Path backup = null;
        if (Files.exists(target)) {
            backup = repositoryRoot.resolve(".provisioning").resolve("replaced")
                    .resolve("git-" + UUID.randomUUID()).normalize();
            if (!backup.startsWith(repositoryRoot)) {
                throw new IOException("Managed source backup escapes the repository root.");
            }
            Files.createDirectories(backup.getParent());
            move(target, backup);
        }
        try {
            Files.createDirectories(target.getParent());
            move(staging, target);
        } catch (IOException failure) {
            if (backup != null && Files.exists(backup) && !Files.exists(target)) {
                move(backup, target);
            }
            throw failure;
        } finally {
            deleteTree(backup);
        }
    }

    private static void run(
            String phase,
            List<String> command,
            Path workingDirectory,
            Duration timeout,
            List<ProvisioningEvidence> evidence
    ) throws IOException, InterruptedException {
        output(phase, command, workingDirectory, timeout, evidence);
    }

    /**
     * Git refuses to inspect repositories on filesystems that do not retain ownership metadata
     * unless the repository is explicitly trusted.  Keep that trust process-local and scoped to
     * the managed checkout; provisioning must not mutate the host's global Git configuration.
     */
    private static List<String> gitCommand(Path repository, String... arguments) {
        List<String> command = new ArrayList<>(List.of(
                "git",
                "-c",
                "safe.directory=" + repository.toAbsolutePath().normalize().toString().replace('\\', '/')
        ));
        command.addAll(List.of(arguments));
        return command;
    }

    private static String output(
            String phase,
            List<String> command,
            Path workingDirectory,
            Duration timeout,
            List<ProvisioningEvidence> evidence
    ) throws IOException, InterruptedException {
        Instant started = Instant.now();
        Path log = Files.createTempFile("meshingress-git-provisioning-", ".log");
        try {
            Process process = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(log.toFile())
                    .start();
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                evidence.add(new ProvisioningEvidence(phase, "Timed out after " + timeout, Duration.between(started, Instant.now()), false));
                throw new IllegalStateException(phase + " timed out");
            }
            String output = Files.readString(log, StandardCharsets.UTF_8).trim();
            boolean successful = process.exitValue() == 0;
            evidence.add(new ProvisioningEvidence(phase, successful ? "Git command completed." : truncate(output),
                    Duration.between(started, Instant.now()), successful));
            if (!successful) {
                throw new IllegalStateException(phase + " exited with " + process.exitValue());
            }
            return output;
        } finally {
            Files.deleteIfExists(log);
        }
    }

    private static boolean isImmutableCommit(String ref) {
        return ref.matches("(?i)[0-9a-f]{7,40}");
    }

    private static String truncate(String value) {
        return value.length() <= MAX_OUTPUT ? value : value.substring(0, MAX_OUTPUT);
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        List<Path> paths;
        try (var files = Files.walk(root)) {
            paths = files.sorted((left, right) -> right.getNameCount() - left.getNameCount()).toList();
        } catch (IOException ignored) {
            return;
        }
        try {
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // A staging checkout never carries a ready marker at the target path, so it cannot be reused.
        }
    }
}
