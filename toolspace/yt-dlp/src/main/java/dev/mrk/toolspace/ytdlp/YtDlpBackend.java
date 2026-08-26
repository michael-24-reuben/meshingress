package dev.mrk.toolspace.ytdlp;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

final class YtDlpBackend {
    private static final String OUTPUT_TEMPLATE = "%(title).120B [%(id)s].%(ext)s";
    private final ObjectMapper objectMapper;
    private final YtDlpProperties properties;
    private int numOfTrials = 0;

    YtDlpBackend(ObjectMapper objectMapper, YtDlpProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    JsonNode inspect(String url) throws YtDlpException {
        validateUrl(url);
        ProcessResult result = run(commandForUrl(url, "--no-playlist", "--skip-download", "--dump-single-json"));
        if (result.exitCode() != 0) {
            throw failed(result);
        }
        try {
            return objectMapper.readTree(result.stdout());
        } catch (RuntimeException exception) {
            throw new YtDlpException("YT_DLP_INVALID_RESPONSE", "yt-dlp did not return valid metadata JSON.");
        }
    }

    JsonNode download(String url, String format) throws YtDlpException {
        validateUrl(url);
        validateFormat(format);
        Path downloadDirectory = downloadDirectory();
        List<String> options = new ArrayList<>(List.of(
                "--no-playlist",
                "--no-progress",
                "--restrict-filenames",
                "--print", "after_move:filepath",
                "--paths", downloadDirectory.toString(),
                "--output", OUTPUT_TEMPLATE
        ));
        if (format != null && !format.isBlank()) {
            options.add("--format");
            options.add(format.trim());
        }
        return writeLocalFiles(url, downloadDirectory, options);
    }

    JsonNode formats(String url) throws YtDlpException {
        JsonNode metadata = inspect(url);
        ObjectNode response = metadataEnvelope(metadata);
        JsonNode formats = metadata.get("formats");
        response.set("formats", formats == null ? objectMapper.createArrayNode() : formats);
        response.put("formatCount", formats == null ? 0 : formats.size());
        return response;
    }

    JsonNode subtitles(String url) throws YtDlpException {
        JsonNode metadata = inspect(url);
        ObjectNode response = metadataEnvelope(metadata);
        JsonNode subtitles = metadata.get("subtitles");
        JsonNode automaticCaptions = metadata.get("automatic_captions");
        response.set("subtitles", subtitles == null ? objectMapper.createObjectNode() : subtitles);
        response.set("automaticCaptions", automaticCaptions == null ? objectMapper.createObjectNode() : automaticCaptions);
        response.put("subtitleLanguageCount", subtitles == null ? 0 : subtitles.size());
        response.put("automaticCaptionLanguageCount", automaticCaptions == null ? 0 : automaticCaptions.size());
        return response;
    }

    JsonNode downloadSubtitles(String url, String languages, String format, boolean includeAutomaticCaptions) throws YtDlpException {
        validateUrl(url);
        validateSubtitleLanguages(languages);
        validateSubtitleFormat(format);
        Path downloadDirectory = downloadDirectory();
        List<String> options = new ArrayList<>(List.of(
                "--no-playlist",
                "--no-progress",
                "--restrict-filenames",
                "--skip-download",
                "--write-subs",
                "--sub-langs", languages.trim(),
                "--print", "after_move:filepath",
                "--paths", downloadDirectory.toString(),
                "--output", OUTPUT_TEMPLATE
        ));
        if (includeAutomaticCaptions) {
            options.add("--write-auto-subs");
        }
        if (format != null && !format.isBlank()) {
            options.add("--sub-format");
            options.add(format.trim());
        }
        return writeLocalFiles(url, downloadDirectory, options);
    }

    JsonNode thumbnails(String url) throws YtDlpException {
        JsonNode metadata = inspect(url);
        ObjectNode response = metadataEnvelope(metadata);
        JsonNode thumbnails = metadata.get("thumbnails");
        response.set("thumbnails", thumbnails == null ? objectMapper.createArrayNode() : thumbnails);
        response.put("thumbnailCount", thumbnails == null ? 0 : thumbnails.size());
        return response;
    }

    JsonNode downloadThumbnails(String url, boolean all) throws YtDlpException {
        validateUrl(url);
        Path downloadDirectory = downloadDirectory();
        List<String> options = new ArrayList<>(List.of(
                "--no-playlist",
                "--no-progress",
                "--restrict-filenames",
                "--skip-download",
                all ? "--write-all-thumbnails" : "--write-thumbnail",
                "--print", "after_move:filepath",
                "--paths", downloadDirectory.toString(),
                "--output", OUTPUT_TEMPLATE
        ));
        return writeLocalFiles(url, downloadDirectory, options);
    }

    private JsonNode writeLocalFiles(String url, Path downloadDirectory, List<String> options) throws YtDlpException {
        ProcessResult result = run(commandForUrl(url, options));
        if (result.exitCode() != 0) {
            throw failed(result);
        }

        ArrayNode files = objectMapper.createArrayNode();
        for (String line : result.stdout().lines().filter(line -> !line.isBlank()).toList()) {
            Path file = Path.of(line.trim()).toAbsolutePath().normalize();
            if (!file.startsWith(downloadDirectory)) {
                throw new YtDlpException("YT_DLP_INVALID_OUTPUT", "yt-dlp reported a file outside the module download directory.");
            }
            files.add(file.toString());
        }
        ObjectNode response = objectMapper.createObjectNode();
        response.put("downloadDirectory", downloadDirectory.toString());
        response.set("files", files);
        response.put("fileCount", files.size());
        return response;
    }

    List<String> inspectCommand(String url) {
        return commandForUrl(url, "--no-playlist", "--skip-download", "--dump-single-json");
    }

    private List<String> commandForUrl(String url, String... options) {
        return commandForUrl(url, List.of(options));
    }

    private List<String> commandForUrl(String url, List<String> options) {
        List<String> command = new ArrayList<>();
        command.add(properties.pythonExecutable());
        command.add("-S");
        command.add("-m");
        command.add("yt_dlp");
        if (isYoutubeUrl(url)) {
            command.add("--js-runtimes");
            command.add(properties.nodeRuntime());
        }
        String impersonateClient = impersonateClientFor(url);
        if (impersonateClient != null) {
            command.add("--impersonate");
            command.add(impersonateClient);
        }
        command.addAll(options);
        command.add(url);
        return List.copyOf(command);
    }

    private ProcessResult run(List<String> command) throws YtDlpException {
        Path module = moduleDirectory();
        Path pythonSource = module.resolve("vendor/python");
        if (!Files.isDirectory(pythonSource.resolve("yt_dlp"))) {
            throw new YtDlpException("YT_DLP_RUNTIME_MISSING", "Vendored yt-dlp source is missing from " + pythonSource);
        }
        Path captureDirectory = module.resolve("runtime/command-output").normalize();
        if (!captureDirectory.startsWith(module)) {
            throw new YtDlpException("YT_DLP_INVALID_CONFIGURATION", "Command output must remain inside the local yt-dlp module.");
        }

        Path stdoutFile = null;
        Path stderrFile = null;
        boolean retainCaptures = false;
        try {
            Files.createDirectories(captureDirectory);
            stdoutFile = Files.createTempFile(captureDirectory, "yt-dlp-", ".stdout");
            stderrFile = Files.createTempFile(captureDirectory, "yt-dlp-", ".stderr");
            ProcessBuilder builder = new ProcessBuilder(command)
                    .directory(module.toFile())
                    .redirectOutput(stdoutFile.toFile())
                    .redirectError(stderrFile.toFile());
            builder.environment().put("PYTHONPATH", pythonSource.toString());
            builder.environment().put("NO_COLOR", "1");
            Process process = builder.start();
            if (!process.waitFor(properties.timeoutMs(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                retainCaptures = !process.waitFor(10, TimeUnit.SECONDS);
                throw new YtDlpException("YT_DLP_TIMEOUT", "yt-dlp exceeded " + properties.timeoutMs() + " ms.");
            }
            return new ProcessResult(process.exitValue(), readCapturedOutput(stdoutFile), readCapturedOutput(stderrFile));
        } catch (YtDlpException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new YtDlpException("YT_DLP_UNAVAILABLE", "Unable to run the configured Python/yt-dlp runtime: " + safeMessage(exception));
        } finally {
            if (!retainCaptures) {
                deleteIfExists(stdoutFile);
                deleteIfExists(stderrFile);
            }
        }
    }

    private String readCapturedOutput(Path output) throws IOException {
        try (InputStream input = Files.newInputStream(output)) {
            ByteArrayOutputStream captured = new ByteArrayOutputStream();
            byte[] buffer = new byte[8_192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                int remaining = properties.maxOutputChars() - captured.size();
                if (remaining > 0) {
                    captured.write(buffer, 0, Math.min(read, remaining));
                }
            }
            return captured.toString(StandardCharsets.UTF_8);
        }
    }

    private static void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // A failed cleanup only leaves a local diagnostic capture inside the module runtime.
        }
    }

    private Path moduleDirectory() throws YtDlpException {
        Path directory = Path.of(properties.moduleDirectory()).toAbsolutePath().normalize();
        if (!Files.isDirectory(directory)) {
            throw new YtDlpException("YT_DLP_MODULE_MISSING", "Local yt-dlp module directory is unavailable: " + directory);
        }
        return directory;
    }

    private Path downloadDirectory() throws YtDlpException {
        Path module = moduleDirectory();
        Path directory = module.resolve(properties.downloadDirectory()).normalize();
        if (!directory.startsWith(module)) {
            throw new YtDlpException("YT_DLP_INVALID_CONFIGURATION", "downloadDirectory must remain inside the local yt-dlp module.");
        }
        try {
            Files.createDirectories(directory);
            return directory;
        } catch (IOException exception) {
            throw new YtDlpException("YT_DLP_OUTPUT_UNAVAILABLE", "Unable to create local yt-dlp download directory.");
        }
    }

    private static void validateUrl(String url) throws YtDlpException {
        try {
            URI uri = URI.create(url == null ? "" : url.trim());
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new YtDlpException("INVALID_ARGUMENTS", "url must be a credential-free HTTP or HTTPS URL.");
            }
        } catch (IllegalArgumentException exception) {
            throw new YtDlpException("INVALID_ARGUMENTS", "url must be a valid HTTP or HTTPS URL.");
        }
    }

