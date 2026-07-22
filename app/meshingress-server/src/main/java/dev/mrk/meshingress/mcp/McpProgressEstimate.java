package dev.mrk.meshingress.mcp;

import java.time.Duration;

/**
 * A client-observability estimate calculated from completed work units.
 *
 * <p>This is intentionally independent of dispatch timeout handling. It
 * describes observed throughput only and never changes an invocation's hard
 * deadline.</p>
 */
public record McpProgressEstimate(
        int unitsCompleted,
        int total,
        int sampleUnits,
        Duration elapsed,
        Duration sampleDuration,
        double observedSecondsPerUnit,
        Duration estimatedRemaining,
        Duration estimatedTotal
) {
}
