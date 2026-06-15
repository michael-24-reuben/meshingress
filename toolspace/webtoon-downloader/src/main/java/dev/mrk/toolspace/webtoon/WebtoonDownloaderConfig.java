package dev.mrk.toolspace.webtoon;

import java.nio.file.Path;
import java.time.Duration;

record WebtoonDownloaderConfig(
        String command,
        Path outputRoot,
        Duration timeout,
        int maxConcurrentChapters,
        int maxConcurrentPages
) {
}
