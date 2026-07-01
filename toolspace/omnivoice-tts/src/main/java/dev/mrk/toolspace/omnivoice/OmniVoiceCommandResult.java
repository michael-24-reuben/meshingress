package dev.mrk.toolspace.omnivoice;

import java.util.List;

public record OmniVoiceCommandResult(
        int exitCode,
        String stdout,
        String stderr,
        boolean timedOut,
        List<String> command
) {
}
