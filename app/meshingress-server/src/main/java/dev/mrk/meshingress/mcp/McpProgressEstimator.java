package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.api.result.progress.McpProgressReporter;
import dev.mrk.meshingress.api.result.progress.ProgressUpdate;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Serially derives client-facing ETA updates from progress events.
 */
final class McpProgressEstimator implements AutoCloseable {

    private static final double EWMA_ALPHA = 0.25d;

    private final Consumer<McpProgressEstimate> estimateSender;
    private final LongSupplier nanoTime;
    private final ExecutorService executor;
    private final AtomicBoolean accepting = new AtomicBoolean(true);

    private long startedAtNanos = -1L;
    private long previousSampleAtNanos = -1L;
    private int previousCompleted;
    private int positiveSamples;
    private double smoothedSecondsPerUnit;

    McpProgressEstimator(Consumer<McpProgressEstimate> estimateSender) {
        this(
                estimateSender,
                System::nanoTime,
                Executors.newSingleThreadExecutor(Thread.ofVirtual().name("meshingress-progress-estimator-", 0).factory())
        );
    }

    McpProgressEstimator(Consumer<McpProgressEstimate> estimateSender, LongSupplier nanoTime, ExecutorService executor) {
        this.estimateSender = Objects.requireNonNull(estimateSender, "estimateSender must not be null");
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
    }

    void accept(ProgressUpdate update) {
        if (!accepting.get()) {
            return;
        }
        long observedAtNanos = nanoTime.getAsLong();
        if (isTerminal(update)) {
            accepting.set(false);
            submit(this::finishAfterQueuedEvents);
            return;
        }
        submit(() -> observe(update, observedAtNanos));
    }

    boolean terminalReported() {
        return !accepting.get();
    }

    @Override
    public void close() {
        accepting.set(false);
        executor.shutdownNow();
    }

    private void observe(ProgressUpdate update, long observedAtNanos) {
        if (update.state() == McpProgressReporter.UpdateState.PLANNED) {
            startedAtNanos = observedAtNanos;
            previousSampleAtNanos = observedAtNanos;
            previousCompleted = update.unitsCompleted();
            positiveSamples = 0;
            smoothedSecondsPerUnit = 0d;
            return;
        }
        if (update.state() != McpProgressReporter.UpdateState.IN_PROGRESS || update.total() <= 0) {
            return;
        }
        if (startedAtNanos < 0L) {
            startedAtNanos = observedAtNanos;
            previousSampleAtNanos = observedAtNanos;
            previousCompleted = update.unitsCompleted();
            return;
        }

        int advancedUnits = update.unitsCompleted() - previousCompleted;
        long elapsedNanos = observedAtNanos - previousSampleAtNanos;
        if (advancedUnits <= 0 || elapsedNanos <= 0L) {
            return;
        }

        double sampleSecondsPerUnit = nanosToSeconds(elapsedNanos) / advancedUnits;
        if (!Double.isFinite(sampleSecondsPerUnit) || sampleSecondsPerUnit <= 0d) {
            return;
        }
        previousCompleted = update.unitsCompleted();
        previousSampleAtNanos = observedAtNanos;
        positiveSamples++;
        smoothedSecondsPerUnit = positiveSamples == 1
                ? sampleSecondsPerUnit
                : EWMA_ALPHA * sampleSecondsPerUnit + (1d - EWMA_ALPHA) * smoothedSecondsPerUnit;

        if (positiveSamples < 2 || update.unitsCompleted() >= update.total()) {
            return;
        }

        if (!accepting.get()) {
            return;
        }

        int remainingUnits = update.total() - update.unitsCompleted();
        long remainingNanos = secondsToNanos(smoothedSecondsPerUnit * remainingUnits);
        long totalElapsedNanos = observedAtNanos - startedAtNanos;
        estimateSender.accept(new McpProgressEstimate(
                update.unitsCompleted(),
                update.total(),
                advancedUnits,
                Duration.ofNanos(totalElapsedNanos),
                Duration.ofNanos(elapsedNanos),
                smoothedSecondsPerUnit,
                Duration.ofNanos(remainingNanos),
                Duration.ofNanos(saturatingAdd(totalElapsedNanos, remainingNanos))
        ));
    }

    private void finishAfterQueuedEvents() {
        executor.shutdown();
    }

    private void submit(Runnable task) {
        try {
            executor.execute(task);
        } catch (RejectedExecutionException ignored) {
            // The call has already completed, timed out, or been cancelled.
        }
    }

    private static boolean isTerminal(ProgressUpdate update) {
        return update.state() == McpProgressReporter.UpdateState.COMPLETED_SUCCESSFUL
                || update.state() == McpProgressReporter.UpdateState.COMPLETED_FAILED;
    }

    private static double nanosToSeconds(long nanos) {
        return nanos / 1_000_000_000d;
    }

    private static long secondsToNanos(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0d) {
            return 0L;
        }
        if (seconds >= Long.MAX_VALUE / 1_000_000_000d) {
            return Long.MAX_VALUE;
        }
        return Math.round(seconds * 1_000_000_000d);
    }

    private static long saturatingAdd(long first, long second) {
        if (first > Long.MAX_VALUE - second) {
            return Long.MAX_VALUE;
        }
        return first + second;
    }
}
