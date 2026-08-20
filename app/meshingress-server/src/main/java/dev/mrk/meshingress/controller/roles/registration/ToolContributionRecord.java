package dev.mrk.meshingress.controller.roles.registration;

import java.time.OffsetDateTime;

/**
 * Persisted administrator-controlled position of one registered module within a tool namespace.
 */
public record ToolContributionRecord(
        String toolId,
        String namespace,
        int precedence,
        ToolContributionActivationMode activationMode,
        String status,
        OffsetDateTime reservedAt,
        String runtimeModuleId
) {
    public ToolContributionRecord {
        toolId = required(toolId, "tool ID");
        namespace = required(namespace, "namespace");
        if (!namespace.matches("[a-z][a-z0-9_-]*")) {
            throw new IllegalArgumentException("tool namespace must use one lower-case segment: " + namespace);
        }
        if (precedence < 0) {
            throw new IllegalArgumentException("tool contribution precedence must not be negative");
        }
        activationMode = activationMode == null ? ToolContributionActivationMode.CONTRIBUTOR : activationMode;
        status = status == null || status.isBlank() ? "reserved" : status.strip();
        reservedAt = reservedAt == null ? OffsetDateTime.now() : reservedAt;
        runtimeModuleId = runtimeModuleId == null ? "" : runtimeModuleId.strip();
    }

    public ToolContributionRecord(String toolId, String namespace, int precedence,
                                  ToolContributionActivationMode activationMode, String status, OffsetDateTime reservedAt) {
        this(toolId, namespace, precedence, activationMode, status, reservedAt, "");
    }

    public ToolContributionRecord withStatus(String nextStatus) {
        return new ToolContributionRecord(toolId, namespace, precedence, activationMode, nextStatus, reservedAt, runtimeModuleId);
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value.strip();
    }
}
