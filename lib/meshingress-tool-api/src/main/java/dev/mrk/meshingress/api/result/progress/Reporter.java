package dev.mrk.meshingress.api.result.progress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Internal progress reporting strategy.
 *
 * <p>This class owns progress-state retention and terminal-state
 * enforcement. Concrete reporters define event delivery through a
 * {@link ConsumeProgressUpdate} implementation.</p>
 */
public abstract class Reporter {

    static final Logger logger = LoggerFactory.getLogger(Reporter.class);

    private final Object stateLock = new Object();
    private final Consumer<ProgressUpdate> onEventCallback;

    private String currentPhase = "unknown";
    private int currentUnitsCompleted;
    private int currentTotal;
    private boolean complete;

    protected Reporter(Consumer<ProgressUpdate> onEventCallback) {
        this.onEventCallback = Objects.requireNonNull(
                onEventCallback,
                "onEventCallback must not be null"
        );
    }

    public final void plan(Duration estimate, int totalUnits, List<String> phases, String message) {
        List<String> resolvedPhases = phases == null
                ? List.of()
                : List.copyOf(phases);

        String initialPhase = resolvedPhases.isEmpty()
                ? "planning"
                : resolvedPhases.getFirst();

        ProgressUpdate progress;

        synchronized (stateLock) {
            requireOpen();
            validateCoordinates(0, totalUnits);

            currentPhase = initialPhase;
            currentUnitsCompleted = 0;
            currentTotal = totalUnits;

            progress = ProgressUpdate.planned(
                    currentPhase,
                    currentTotal,
                    message,
                    estimate,
                    resolvedPhases
            );
        }

        emit(progress);
    }

    public final void update(String phase, int unitsCompleted, int total, String message) {
        ProgressUpdate progress;

        synchronized (stateLock) {
            requireOpen();
            validateCoordinates(unitsCompleted, total);

            currentPhase = resolvePhase(phase, currentPhase);
            currentUnitsCompleted = unitsCompleted;
            currentTotal = total;

            progress = ProgressUpdate.inProgress(
                    currentPhase,
                    currentUnitsCompleted,
                    currentTotal,
                    message
            );
        }

        emit(progress);
    }

    /**
     * Emits an update while retaining the previous progress coordinates.
     */
    public final void update(String message) {
        ProgressUpdate progress;

        synchronized (stateLock) {
            requireOpen();

            progress = ProgressUpdate.inProgress(
                    currentPhase,
                    currentUnitsCompleted,
                    currentTotal,
                    message
            );
        }

        emit(progress);
    }

    /**
     * Emits a warning while retaining the previous progress coordinates.
     */
    public final void warning(String message) {
        ProgressUpdate progress;

        synchronized (stateLock) {
            requireOpen();

            progress = ProgressUpdate.warning(
                    currentPhase,
                    currentUnitsCompleted,
                    currentTotal,
                    message
            );
        }

        emit(progress);
    }

    /**
     * Emits a successful terminal event while retaining the previous
     * progress coordinates.
     */
    public final void complete(String message) {
        ProgressUpdate progress;

        synchronized (stateLock) {
            requireOpen();
            complete = true;

            progress = ProgressUpdate.successful(
                    currentPhase,
                    currentUnitsCompleted,
                    currentTotal,
                    message
            );
        }

        emit(progress);
    }

    /**
     * Emits a failed terminal event while retaining the previous progress
     * coordinates.
     */
    public final void error(String message) {
        ProgressUpdate progress;

        synchronized (stateLock) {
            requireOpen();
            complete = true;

            progress = ProgressUpdate.failed(
                    currentPhase,
                    currentUnitsCompleted,
                    currentTotal,
                    message
            );
        }

        emit(progress);
    }

    /**
     * Returns whether this reporter has reached a terminal state.
     */
    public final boolean isComplete() {
        synchronized (stateLock) {
            return complete;
        }
    }

    private void emit(ProgressUpdate progress) {
        try {
            onEventCallback.accept(progress);
        } catch (RuntimeException exception) {
            logger.warn(
                    "Progress event callback failed: state={}, phase={}, completed={}, total={}",
                    progress.state(),
                    progress.phase(),
                    progress.unitsCompleted(),
                    progress.total(),
                    exception
            );
        }
    }

    private void requireOpen() {
        if (complete) {
            throw new IllegalStateException("Progress reporter has already reached a terminal state");
        }
    }

    private static String resolvePhase(String requestedPhase, String retainedPhase) {
        if (requestedPhase == null || requestedPhase.isBlank()) {
            return retainedPhase;
        }

        return requestedPhase;
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
}
