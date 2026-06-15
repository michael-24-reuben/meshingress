package dev.mrk.toolspace.webtoon;

import java.util.List;

public record WebtoonDownloaderCommandResult(
        int exitCode,
        String stdout,
        String stderr,
        boolean timedOut,
        List<String> command
) {
}
