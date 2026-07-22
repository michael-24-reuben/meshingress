package dev.mrk.meshingress.provisioning;

import java.util.List;

public record ProvisioningResult(
        ProvisioningStatus status,
        ProvisionedResource resource,
        List<ProvisioningEvidence> evidence,
        List<String> warnings,
        String diagnosticSummary
) {
    public ProvisioningResult {
        status = status == null ? ProvisioningStatus.FAILED : status;
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        diagnosticSummary = diagnosticSummary == null ? "" : diagnosticSummary.trim();
    }

    public boolean ready() {
        return status == ProvisioningStatus.READY || status == ProvisioningStatus.REUSED;
    }
}
