package dev.mrk.toolspace.webtoon;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.result.ResultContent;
import dev.mrk.meshingress.api.tools.annotation.McpConfigureMapping;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.McpToolMapping;
import dev.mrk.meshingress.api.tools.annotation.McpToolScopes;
import dev.mrk.meshingress.scopes.McpToolScope;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@McpTool(
        value = "webtoon",
        title = "Webtoon Downloader",
        description = "Guarded wrapper around Zehina/Webtoon-Downloader for authorized public WEBTOON metadata export and selected chapter downloads.",
        defaultFunction = "inspect"
)
@McpToolMapping("tools")
@McpToolScopes({
        McpToolScope.PROCESS_EXECUTE,
        McpToolScope.HTTP_CLIENT,
        McpToolScope.EXTERNAL_API_READ,
        McpToolScope.FILES_WRITE,
        McpToolScope.FILES_LIST
})
public class WebtoonDownloaderTool {

    private static final Set<String> EXPORT_FORMATS = Set.of("json", "text", "all");
    private static final Set<String> SAVE_AS = Set.of("images", "zip", "cbz", "pdf");
    private static final Set<String> IMAGE_FORMATS = Set.of("jpg", "png");
    private static final Set<String> RETRY_STRATEGIES = Set.of("exponential", "linear", "fixed", "none");
    private static final Set<Integer> QUALITIES = Set.of(40, 50, 60, 70, 80, 90, 100);

    private final ObjectMapper objectMapper;
    private final WebtoonDownloaderRunner runner;
    private final WebtoonDownloaderConfig config;
    private final Path outputRoot;
/*
* curl -sS -X POST "$BASE_URL/mcp" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 502,
    "method": "tools/call",
    "params": {
      "name": "webtoon.health",
      "arguments": {}
  }'
*/
    public WebtoonDownloaderTool(
            ObjectMapper objectMapper,
            WebtoonDownloaderRunner runner,
            WebtoonDownloaderConfig config
    ) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.runner = Objects.requireNonNull(runner, "runner must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.outputRoot = config.outputRoot().toAbsolutePath().normalize();
    }

    @McpConfigureMapping(timeoutMs = 15_000, audit = true)
    @McpFunction(
            value = "health",
            title = "Webtoon Downloader Health",
            description = "Check the configured webtoon-downloader executable, output root, safety disclaimer, and installed CLI version."
    )
    @McpToolScopes(McpToolScope.HEALTH_CHECK)
    public DispatchExecutionResult health(WebtoonHealthArgs arguments, McpCallContext context) {
        ObjectNode structured = baseResponse(false);
        structured.put("command", config.command());
        structured.put("outputRoot", outputRoot.toString());
        structured.put("timeoutSeconds", config.timeout().toSeconds());
        structured.put("maxConcurrentChapters", config.maxConcurrentChapters());
        structured.put("maxConcurrentPages", config.maxConcurrentPages());
        structured.put("upstreamRepository", "https://github.com/Zehina/Webtoon-Downloader");
        structured.put("upstreamDocs", "https://zehina.github.io/Webtoon-Downloader/");

        try {
            Files.createDirectories(outputRoot);
            WebtoonDownloaderCommandResult result = runner.run(List.of(config.command(), "--version"), outputRoot, config.timeout());
            structured.put("available", result.exitCode() == 0 && !result.timedOut());
            structured.set("versionProbe", commandResultJson(result));
            return ok("Webtoon downloader health checked.", structured);
        } catch (Exception exception) {
            structured.put("available", false);
            structured.put("message", exception.getMessage() == null ? "" : exception.getMessage());
            structured.put("exceptionType", exception.getClass().getName());
            return DispatchExecutionResult.builder()
                    .structuredContent(structured)
                    .error("WEBTOON_DOWNLOADER_UNAVAILABLE", "webtoon-downloader is not available. Install with `uv tool install webtoon_downloader` or `pipx install webtoon_downloader`, or set meshingress.webtoon-downloader.command.")
                    .status("failed")
                    .summary("Webtoon downloader is unavailable.")
                    .build();
        }
    }

