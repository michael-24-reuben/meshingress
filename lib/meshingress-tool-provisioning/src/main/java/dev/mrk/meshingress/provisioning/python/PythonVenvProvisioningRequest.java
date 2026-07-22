package dev.mrk.meshingress.provisioning.python;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * A local-source request. Git checkout/download is a separate provider phase and is deliberately
 * not inferred from this request.
 */
public record PythonVenvProvisioningRequest(
        String requirementId,
        Path sourceRoot,
        Path virtualEnvironmentRoot,
        String bootstrapPython,
        List<String> requiredImports,
        List<String> explicitAdditionalDistributions,
        Path requirementsFile,
        boolean installProject,
        Duration timeout
) {
    public PythonVenvProvisioningRequest {
        requirementId = requirementId == null || requirementId.isBlank() ? "python-project" : requirementId.trim();
        requiredImports = requiredImports == null ? List.of() : List.copyOf(requiredImports);
        explicitAdditionalDistributions = explicitAdditionalDistributions == null
                ? List.of() : List.copyOf(explicitAdditionalDistributions);
        timeout = timeout == null || timeout.isNegative() || timeout.isZero() ? Duration.ofMinutes(10) : timeout;
    }

    /**
     * Compatibility constructor for direct callers that want the default package-install flow.
     */
    public PythonVenvProvisioningRequest(
            String requirementId,
            Path sourceRoot,
            Path virtualEnvironmentRoot,
            String bootstrapPython,
            List<String> requiredImports,
            List<String> explicitAdditionalDistributions,
            Duration timeout
    ) {
        this(requirementId, sourceRoot, virtualEnvironmentRoot, bootstrapPython, requiredImports,
                explicitAdditionalDistributions, null, true, timeout);
    }
}
