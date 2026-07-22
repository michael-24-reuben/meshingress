package dev.mrk.meshingress.api.result.progress.hooks;

import dev.mrk.meshingress.api.result.progress.ConsumeProgressUpdate;
import dev.mrk.meshingress.api.result.progress.ProgressUpdate;
import dev.mrk.meshingress.api.result.progress.Reporter;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Meshingress progress reporter that forwards every lifecycle event to a
 * WebSocket transport sender.
 *
 * <p>The sender owns serialization, session selection, and transmission. This
 * reporter only translates the progress lifecycle into sender invocations.</p>
 */
public final class MeshigressWebSocketReporter extends Reporter {

    private MeshigressWebSocketReporter(Consumer<ProgressUpdate> sender) {
        super(new ConsumeProgressUpdate() {

            @Override
            protected void onPlanEvent(ProgressUpdate progress) {
                sender.accept(progress);
            }

            @Override
            protected void onUpdateEvent(ProgressUpdate progress) {
                sender.accept(progress);
            }

            @Override
            protected void onWarningEvent(ProgressUpdate progress) {
                sender.accept(progress);
            }

            @Override
            protected void onCompleteEvent(ProgressUpdate progress) {
                sender.accept(progress);
            }

            @Override
            protected void onErrorEvent(ProgressUpdate progress) {
                sender.accept(progress);
            }
        });
    }

    /**
     * Creates a reporter for one progress invocation.
     *
     * @param sender the transport-specific event sender
     * @return a WebSocket-backed reporter
     */
    public static MeshigressWebSocketReporter create(Consumer<ProgressUpdate> sender) {
        return new MeshigressWebSocketReporter(
                Objects.requireNonNull(sender, "sender must not be null")
        );
    }
}