    @McpConfigureMapping(timeoutMs = 15_000, audit = true)
    @McpFunction(
            value = "inspect",
            title = "Inspect Webtoon Request",
            description = "Validate a public WEBTOON series URL and return the safe command template without downloading images. Use export_metadata for upstream metadata extraction."
    )
    @McpToolScopes({
            McpToolScope.EXTERNAL_API_READ,
            McpToolScope.PROCESS_EXECUTE
    })
    public DispatchExecutionResult inspect(WebtoonInspectArgs arguments, McpCallContext context) {
        try {
            String url = validWebtoonUrl(arguments == null ? null : arguments.url());
            ObjectNode structured = baseResponse(false);
            structured.put("url", url);
            structured.put("valid", true);
            structured.put("metadataNote", "The current upstream CLI exposes metadata export through --export-metadata; it does not document a separate inspect-only network command.");
            structured.set("metadataCommandTemplate", commandJson(metadataCommand(url, outputRoot, "json", null, null, false, "exponential", 1, 5, false)));
            return ok("WEBTOON URL validated.", structured);
        } catch (Exception exception) {
            return failure("WEBTOON_INVALID_ARGUMENTS", "Webtoon inspect failed.", exception, null);
        }
    }

    @McpConfigureMapping(timeoutMs = 900_000, audit = true, debugTrace = true)
    @McpFunction(
            value = "export_metadata",
            title = "Export Webtoon Metadata",
            description = "Run Zehina/Webtoon-Downloader with --export-metadata for a specific public WEBTOON series URL under the configured output root."
    )
    @McpToolScopes({
            McpToolScope.PROCESS_EXECUTE,
            McpToolScope.HTTP_CLIENT,
            McpToolScope.EXTERNAL_API_READ,
            McpToolScope.FILES_WRITE,
            McpToolScope.FILES_LIST
    })
    public DispatchExecutionResult exportMetadata(WebtoonExportMetadataArgs arguments, McpCallContext context) {
        try {
            String url = validWebtoonUrl(arguments == null ? null : arguments.url());
            validateRange(arguments.start(), arguments.end(), Boolean.TRUE.equals(arguments.latest()));
            Path outputDirectory = resolveOutputDirectory(arguments.outputSubdirectory(), url, "metadata");
            String exportFormat = option(arguments.exportFormat(), "json", EXPORT_FORMATS, "exportFormat");
            String retryStrategy = option(arguments.retryStrategy(), "exponential", RETRY_STRATEGIES, "retryStrategy");
            int concurrentChapters = capped(arguments.concurrentChapters(), 1, config.maxConcurrentChapters(), "concurrentChapters");
            int concurrentPages = capped(arguments.concurrentPages(), 5, config.maxConcurrentPages(), "concurrentPages");
            List<String> command = metadataCommand(
                    url,
                    outputDirectory,
                    exportFormat,
                    arguments.start(),
                    arguments.end(),
                    Boolean.TRUE.equals(arguments.latest()),
                    retryStrategy,
                    concurrentChapters,
                    concurrentPages,
                    Boolean.TRUE.equals(arguments.debug())
            );
            return runCli("Webtoon metadata export completed.", command, outputDirectory, true);
        } catch (Exception exception) {
            return failure("WEBTOON_EXPORT_FAILED", "Webtoon metadata export failed.", exception, null);
        }
    }

    @McpConfigureMapping(timeoutMs = 900_000, audit = true, debugTrace = true)
    @McpFunction(
            value = "download_series",
            title = "Download Webtoon Series",
            description = "Download public chapters for one user-provided WEBTOON series URL. Does not bypass paid, Daily Pass, Fast Pass, app-only, or protected chapters."
    )
    @McpToolScopes({
            McpToolScope.PROCESS_EXECUTE,
            McpToolScope.HTTP_CLIENT,
            McpToolScope.EXTERNAL_API_READ,
            McpToolScope.FILES_WRITE,
            McpToolScope.FILES_LIST,
            McpToolScope.FILES_ARCHIVE
    })
    public DispatchExecutionResult downloadSeries(WebtoonDownloadSeriesArgs arguments, McpCallContext context) {
        try {
            String url = validWebtoonUrl(arguments == null ? null : arguments.url());
            validateRange(arguments.start(), arguments.end(), Boolean.TRUE.equals(arguments.latest()));
            String saveAs = option(arguments.saveAs(), "cbz", SAVE_AS, "saveAs");
            if (Boolean.TRUE.equals(arguments.separate()) && !"images".equals(saveAs)) {
                throw new IllegalArgumentException("separate is valid only when saveAs is images");
            }
            String exportFormat = option(arguments.exportFormat(), "json", EXPORT_FORMATS, "exportFormat");
            String retryStrategy = option(arguments.retryStrategy(), "exponential", RETRY_STRATEGIES, "retryStrategy");
            String imageFormat = optional(arguments.imageFormat(), IMAGE_FORMATS, "imageFormat");
            Integer quality = quality(arguments.quality());
            int concurrentChapters = capped(arguments.concurrentChapters(), 1, config.maxConcurrentChapters(), "concurrentChapters");
            int concurrentPages = capped(arguments.concurrentPages(), 5, config.maxConcurrentPages(), "concurrentPages");
            Path outputDirectory = resolveOutputDirectory(arguments.outputSubdirectory(), url, "download");

            List<String> command = new ArrayList<>();
            command.add(config.command());
            command.add(url);
            addRange(command, arguments.start(), arguments.end(), Boolean.TRUE.equals(arguments.latest()));
            command.add("--out");
            command.add(outputDirectory.toString());
            command.add("--save-as");
            command.add(saveAs);
            if (Boolean.TRUE.equals(arguments.separate())) {
                command.add("--separate");
            }
            if (imageFormat != null) {
                command.add("--image-format");
                command.add(imageFormat);
            }
            if (quality != null) {
                command.add("--quality");
                command.add(Integer.toString(quality));
            }
            if (Boolean.TRUE.equals(arguments.exportMetadata())) {
                command.add("--export-metadata");
                command.add("--export-format");
                command.add(exportFormat);
            }
            addReliability(command, retryStrategy, concurrentChapters, concurrentPages, Boolean.TRUE.equals(arguments.debug()));

            return runCli("Webtoon series download completed.", command, outputDirectory, true);
        } catch (Exception exception) {
            return failure("WEBTOON_DOWNLOAD_FAILED", "Webtoon download failed.", exception, null);
        }
    }

