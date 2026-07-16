package dev.mrk.meshingress.runtime.provisioning;

import dev.mrk.meshingress.runtime.artifacts.ResolvedToolArtifact;

import java.util.Map;

/**
 * Ensures a resolved artifact's external runtime requirements are ready before its code is loaded.
 */
@FunctionalInterface
public interface ToolProvisioningGate {

    /**
     * Returns verified runtime values that must be visible only to the tool's child context.
     */
    Map<String, String> requireReady(ResolvedToolArtifact artifact);

    static ToolProvisioningGate none() {
        return artifact -> {
            // Preserves compatibility for callers that have not opted into provisioning yet.
            return Map.of();
        };
    }
}
