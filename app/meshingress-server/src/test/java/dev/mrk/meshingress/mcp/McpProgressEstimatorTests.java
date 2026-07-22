package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.api.result.progress.ProgressUpdate;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpProgressEstimatorTests {

    @Test
    void calculatesSmoothedEtaFromAggregateConcurrentUnitProgress() throws Exception {
        AtomicLong clock = new AtomicLong();
        List<McpProgressEstimate> estimates = new CopyOnWriteArrayList<>();
        CountDownLatch estimateDelivered = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        McpProgressEstimator estimator = new McpProgressEstimator(estimate -> {
            estimates.add(estimate);
            estimateDelivered.countDown();
        }, clock::get, executor);

        try {
            estimator.accept(ProgressUpdate.planned("downloading", 10, "Starting", Duration.ofMinutes(10), List.of("downloading")));
            clock.set(Duration.ofSeconds(1).toNanos());
            estimator.accept(ProgressUpdate.inProgress("downloading", 2, 10, "Two workers completed units."));
            clock.set(Duration.ofSeconds(3).toNanos());
            estimator.accept(ProgressUpdate.inProgress("downloading", 5, 10, "Three more workers completed units."));

            assertTrue(estimateDelivered.await(1, TimeUnit.SECONDS));
            McpProgressEstimate estimate = estimates.getFirst();
            assertEquals(5, estimate.unitsCompleted());
            assertEquals(10, estimate.total());
            assertEquals(3, estimate.sampleUnits());
            assertEquals(Duration.ofSeconds(3), estimate.elapsed());
            assertEquals(Duration.ofSeconds(2), estimate.sampleDuration());
            assertEquals(13d / 24d, estimate.observedSecondsPerUnit(), 0.0001d);
            assertEquals(Duration.ofSeconds(2, 708_333_333), estimate.estimatedRemaining());
            assertEquals(Duration.ofSeconds(5, 708_333_333), estimate.estimatedTotal());

            estimator.accept(ProgressUpdate.successful("downloading", 5, 10, "Stopped."));
            assertTrue(estimator.terminalReported());
        } finally {
            estimator.close();
        }
    }
}
