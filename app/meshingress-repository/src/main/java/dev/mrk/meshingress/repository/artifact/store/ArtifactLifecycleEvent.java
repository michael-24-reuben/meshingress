package dev.mrk.meshingress.repository.artifact.store;

import dev.mrk.meshingress.artifact.model.ArtifactCoordinate;

public record ArtifactLifecycleEvent(
        ArtifactCoordinate coordinate,
        String eventType,
        String fromState,
        String toState,
        String actor,
        String requestId,
        String reason
) {
    public ArtifactLifecycleEvent {
        actor = actor == null || actor.isBlank() ? "unknown" : actor.trim();
        requestId = clean(requestId);
        reason = clean(reason);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
