package dev.mrk.meshingress.api.result.progress.hooks;

import dev.mrk.meshingress.api.result.progress.ConsumeProgressUpdate;
import dev.mrk.meshingress.api.result.progress.ProgressUpdate;
import dev.mrk.meshingress.api.result.progress.Reporter;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

/**
 * Reporter that writes events to the console.
 */
public final class ConsoleReporter extends Reporter {

    ConsoleReporter() {
        super(new ConsumeProgressUpdate() {

            @Override
            protected void onPlanEvent(ProgressUpdate progress) {
                System.out.printf(
                        "[MCP PLAN] estimate=%s total=%d phases=%s message=%s%n",
                        progress.estimate() == null
                                ? "unknown"
                                : progress.estimate(),
                        progress.total(),
                        progress.phases(),
                        progress.message()
                );
            }

            @Override
            protected void onUpdateEvent(ProgressUpdate progress) {
                System.out.printf(
                        "[MCP UPDATE] phase=%s %d/%d %s%n",
                        progress.phase(),
                        progress.unitsCompleted(),
                        progress.total(),
                        progress.message()
                );
            }

            @Override
            protected void onWarningEvent(ProgressUpdate progress) {
                System.out.printf(
                        "[MCP WARNING] phase=%s %d/%d %s%n",
                        progress.phase(),
                        progress.unitsCompleted(),
                        progress.total(),
                        progress.message()
                );
            }

            @Override
            protected void onCompleteEvent(ProgressUpdate progress) {
                System.out.printf(
                        "[MCP COMPLETE] phase=%s %d/%d %s%n",
                        progress.phase(),
                        progress.unitsCompleted(),
                        progress.total(),
                        progress.message()
                );
            }

            @Override
            protected void onErrorEvent(ProgressUpdate progress) {
                System.err.printf(
                        "[MCP ERROR] phase=%s %d/%d %s%n",
                        progress.phase(),
                        progress.unitsCompleted(),
                        progress.total(),
                        progress.message()
                );
            }
        });
    }

    @Contract(" -> new")
    public static @NonNull ConsoleReporter create() {
        return new ConsoleReporter();
    }
}
