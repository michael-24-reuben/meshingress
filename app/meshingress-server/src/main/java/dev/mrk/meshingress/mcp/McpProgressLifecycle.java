package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.api.result.progress.McpProgressReporter;
import dev.mrk.meshingress.api.result.progress.ProgressUpdate;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Server-side bridge between a tool's reporter and its WebSocket deadline.
 */
public final class McpProgressLifecycle {

    private final CompletableFuture<Duration> estimatedDuration = new CompletableFuture<>();
    private final Consumer<ProgressUpdate> eventSender;
    private final McpProgressEstimator progressEstimator;

    public McpProgressLifecycle(Consumer<ProgressUpdate> eventSender) {
        this.eventSender = Objects.requireNonNull(eventSender, "eventSender must not be null");
        this.progressEstimator = null;
    }

    public McpProgressLifecycle(
            Consumer<ProgressUpdate> eventSender,
            Consumer<McpProgressEstimate> estimateSender
    ) {
        this.eventSender = Objects.requireNonNull(eventSender, "eventSender must not be null");
        this.progressEstimator = new McpProgressEstimator(Objects.requireNonNull(estimateSender, "estimateSender must not be null"));
    }

    public McpProgressReporter reporter() {
        return McpProgressReporter.webSocket(this::report);
    }

    public CompletableFuture<Duration> estimatedDuration() {
        return estimatedDuration;
    }

    private void report(ProgressUpdate update) {
        if (update.state() == McpProgressReporter.UpdateState.PLANNED) {
            Duration estimate = update.estimate();
            estimatedDuration.complete(estimate == null || estimate.isNegative() ? Duration.ZERO : estimate);
        }
        eventSender.accept(update);
        if (progressEstimator != null) {
            progressEstimator.accept(update);
        }
    }

    public boolean terminalReported() {
        return progressEstimator == null || progressEstimator.terminalReported();
    }

    public void close() {
        if (progressEstimator != null) {
            progressEstimator.close();
        }
    }
}