    private static void validateFormat(String format) throws YtDlpException {
        if (format == null || format.isBlank()) {
            return;
        }
        if (format.length() > 240 || !format.matches("[A-Za-z0-9_+\\-./,\\[\\]()=<>!&|*:\\s]+")) {
            throw new YtDlpException("INVALID_ARGUMENTS", "format contains unsupported characters.");
        }
    }

    private static void validateSubtitleLanguages(String languages) throws YtDlpException {
        if (languages == null || languages.isBlank() || languages.length() > 160 || !languages.matches("[A-Za-z0-9*._,-]+")) {
            throw new YtDlpException("INVALID_ARGUMENTS", "languages must be a comma-separated yt-dlp subtitle language selector.");
        }
    }

    private static void validateSubtitleFormat(String format) throws YtDlpException {
        if (format == null || format.isBlank()) {
            return;
        }
        if (format.length() > 120 || !format.matches("[A-Za-z0-9_+./,-]+")) {
            throw new YtDlpException("INVALID_ARGUMENTS", "subtitleFormat contains unsupported characters.");
        }
    }

    private static boolean isYoutubeUrl(String url) {
        try {
            String host = URI.create(url == null ? "" : url.trim()).getHost();
            if (host == null) {
                return false;
            }
            String normalized = host.toLowerCase(Locale.ROOT).replaceFirst("\\.$", "");
            return normalized.equals("youtube.com")
                    || normalized.endsWith(".youtube.com")
                    || normalized.equals("youtu.be")
                    || normalized.endsWith(".youtu.be")
                    || normalized.equals("youtube-nocookie.com")
                    || normalized.endsWith(".youtube-nocookie.com");
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isImpersonateHostUrl(String url) {
        try {
            String host = URI.create(url == null ? "" : url.trim()).getHost();
            if (host == null) {
                return false;
            }
            String normalized = host.toLowerCase(Locale.ROOT).replaceFirst("\\.$", "");
            return Arrays.stream(properties.impersonateHosts()).anyMatch(impersonateHost -> {
                String normalizedImpersonateHost = impersonateHost.toLowerCase(Locale.ROOT).replaceFirst("\\.$", "");
                return normalized.equals(normalizedImpersonateHost) || normalized.endsWith("." + normalizedImpersonateHost);
            });
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private @Nullable String impersonateClientFor(String url) {
        if (!isImpersonateHostUrl(url)) {
            return null;
        }
        String[] impersonateClients = properties.impersonateClients();
        return Arrays.stream(impersonateClients)
                .filter(client -> client != null && !client.isBlank())
                .map(String::trim)
                .skip(Math.clamp(numOfTrials, 0, impersonateClients.length - 1))
                .findFirst()
                .orElse(null);
    }

    private ObjectNode metadataEnvelope(JsonNode metadata) {
        ObjectNode response = objectMapper.createObjectNode();
        copyMetadataField(response, metadata, "id");
        copyMetadataField(response, metadata, "title");
        copyMetadataField(response, metadata, "webpage_url");
        copyMetadataField(response, metadata, "extractor");
        return response;
    }

    private static void copyMetadataField(ObjectNode target, JsonNode source, String field) {
        JsonNode value = source.get(field);
        if (value != null) {
            target.set(field, value);
        }
    }

    private static YtDlpException failed(ProcessResult result) {
        String detail = result.stderr().isBlank() ? result.stdout() : result.stderr();
        return new YtDlpException("YT_DLP_FAILED", "yt-dlp failed: " + truncate(detail));
    }

    private static String safeMessage(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank() ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private static String truncate(String value) {
        String normalized = value == null || value.isBlank() ? "no diagnostic output" : value.trim();
        return normalized.length() <= 4_000 ? normalized : normalized.substring(0, 4_000);
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) { }

}
