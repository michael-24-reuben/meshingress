package dev.mrk.meshingress.runtime.provisioning;

import dev.mrk.meshingress.provisioning.ProvisioningResult;
import dev.mrk.meshingress.provisioning.ToolProvisioningService;
import dev.mrk.meshingress.provisioning.git.GitSourceProvisioningRequest;
import dev.mrk.meshingress.provisioning.python.PythonProjectDescriptor;
import dev.mrk.meshingress.provisioning.python.PythonProjectDiscovery;
import dev.mrk.meshingress.provisioning.python.PythonVenvProvisioningRequest;
import dev.mrk.meshingress.runtime.artifacts.ResolvedToolArtifact;
import dev.mrk.meshingress.toolmetadata.McpToolManifestJson;
import dev.mrk.meshingress.toolmetadata.McpToolNativeMetadata;
import dev.mrk.meshingress.toolmetadata.ToolRequirement;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Applies required source-repository requirements from the static manifest copied beside a cached
 * tool artifact. A missing static manifest remains compatible with legacy artifacts.
 */
public final class StaticManifestToolProvisioningGate implements ToolProvisioningGate {

    private static final Pattern COMMIT_ID = Pattern.compile("(?i)^[0-9a-f]{7,40}$");

    private final Path repositoryRoot;
    private final ToolProvisioningService provisioningService;
    private final Duration timeout;

    public StaticManifestToolProvisioningGate(Path repositoryRoot, ToolProvisioningService provisioningService) {
        this(repositoryRoot, provisioningService, Duration.ofMinutes(2));
    }

    public StaticManifestToolProvisioningGate(
            Path repositoryRoot,
            ToolProvisioningService provisioningService,
            Duration timeout
    ) {
        this.repositoryRoot = Objects.requireNonNull(repositoryRoot, "repositoryRoot must not be null");
        this.provisioningService = Objects.requireNonNull(provisioningService, "provisioningService must not be null");
        this.timeout = timeout == null || timeout.isZero() || timeout.isNegative() ? Duration.ofMinutes(2) : timeout;
    }

    @Override
    public Map<String, String> requireReady(ResolvedToolArtifact artifact) {
        Objects.requireNonNull(artifact, "artifact must not be null");
        Path manifestPath = artifact.mainJar().getParent().resolve(McpToolManifestJson.RESOURCES_MANIFEST_PATH);
        if (!Files.isRegularFile(manifestPath)) {
            return Map.of();
        }

        McpToolNativeMetadata metadata = McpToolManifestJson.read(manifestPath);
        Map<String, String> runtimeProperties = new LinkedHashMap<>();
        metadata.requirements().stream()
                .filter(requirement -> requirement.required() && "sourceRepository".equals(requirement.kind()))
                .forEach(requirement -> provisionRequiredSource(metadata, requirement, runtimeProperties));
        return Map.copyOf(runtimeProperties);
    }

    private void provisionRequiredSource(
            McpToolNativeMetadata metadata,
            ToolRequirement requirement,
            Map<String, String> runtimeProperties
    ) {
        String toolId = metadata.toolId();
        String checkoutRef = requirement.checkoutRef();
        if (!COMMIT_ID.matcher(checkoutRef).matches()) {
            throw new IllegalStateException("Required source repository for " + toolId
                    + " must declare an immutable commit checkoutRef: " + requirement.name());
        }
        String cloneUrl = requirement.cloneUrl().isBlank() ? requirement.name() : requirement.cloneUrl();
        if (requirement.canonicalIdentity().isBlank()) {
            throw new IllegalStateException("Required source repository for " + toolId
                    + " must declare a canonical identity: " + requirement.name());
        }

        ProvisioningResult result = provisioningService.provision(new GitSourceProvisioningRequest(
                toolId + ":" + requirement.canonicalIdentity(),
                cloneUrl,
                requirement.canonicalIdentity(),
                checkoutRef,
                repositoryRoot,
                null,
                timeout
        ));
        if (!result.ready()) {
            throw new IllegalStateException("Required source repository is not ready for " + toolId
                    + ": " + result.diagnosticSummary());
        }
        provisionPythonEnvironment(metadata, requirement, result, runtimeProperties);
    }

    private void provisionPythonEnvironment(
            McpToolNativeMetadata metadata,
            ToolRequirement requirement,
            ProvisioningResult source,
            Map<String, String> runtimeProperties
    ) {
        List<String> interpreterProperties = metadata.properties().stream()
                .map(property -> property.name())
                .filter(name -> name.endsWith(".python-executable"))
                .toList();
        if (interpreterProperties.isEmpty()) {
            return;
        }
        if (interpreterProperties.size() != 1) {
            throw new IllegalStateException("Tool " + metadata.toolId()
                    + " must declare exactly one *.python-executable property for automatic Python provisioning.");
        }
        Path sourceRoot = source.resource() == null ? null : source.resource().sourceRoot();
        PythonProjectDescriptor project = PythonProjectDiscovery.discover(sourceRoot)
                .filter(PythonProjectDescriptor::provisionable)
                .orElseThrow(() -> new IllegalStateException("Required source repository is not a supported Python project: " + requirement.name()));
        Path virtualEnvironment = pythonEnvironmentRoot(requirement.canonicalIdentity(), requirement.checkoutRef());
        ProvisioningResult environment = provisioningService.provision(new PythonVenvProvisioningRequest(
                metadata.toolId() + ":" + requirement.canonicalIdentity() + "@" + requirement.checkoutRef(),
                project.sourceRoot(),
                virtualEnvironment,
                configuredBootstrapPython(),
                project.requiredImports(),
                List.of(),
                project.requirementsFile(),
                project.installProject(),
                timeout
        ));
        if (!environment.ready() || environment.resource() == null || environment.resource().executable() == null) {
            throw new IllegalStateException("Required Python environment is not ready for " + metadata.toolId()
                    + ": " + environment.diagnosticSummary());
        }
        runtimeProperties.put(interpreterProperties.getFirst(), environment.resource().executable().toString());
    }

    private Path pythonEnvironmentRoot(String canonicalIdentity, String checkoutRef) {
        Path root = repositoryRoot.resolve("runtime").resolve("python");
        for (String segment : canonicalIdentity.split("/")) {
            if (!segment.matches("[A-Za-z0-9._-]+") || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalStateException("Canonical source identity contains an unsafe path segment.");
            }
            root = root.resolve(segment);
        }
        Path environment = root.resolve(checkoutRef).resolve(".venv").normalize();
        if (!environment.startsWith(repositoryRoot.toAbsolutePath().normalize())) {
            throw new IllegalStateException("Python virtual environment escapes the repository root.");
        }
        return environment;
    }

    private static String configuredBootstrapPython() {
        String override = System.getenv("MESHINGRESS_PROVISIONING_PYTHON_EXECUTABLE");
        if (override == null || override.isBlank()) {
            override = System.getenv("PYTHON");
        }
        return override == null || override.isBlank() ? "python" : override.trim();
    }
}