    private DispatchExecutionResult runCli(String summary, List<String> command, Path outputDirectory, boolean mutating)
            throws IOException, InterruptedException, WebtoonCliException {
        Files.createDirectories(outputDirectory);
        WebtoonDownloaderCommandResult result = runner.run(command, outputRoot, config.timeout());
        if (result.timedOut()) {
            throw new WebtoonCliException("webtoon-downloader timed out after " + config.timeout().toSeconds() + " seconds", result);
        }
        if (result.exitCode() != 0) {
            throw new WebtoonCliException("webtoon-downloader exited with code " + result.exitCode(), result);
        }

        ObjectNode structured = baseResponse(mutating);
        structured.put("outputDirectory", outputDirectory.toString());
        structured.set("command", commandJson(command));
        structured.set("result", commandResultJson(result));
        structured.set("outputFiles", outputFiles(outputDirectory));
        return ok(summary, structured);
    }

    private DispatchExecutionResult ok(String summary, ObjectNode structured) {
        structured.put("ok", true);
        return DispatchExecutionResult.builder()
                .appendContent(ResultContent.json(structured))
                .structuredContent(structured)
                .status("ok")
                .summary(summary)
                .build();
    }

    private DispatchExecutionResult failure(String code, String summary, Exception exception, WebtoonDownloaderCommandResult result) {
        ObjectNode structured = baseResponse(false);
        structured.put("ok", false);
        structured.put("exceptionType", exception.getClass().getName());
        structured.put("message", exception.getMessage() == null ? "" : exception.getMessage());
        WebtoonDownloaderCommandResult commandResult = result;
        if (exception instanceof WebtoonCliException cliException) {
            commandResult = cliException.result();
        }
        if (commandResult != null) {
            structured.set("result", commandResultJson(commandResult));
        }
        return DispatchExecutionResult.builder()
                .structuredContent(structured)
                .error(code, exception.getMessage() == null ? summary : exception.getMessage())
                .status("failed")
                .summary(summary)
                .build();
    }

    private ObjectNode baseResponse(boolean mutating) {
        ObjectNode structured = objectMapper.createObjectNode();
        structured.put("ok", false);
        structured.put("mutating", mutating);
        structured.put("receivedAt", Instant.now().toString());
        structured.put("disclaimer", WebtoonDownloaderConstants.DISCLAIMER);
        return structured;
    }

    private List<String> metadataCommand(
            String url,
            Path outputDirectory,
            String exportFormat,
            Integer start,
            Integer end,
            boolean latest,
            String retryStrategy,
            int concurrentChapters,
            int concurrentPages,
            boolean debug
    ) {
        List<String> command = new ArrayList<>();
        command.add(config.command());
        command.add(url);
        addRange(command, start, end, latest);
        command.add("--out");
        command.add(outputDirectory.toString());
        command.add("--export-metadata");
        command.add("--export-format");
        command.add(exportFormat);
        addReliability(command, retryStrategy, concurrentChapters, concurrentPages, debug);
        return command;
    }

    private void addRange(List<String> command, Integer start, Integer end, boolean latest) {
        if (latest) {
            command.add("--latest");
            return;
        }
        if (start != null) {
            command.add("--start");
            command.add(Integer.toString(start));
        }
        if (end != null) {
            command.add("--end");
            command.add(Integer.toString(end));
        }
    }

