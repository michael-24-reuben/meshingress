package dev.mrk.meshingress.api;

import dev.mrk.meshingress.api.result.progress.McpProgressReporter;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Execution capability and cancellation state, kept separate from caller identity. */
public record McpExecutionControl(Instant deadline, AtomicBoolean cancelled, McpProgressReporter progressReporter) {
    public McpExecutionControl {
        cancelled = Objects.requireNonNullElseGet(cancelled, AtomicBoolean::new);
        progressReporter = Objects.requireNonNullElseGet(progressReporter, McpProgressReporter::new);
    }
    public static McpExecutionControl none() { return new McpExecutionControl(null, new AtomicBoolean(), new McpProgressReporter()); }
    public boolean isCancelled() { return cancelled.get() || (deadline != null && Instant.now().isAfter(deadline)); }
}
