package dev.mrk.meshingress.api.result.progress;

import dev.mrk.meshingress.api.result.progress.hooks.ConsoleReporter;
import dev.mrk.meshingress.api.result.progress.hooks.NoopReporter;
import dev.mrk.meshingress.api.result.progress.hooks.MeshigressWebSocketReporter;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Reports progress for long-running MCP tool operations.
 *
 * <p>The reporter supports detailed progress updates and message-only updates.
 * Message-only updates retain the phase, completed-unit count, and total-unit
 * count from the previous plan or detailed update.</p>
 *
 * <p>Typical usage:</p>
 *
 * <pre>{@code
 * progress.plan(
 *         Duration.ofMinutes(5),
 *         10,
 *         List.of("downloading", "processing", "uploading"),
 *         "Preparing operation"
 * );
 *
 * progress.update("downloading", 3, 10, "Downloaded chapter 3");
 * progress.update("Validating downloaded content");
 * progress.warning("Chapter metadata was incomplete");
 * progress.complete("Operation completed");
 * }</pre>
 */
public class McpProgressReporter {

    /**
     * No-op reporter for handlers that do not need progress feedback.
     */
    public static final McpProgressReporter NOOP = new McpProgressReporter(NoopReporter.create());

    private final Reporter delegate;

    /**
     * Creates a no-op progress reporter.
     */
    public McpProgressReporter() {
        this(NoopReporter.create());
    }

    private McpProgressReporter(Reporter delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Creates a console-backed reporter.
     *
     * @return console-backed progress reporter
     */
    public static McpProgressReporter console() {
        return new McpProgressReporter(ConsoleReporter.create());
    }

    /**
     * Creates a reporter that forwards progress events to a WebSocket sender.
     *
     * <p>The supplied consumer should serialize and transmit the event through
     * the appropriate WebSocket session.</p>
     *
     * @param sender WebSocket event sender
     * @return WebSocket-backed progress reporter
     */
    public static McpProgressReporter webSocket(Consumer<ProgressUpdate> sender) {
        Objects.requireNonNull(sender, "sender must not be null");

        return new McpProgressReporter(MeshigressWebSocketReporter.create(sender));
    }

    /**
     * Reports the overall operation plan.
     *
     * <p>The first declared phase becomes the retained current phase. When no
     * phases are declared, {@code planning} is used.</p>
     *
     * @param estimate   estimated operation duration, or {@code null} if unknown
     * @param totalUnits total number of planned work units
     * @param phases     ordered logical operation phases
     * @param message    human-readable planning message
     */
    public void plan(Duration estimate, int totalUnits, List<String> phases, String message) {
        delegate.plan(estimate, totalUnits, phases, message);
    }

    /**
     * Reports detailed progress.
     *
     * <p>This method replaces the retained phase, completed-unit count, and
     * total-unit count.</p>
     *
     * @param phase          current logical phase
     * @param unitsCompleted units completed so far
     * @param total          total planned units
     * @param message        human-readable progress message
     */
    public void update(String phase, int unitsCompleted, int total, String message) {
        delegate.update(phase, unitsCompleted, total, message);
    }

    /**
     * Reports a message without changing the current progress coordinates.
     *
     * <p>The previously retained phase, completed-unit count, and total-unit
     * count are included in the emitted event.</p>
     *
     * @param message human-readable progress message
     */
    public void update(String message) {
        delegate.update(message);
    }

    /**
     * Reports a non-terminal warning without changing the current progress
     * coordinates.
     *
     * @param message human-readable warning message
     */
    public void warning(String message) {
        delegate.warning(message);
    }

    /**
     * Marks the operation as successfully completed.
     *
     * <p>The completion event retains the latest phase, completed-unit count,
     * and total-unit count.</p>
     *
     * @param message human-readable completion message
     */
    public void complete(String message) {
        delegate.complete(message);
    }

    /**
     * Marks the operation as failed.
     *
     * <p>The failure event retains the latest phase, completed-unit count, and
     * total-unit count.</p>
     *
     * @param message human-readable failure message
     */
    public void error(String message) {
        delegate.error(message);
    }

    /**
     * Indicates the semantic type of a progress event.
     */
    public enum UpdateState {
        PLANNED,
        IN_PROGRESS,
        WARNING,
        COMPLETED_SUCCESSFUL,
        COMPLETED_FAILED
    }

}
