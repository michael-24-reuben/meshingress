package dev.mrk.toolspace.webtoon;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public interface WebtoonDownloaderRunner {
    WebtoonDownloaderCommandResult run(List<String> command, Path workingDirectory, Duration timeout)
            throws IOException, InterruptedException;
}
