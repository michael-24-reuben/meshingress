package dev.mrk.meshingress.provisioning.git;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Requests a managed Git checkout. The caller supplies a canonical identity from trusted tool
 * metadata; this module deliberately does not duplicate manifest URL canonicalization.
 */
public record GitSourceProvisioningRequest(
        String requirementId,
        String cloneUrl,
        String canonicalIdentity,
        String checkoutRef,
        Path repositoryRoot,
        Path subdirectory,
        Duration timeout
) {
    public GitSourceProvisioningRequest {
        requirementId = defaulted(requirementId, "git-source");
        cloneUrl = required(cloneUrl, "cloneUrl");
        canonicalIdentity = required(canonicalIdentity, "canonicalIdentity");
        checkoutRef = defaulted(checkoutRef, "HEAD");
        repositoryRoot = repositoryRoot == null ? Path.of("repository") : repositoryRoot;
        timeout = timeout == null || timeout.isNegative() || timeout.isZero() ? Duration.ofMinutes(2) : timeout;
    }

    private static String defaulted(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isBlank() ? fallback : normalized;
    }

    private static String required(String value, String name) {
        String normalized = defaulted(value, "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
