package dev.mrk.meshingress.api.tools.annotation.availability;

import java.util.Map;

public record AvailabilityDecision(
        boolean allowed,
        String reason,
        Map<String, Object> metadata
) {

    public AvailabilityDecision {
        reason = reason == null ? "" : reason;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static AvailabilityDecision allow() {
        return new AvailabilityDecision(true, "", Map.of());
    }

    public static AvailabilityDecision allow(String reason) {
        return new AvailabilityDecision(true, reason, Map.of());
    }

    public static AvailabilityDecision deny(String reason) {
        return new AvailabilityDecision(false, reason, Map.of());
    }
}
