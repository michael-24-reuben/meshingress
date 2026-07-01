package dev.mrk.meshingress.artifact.model;

import java.util.List;
import java.util.Map;

public record PublicationEligibilityDecision(
        boolean eligible,
        List<String> reasonCodes,
        Map<String, Object> evidence
) {
    public PublicationEligibilityDecision {
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }

    public static PublicationEligibilityDecision accepted(Map<String, Object> evidence) {
        return new PublicationEligibilityDecision(true, List.of(), evidence);
    }

    public static PublicationEligibilityDecision denied(List<String> reasonCodes, Map<String, Object> evidence) {
        return new PublicationEligibilityDecision(false, reasonCodes, evidence);
    }
}
