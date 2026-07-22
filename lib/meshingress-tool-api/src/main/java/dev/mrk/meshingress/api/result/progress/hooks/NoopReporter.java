package dev.mrk.meshingress.api.result.progress.hooks;

import dev.mrk.meshingress.api.result.progress.ConsumeProgressUpdate;
import dev.mrk.meshingress.api.result.progress.Reporter;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

/**
 * Reporter whose event handlers intentionally perform no work.
 */
public final class NoopReporter extends Reporter {

    NoopReporter() {
        super(new ConsumeProgressUpdate());
    }

    @Contract(" -> new")
    public static @NonNull NoopReporter create() {
        return new NoopReporter();
    }
}