    private void addReliability(List<String> command, String retryStrategy, int concurrentChapters, int concurrentPages, boolean debug) {
        command.add("--retry-strategy");
        command.add(retryStrategy);
        command.add("--concurrent-chapters");
        command.add(Integer.toString(concurrentChapters));
        command.add("--concurrent-pages");
        command.add(Integer.toString(concurrentPages));
        if (debug) {
            command.add("--debug");
        }
    }

    private String validWebtoonUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("url is required");
        }
        URI uri = URI.create(raw.trim());
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getPath() == null ? "" : uri.getPath();
        String query = uri.getQuery() == null ? "" : uri.getQuery();
        if (!"https".equals(scheme)) {
            throw new IllegalArgumentException("url must use https");
        }
        if (!"www.webtoons.com".equals(host) && !"webtoons.com".equals(host)) {
            throw new IllegalArgumentException("url must point to webtoons.com");
        }
        if (!path.endsWith("/list") && !path.contains("/list/")) {
            throw new IllegalArgumentException("url must be a WEBTOON series list page");
        }
        if (!query.contains("title_no=")) {
            throw new IllegalArgumentException("url must include title_no");
        }
        return uri.toString();
    }

    private Path resolveOutputDirectory(String requested, String url, String prefix) throws IOException {
        String leaf = requested == null || requested.isBlank() ? prefix + "-" + safeTitleNo(url) : requested.trim();
        Path relative = Path.of(leaf);
        if (relative.isAbsolute()) {
            throw new IllegalArgumentException("outputSubdirectory must be relative");
        }
        Path resolved = outputRoot.resolve(relative).toAbsolutePath().normalize();
        if (!resolved.startsWith(outputRoot)) {
            throw new IllegalArgumentException("outputSubdirectory must stay inside the configured output root");
        }
        Files.createDirectories(outputRoot);
        return resolved;
    }

    private String safeTitleNo(String url) {
        int index = url.indexOf("title_no=");
        if (index < 0) {
            return "series";
        }
        String value = url.substring(index + "title_no=".length());
        int ampersand = value.indexOf('&');
        if (ampersand >= 0) {
            value = value.substring(0, ampersand);
        }
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void validateRange(Integer start, Integer end, boolean latest) {
        if (latest && (start != null || end != null)) {
            throw new IllegalArgumentException("latest cannot be combined with start or end");
        }
        if (start != null && start < 1) {
            throw new IllegalArgumentException("start must be greater than zero");
        }
        if (end != null && end < 1) {
            throw new IllegalArgumentException("end must be greater than zero");
        }
        if (start != null && end != null && start > end) {
            throw new IllegalArgumentException("start cannot be greater than end");
        }
    }

    private String option(String raw, String fallback, Set<String> allowed, String name) {
        String value = raw == null || raw.isBlank() ? fallback : raw.trim().toLowerCase(Locale.ROOT);
        if (!allowed.contains(value)) {
            throw new IllegalArgumentException(name + " must be one of " + allowed);
        }
        return value;
    }

    private String optional(String raw, Set<String> allowed, String name) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return option(raw, null, allowed, name);
    }

    private int capped(Integer raw, int fallback, int max, String name) {
        int value = raw == null ? fallback : raw;
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
        return Math.min(value, max);
    }

    private Integer quality(Integer raw) {
        if (raw == null) {
            return null;
        }
        if (!QUALITIES.contains(raw)) {
            throw new IllegalArgumentException("quality must be one of " + QUALITIES);
        }
        return raw;
    }

    private ArrayNode commandJson(List<String> command) {
        ArrayNode array = objectMapper.createArrayNode();
        command.forEach(array::add);
        return array;
    }

    private ObjectNode commandResultJson(WebtoonDownloaderCommandResult result) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("exitCode", result.exitCode());
        node.put("timedOut", result.timedOut());
        node.put("stdout", truncate(result.stdout()));
        node.put("stderr", truncate(result.stderr()));
        node.set("command", commandJson(result.command()));
        return node;
    }

    private ArrayNode outputFiles(Path outputDirectory) throws IOException {
        ArrayNode files = objectMapper.createArrayNode();
        if (!Files.exists(outputDirectory)) {
            return files;
        }
        try (var stream = Files.walk(outputDirectory, 3)) {
            stream.filter(Files::isRegularFile)
                    .limit(50)
                    .forEach(path -> files.add(outputDirectory.relativize(path).toString()));
        }
        return files;
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        int max = 12_000;
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "\n... truncated ...";
    }
}
