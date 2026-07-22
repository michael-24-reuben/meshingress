package dev.mrk.meshingress.api.result.progress;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Consumes progress events and dispatches them to state-specific event
 * methods.
 *
 * <p>Reporter implementations override only the event methods they need.</p>
 */
public class ConsumeProgressUpdate implements Consumer<ProgressUpdate> {

    /**
     * Handles a planning event.
     */
    protected void onPlanEvent(ProgressUpdate progress) {
        // Default no-op.
    }

    /**
     * Handles a regular progress update.
     */
    protected void onUpdateEvent(ProgressUpdate progress) {
        // Default no-op.
    }

    /**
     * Handles a warning event.
     */
    protected void onWarningEvent(ProgressUpdate progress) {
        // Default no-op.
    }

    /**
     * Handles a successful terminal event.
     */
    protected void onCompleteEvent(ProgressUpdate progress) {
        // Default no-op.
    }

    /**
     * Handles a failed terminal event.
     */
    protected void onErrorEvent(ProgressUpdate progress) {
        // Default no-op.
    }

    @Override
    public final void accept(ProgressUpdate progress) {
        Objects.requireNonNull(
                progress,
                "progress must not be null"
        );

        switch (progress.state()) {
            case PLANNED -> onPlanEvent(progress);

            case IN_PROGRESS -> onUpdateEvent(progress);

            case WARNING -> onWarningEvent(progress);

            case COMPLETED_SUCCESSFUL -> onCompleteEvent(progress);

            case COMPLETED_FAILED -> onErrorEvent(progress);
        }
    }
}
