package dev.mrk.toolspace.omnivoice;

import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public interface OmniVoiceRunner {
    OmniVoiceCommandResult run(List<String> command, JsonNode request, Path workingDirectory, Duration timeout)
            throws IOException, InterruptedException;
}
