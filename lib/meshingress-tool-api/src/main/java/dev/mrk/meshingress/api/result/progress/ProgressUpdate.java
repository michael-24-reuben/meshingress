package dev.mrk.meshingress.api.result.progress;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Immutable progress event delivered to consumers.
 *
 * @param phase          current logical phase
 * @param unitsCompleted units completed so far
 * @param total          total planned units
 * @param message        human-readable event message
 * @param state          semantic event state
 * @param estimate       estimated duration for planning events
 * @param phases         declared phases for planning events
 */
public record ProgressUpdate(String phase, int unitsCompleted, int total, String message, McpProgressReporter.UpdateState state, Duration estimate, List<String> phases) {

    public ProgressUpdate {
        phase = normalizePhase(phase);
        message = normalizeMessage(message);
        state = Objects.requireNonNull(state, "state must not be null");
        phases = phases == null
                ? List.of()
                : List.copyOf(phases);

        validateCoordinates(unitsCompleted, total);
    }

    /**
     * Creates a planning event.
     */
    public static ProgressUpdate planned(String phase, int total, String message, Duration estimate, List<String> phases) {
        return new ProgressUpdate(phase, 0, total, message, McpProgressReporter.UpdateState.PLANNED, estimate, phases);
    }

    /**
     * Creates an in-progress event.
     */
    public static ProgressUpdate inProgress(String phase, int unitsCompleted, int total, String message) {
        return new ProgressUpdate(
                phase,
                unitsCompleted,
                total,
                message,
                McpProgressReporter.UpdateState.IN_PROGRESS,
                null,
                List.of()
        );
    }

    /**
     * Creates a warning event.
     */
    public static ProgressUpdate warning(String phase, int unitsCompleted, int total, String message) {
        return new ProgressUpdate(
                phase,
                unitsCompleted,
                total,
                message,
                McpProgressReporter.UpdateState.WARNING,
                null,
                List.of()
        );
    }

    /**
     * Creates a successful terminal event.
     */
    public static ProgressUpdate successful(String phase, int unitsCompleted, int total, String message) {
        return new ProgressUpdate(
                phase,
                unitsCompleted,
                total,
                message,
                McpProgressReporter.UpdateState.COMPLETED_SUCCESSFUL,
                null,
                List.of()
        );
    }

    /**
     * Creates a failed terminal event.
     */
    public static ProgressUpdate failed(String phase, int unitsCompleted, int total, String message) {
        return new ProgressUpdate(
                phase,
                unitsCompleted,
                total,
                message,
                McpProgressReporter.UpdateState.COMPLETED_FAILED,
                null,
                List.of()
        );
    }

    private static void validateCoordinates(int unitsCompleted, int total) {
        if (unitsCompleted < 0) {
            throw new IllegalArgumentException("unitsCompleted must not be negative");
        }

        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }

        if (unitsCompleted > total) {
            throw new IllegalArgumentException("unitsCompleted must not exceed total");
        }
    }

    private static String normalizePhase(String phase) {
        if (phase == null || phase.isBlank()) {
            return "unknown";
        }

        return phase;
    }

    private static String normalizeMessage(String message) {
        return message == null ? "" : message;
    }
}
