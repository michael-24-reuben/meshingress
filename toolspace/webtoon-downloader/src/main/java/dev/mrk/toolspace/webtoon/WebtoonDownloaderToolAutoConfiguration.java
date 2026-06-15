package dev.mrk.toolspace.webtoon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.Duration;

@AutoConfiguration
public class WebtoonDownloaderToolAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    WebtoonDownloaderRunner webtoonDownloaderRunner() {
        return new ProcessWebtoonDownloaderRunner();
    }

    @Bean
    WebtoonDownloaderTool webtoonDownloaderTool(
            ObjectMapper objectMapper,
            WebtoonDownloaderRunner runner,
            @Value("${meshingress.webtoon-downloader.command:webtoon-downloader}") String command,
            @Value("${meshingress.webtoon-downloader.output-root:./var/meshingress/webtoon-downloads}") String outputRoot,
            @Value("${meshingress.webtoon-downloader.timeout-seconds:900}") long timeoutSeconds,
            @Value("${meshingress.webtoon-downloader.max-concurrent-chapters:3}") int maxConcurrentChapters,
            @Value("${meshingress.webtoon-downloader.max-concurrent-pages:20}") int maxConcurrentPages
    ) {
        WebtoonDownloaderConfig config = new WebtoonDownloaderConfig(
                command,
                Path.of(outputRoot),
                Duration.ofSeconds(Math.max(5L, timeoutSeconds)),
                Math.max(1, maxConcurrentChapters),
                Math.max(1, maxConcurrentPages)
        );
        return new WebtoonDownloaderTool(objectMapper, runner, config);
    }
}
