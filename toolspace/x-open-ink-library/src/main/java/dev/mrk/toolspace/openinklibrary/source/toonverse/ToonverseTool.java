package dev.mrk.toolspace.openinklibrary.source.toonverse;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.progress.McpProgressReporter;
import dev.mrk.meshingress.api.storage.ToolStorageException;
import dev.mrk.meshingress.api.storage.ToolStorageFile;
import dev.mrk.meshingress.api.storage.ToolStorageFileRequest;
import dev.mrk.meshingress.api.storage.ToolStorageService;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspace;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspaceRequest;
import dev.mrk.meshingress.api.storage.ToolStorageTransferMode;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.tools.annotation.McpConfigureMapping;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.McpToolMapping;
import dev.mrk.meshingress.api.tools.annotation.McpToolScopes;
import dev.mrk.meshingress.scopes.McpToolScope;
import dev.mrk.toolspace.openinklibrary.SourceException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.LinkedHashMap;
import java.util.Map;

@McpTool(value = "toonverse", title = "Toonverse", description = "Search and fetch public Toonverse series data.")
@McpToolScopes({McpToolScope.HTTP_CLIENT, McpToolScope.EXTERNAL_API_READ})
@McpToolMapping("tools")
public final class ToonverseTool {
    private static final Pattern H2_NULL_COLUMN = Pattern.compile("(?i)\\bnull not allowed for column \\\"([A-Za-z_][A-Za-z0-9_]*)\\\"");
    // 2.5s/chapter is a rough estimate for planning progress reporting; actual download time may vary based on network conditions and source responsiveness.
    private static final Duration ESTIMATED_CHAPTER_DOWNLOAD_DURATION = Duration.ofMillis(2_500);

    private final ToonverseClient client;
    private final ObjectMapper objectMapper;
    private final ToolStorageService storage;

    public ToonverseTool(ToonverseClient client, ObjectMapper objectMapper) {
        this(client, objectMapper, null);
    }

    public ToonverseTool(ToonverseClient client, ObjectMapper objectMapper, ToolStorageService storage) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.storage = storage;
    }

    @McpConfigureMapping(timeoutMs = 30_000, audit = true)
    @McpFunction(value = "search", title = "Search Toonverse", description = "Search Toonverse using name, genre, type, chapter, rating, author, status, and sorting filters.")
    public DispatchExecutionResult search(ToonverseSearchArgs arguments, McpCallContext context) {
        try {
            if (arguments == null) {
                throw new SourceException("SOURCE_RESPONSE_INVALID", "Tool arguments are required.");
            }
            return completed(dataResponse(client.searchSeries(arguments.toRequest())), "Searched Toonverse.");
        } catch (SourceException exception) {
            return failed(exception.code(), exception.getMessage());
        } catch (RuntimeException exception) {
            return failed("SOURCE_REQUEST_FAILED", "The Toonverse search request could not be completed.");
        }
    }


    @McpConfigureMapping(timeoutMs = 30_000, audit = true)
    @McpFunction(value = "fetch", title = "Fetch Toonverse work", description = "Fetch one Toonverse work by its exact name or Toonverse slug.")
    public DispatchExecutionResult fetch(ToonverseFetchArgs arguments, McpCallContext context) {
        try {
            JsonNode metadata = fetchMetadata(arguments);
            return completed(dataResponse(metadata), "Fetched Toonverse work metadata.");
        } catch (SourceException exception) {
            return failed(exception.code(), exception.getMessage());
        } catch (RuntimeException exception) {
            return failed("SOURCE_REQUEST_FAILED", "The Toonverse fetch request could not be completed.");
        }
    }

    @McpConfigureMapping(timeoutMs = 30_000, audit = true)
    @McpFunction(value = "fetch-full", title = "Fetch complete Toonverse work", description = "Fetch Toonverse metadata and a requested chapter page by exact name or Toonverse slug.")
    public DispatchExecutionResult fetchFull(ToonverseFetchFullArgs arguments, McpCallContext context) {
        try {
            if (arguments == null) {
                throw new SourceException("SOURCE_RESPONSE_INVALID", "Tool arguments are required.");
            }
            JsonNode metadata = fetchMetadata(arguments.name());
            JsonNode chapterPage = client.fetchSeriesChapterPage(requiredText(metadata, "id"), arguments.limit(), arguments.offset(), arguments.order(), null);
            JsonNode payload = bookResponse(metadata, chapterPage);
            return completed(payload, "Fetched Toonverse work metadata and chapters.");
        } catch (SourceException exception) {
            return failed(exception.code(), exception.getMessage());
        } catch (RuntimeException exception) {
            return failed("SOURCE_REQUEST_FAILED", "The Toonverse fetch request could not be completed.");
        }
    }

    @McpConfigureMapping(timeoutMs = 30_000, audit = true)
    @McpFunction(value = "fetch-chapter", title = "Fetch Toonverse chapter", description = "Fetch one Toonverse chapter reader payload by exact work name or slug and chapter number.")
    public DispatchExecutionResult fetchChapter(ToonverseFetchChapterArgs arguments, McpCallContext context) {
        try {
            if (arguments == null) {
                throw new SourceException("SOURCE_RESPONSE_INVALID", "Tool arguments are required.");
            }
            return completed(dataResponse(client.fetchReadingChapter(resolveSlug(arguments.name()), arguments.chapterNumber())), "Fetched Toonverse chapter.");
        } catch (SourceException exception) {
            return failed(exception.code(), exception.getMessage());
        } catch (RuntimeException exception) {
            return failed("SOURCE_REQUEST_FAILED", "The Toonverse chapter request could not be completed.");
        }
    }

    @McpConfigureMapping(timeoutMs = 30_000, audit = true)
    @McpFunction(value = "fetch-chapters", title = "Fetch Toonverse chapters", description = "Fetch a range of Toonverse chapter reader payload by exact work name or slug and a chapter number range.")
    public DispatchExecutionResult fetchChapters(ToonverseFetchChaptersArgs arguments, McpCallContext context) {
        try {
            if (arguments == null) {
                throw new SourceException("SOURCE_RESPONSE_INVALID", "Tool arguments are required.");
            }
            validateChapterRange(arguments.minChapterNumber(), arguments.maxChapterNumber());
            String slug = resolveSlug(arguments.name());
            List<JsonNode> chapters = new ArrayList<>();
            for (long chapterNumber = arguments.minChapterNumber(); chapterNumber <= arguments.maxChapterNumber(); chapterNumber++) {
                chapters.add(client.fetchReadingChapter(slug, (int) chapterNumber));
            }
            return completed(dataResponse(objectMapper.valueToTree(chapters)), "Fetched " + chapters.size() + " Toonverse chapter reader payloads.");
        } catch (SourceException exception) {
            return failed(exception.code(), exception.getMessage());
        } catch (RuntimeException exception) {
            return failed("SOURCE_REQUEST_FAILED", "The Toonverse chapter request could not be completed.");
        }
    }

    @McpConfigureMapping(timeoutMs = 900_000, audit = true)
    @McpFunction(value = "download-book", title = "Download Toonverse book", description = "Download an inclusive Toonverse chapter range into a temporary Meshingress book workspace with local page files and descriptors.")
    public DispatchExecutionResult downloadBook(ToonverseDownloadBookArgs arguments, McpCallContext context, McpProgressReporter progress) {
        try {
            if (arguments == null) throw new SourceException("SOURCE_RESPONSE_INVALID", "Tool arguments are required.");
            validateChapterRange(arguments.minChapterNumber(), arguments.maxChapterNumber());
            int requestedChapterCount = Math.toIntExact((long) arguments.maxChapterNumber() - arguments.minChapterNumber() + 1);
            progress.plan(
                    ESTIMATED_CHAPTER_DOWNLOAD_DURATION.multipliedBy(requestedChapterCount),
                    requestedChapterCount,
                    List.of("preparing", "downloading", "packaging"),
                    "Preparing to download " + requestedChapterCount + " Toonverse chapter(s)."
            );
            progress.update("preparing", 0, requestedChapterCount, "Resolving the Toonverse series and validating the requested chapter range.");
            ToolStorageService storage = requireStorage();
            String slug = resolveSlug(arguments.name());
            JsonNode firstPayload = client.fetchReadingChapter(slug, arguments.minChapterNumber());
            validateDownloadRange(firstPayload, arguments.minChapterNumber(), arguments.maxChapterNumber());
            ToolStorageWorkspaceRequest workspaceRequest = new ToolStorageWorkspaceRequest(
                    storageTtl(arguments.ttlSeconds()), arguments.maxRequests(), ToolStorageTransferMode.DELEGATED_SOURCE_URLS
            );
            ToolStorageWorkspace workspace = storage.openWorkspace("toonverse.download-book", context, workspaceRequest);
            List<Map<String, Object>> chapters = new ArrayList<>();
            JsonNode series = null;
            long pageCount = 0;
            for (long requestedChapter = arguments.minChapterNumber(); requestedChapter <= arguments.maxChapterNumber(); requestedChapter++) {
                int number = Math.toIntExact(requestedChapter);
                int completedChapters = chapters.size();
                progress.update("Downloading chapter " + number + " (" + (completedChapters + 1) + " of " + requestedChapterCount + ").");
                JsonNode payload = number == arguments.minChapterNumber() ? firstPayload : client.fetchReadingChapter(slug, number);
                JsonNode chapter = payload.path("chapter");
                if (!chapter.isObject()) throw new SourceException("SOURCE_RESPONSE_INVALID", "The Toonverse reader response is missing chapter data.");
                if (series == null && payload.path("series").isObject()) series = payload.path("series");
                int chapterNumber = chapter.path("number").isInt() ? chapter.path("number").asInt() : number;
                String directory = "chapters/" + String.format(Locale.ROOT, "%04d", chapterNumber);
                List<Map<String, Object>> pages = downloadPages(storage, workspace, directory, chapter.path("pages"));
                pageCount += pages.size();
                Map<String, Object> descriptor = new LinkedHashMap<>();
                descriptor.put("number", chapterNumber);
                descriptor.put("title", chapter.path("title").asString("Chapter " + chapterNumber));
                if (!chapter.path("publishedAt").asString().isBlank()) descriptor.put("publishedAt", chapter.path("publishedAt").asString());
                descriptor.put("pages", pages);
                writeJson(storage, workspace, directory + "/chapter.json", descriptor);
                chapters.add(Map.of("number", chapterNumber, "path", directory + "/chapter.json"));
                progress.update(
                        "downloading",
                        chapters.size(),
                        requestedChapterCount,
                        "Downloaded chapter " + chapterNumber + " (" + chapters.size() + " of " + requestedChapterCount + ")."
                );
            }
            progress.update("packaging", requestedChapterCount, requestedChapterCount, "Writing the book descriptor and cover.");
            Map<String, Object> book = bookDescriptor(arguments.name(), slug, series, chapters);
            if (series != null && !series.path("coverUrl").asString().isBlank()) {
                book.put("cover", downloadCover(storage, workspace, series.path("coverUrl").asString()));
            }
            writeJson(storage, workspace, "book.json", book);
            progress.update("Publishing the Toonverse book workspace.");

            ToolStorageWorkspace published = storage.publish(workspace);
            JsonNode response = objectMapper.valueToTree(new ToonverseDownloadResponse("toonverse", published, "book.json", chapters.size(), pageCount));
            progress.complete("Downloaded " + chapters.size() + " Toonverse chapter(s) into storage.");
            return completed(response, "Downloaded " + chapters.size() + " Toonverse chapters into storage.");
        } catch (SourceException exception) {
            progress.error(exception.getMessage());
            return failed(exception.code(), exception.getMessage());
        } catch (ToolStorageException exception) {
            String message = storageFailureMessage(exception);
            progress.error(message);
            return failed("STORAGE_REQUEST_FAILED", message);
        } catch (RuntimeException exception) {
            progress.error("The Toonverse book download could not be completed.");
            return failed("SOURCE_REQUEST_FAILED", "The Toonverse book download could not be completed.");
        }
    }

    private JsonNode fetchMetadata(ToonverseFetchArgs arguments) {
        if (arguments == null) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", "Tool arguments are required.");
        }
        return fetchMetadata(arguments.name());
    }

    private List<Map<String, Object>> downloadPages(ToolStorageService storage, ToolStorageWorkspace workspace, String directory, JsonNode sourcePages) {
        if (!sourcePages.isArray())
            throw new SourceException("SOURCE_RESPONSE_INVALID", "The Toonverse chapter response is missing pages.");

        List<Map<String, Object>> pages = new ArrayList<>();
        for (JsonNode page : sourcePages) {
            if (page.path("hidden").asBoolean(false)) continue;
            int pageNumber = page.path("number").isInt() ? page.path("number").asInt() : pages.size() + 1;
            String imageUrl = page.path("imageUrl").asString();

            if (workspace.transferMode() == ToolStorageTransferMode.DELEGATED_SOURCE_URLS) {
                String extension = extension(imageUrl, "application/octet-stream");
                String path = directory + "/" + String.format(Locale.ROOT, "%03d", pageNumber) + extension;
                storage.delegateFile(workspace, java.net.URI.create(imageUrl), path);
                Map<String, Object> descriptor = new LinkedHashMap<>();
                descriptor.put("number", pageNumber);
                descriptor.put("path", path.substring((directory + "/").length()));
                descriptor.put("mimeType", mimeTypeForExtension(extension));
                if (page.path("width").isInt()) descriptor.put("width", page.path("width").asInt());
                if (page.path("height").isInt()) descriptor.put("height", page.path("height").asInt());
                pages.add(descriptor);
                continue;
            }

            try (ToonverseClient.DownloadedMedia media = client.downloadMedia(imageUrl)) {
                String path = directory + "/" + String.format(Locale.ROOT, "%03d", pageNumber) + extension(imageUrl, media.mimeType());
                ToolStorageFile stored = storage.writeFile(workspace, path, media.content(), new ToolStorageFileRequest(media.mimeType()));
                Map<String, Object> descriptor = new LinkedHashMap<>();
                descriptor.put("number", pageNumber);
                descriptor.put("path", stored.relativePath().substring((directory + "/").length()));
                descriptor.put("mimeType", stored.mimeType());
                descriptor.put("byteSize", stored.byteSize());
                if (page.path("width").isInt()) descriptor.put("width", page.path("width").asInt());
                if (page.path("height").isInt()) descriptor.put("height", page.path("height").asInt());
                pages.add(descriptor);
            } catch (java.io.IOException exception) {
                throw new SourceException("SOURCE_REQUEST_FAILED", "The Toonverse page stream could not be closed.", exception);
            }
        }
        return List.copyOf(pages);
    }

    private String downloadCover(ToolStorageService storage, ToolStorageWorkspace workspace, String coverUrl) {
        if (workspace.transferMode() == ToolStorageTransferMode.DELEGATED_SOURCE_URLS) {
            String path = "cover" + extension(coverUrl, "application/octet-stream");
            storage.delegateFile(workspace, java.net.URI.create(coverUrl), path);
            return path;
        }
        try (ToonverseClient.DownloadedMedia media = client.downloadMedia(coverUrl)) {
            ToolStorageFile stored = storage.writeFile(workspace, "cover" + extension(coverUrl, media.mimeType()), media.content(), new ToolStorageFileRequest(media.mimeType()));
            return stored.relativePath();
        } catch (java.io.IOException exception) {
            throw new SourceException("SOURCE_REQUEST_FAILED", "The Toonverse cover stream could not be closed.", exception);
        }
    }

    private Map<String, Object> bookDescriptor(String requestedName, String slug, JsonNode series, List<Map<String, Object>> chapters) {
        Map<String, Object> book = new LinkedHashMap<>();
        book.put("type", "open-ink.book/v1");
        book.put("source", "toonverse");
        book.put("title", series == null ? requestedName : series.path("title").asString(requestedName));
        book.put("slug", series == null ? slug : series.path("slug").asString(slug));
        if (series != null && !series.path("type").asString().isBlank()) book.put("publicationType", series.path("type").asString());
        if (series != null && series.path("genres").isArray()) book.put("genres", objectMapper.convertValue(series.path("genres"), List.class));
        book.put("chapters", chapters);
        return book;
    }

    private void writeJson(ToolStorageService storage, ToolStorageWorkspace workspace, String relativePath, Object value) {
        try {
            storage.writeFile(workspace, relativePath, new ByteArrayInputStream(objectMapper.writeValueAsBytes(value)), new ToolStorageFileRequest("application/json"));
        } catch (Exception exception) {
            if (exception instanceof ToolStorageException storageException) throw storageException;
            throw new SourceException("SOURCE_RESPONSE_INVALID", "The Toonverse book descriptor could not be written.", exception);
        }
    }

    private ToolStorageService requireStorage() {
        if (storage == null) throw new ToolStorageException("Tool storage is not available in this server.");
        return storage;
    }

    private static Duration storageTtl(Integer ttlSeconds) {
        if (ttlSeconds == null) return null;
        if (ttlSeconds <= 0) throw new SourceException("SOURCE_RESPONSE_INVALID", "ttlSeconds must be a positive integer.");
        return Duration.ofSeconds(ttlSeconds.longValue());
    }

    private static String extension(String url, String mimeType) {
        try {
            String path = java.net.URI.create(url).getPath();
            int dot = path == null ? -1 : path.lastIndexOf('.');
            if (dot >= 0 && dot < path.length() - 1) {
                String extension = path.substring(dot).toLowerCase(Locale.ROOT);
                if (extension.matches("\\.[a-z0-9]{1,10}")) return extension;
            }
        } catch (IllegalArgumentException ignored) {
        }
        return switch (mimeType) {
            case "image/webp" -> ".webp";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            default -> ".bin";
        };
    }

    private static String mimeTypeForExtension(String extension) {
        return switch (extension) {
            case ".webp" -> "image/webp";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }

    private JsonNode fetchMetadata(String name) {
        return client.fetchSeriesMetadata(resolveSlug(name), null);
    }

    private String resolveSlug(String name) {
        JsonNode candidates = client.searchSeriesByName(name);
        return exactSlug(candidates, name);
    }

    private JsonNode dataResponse(JsonNode data) {
        return objectMapper.valueToTree(new ToonverseResponse("toonverse", data));
    }

    private JsonNode bookResponse(JsonNode metadata, JsonNode chapters) {

        return objectMapper.valueToTree(new ToonverseBookResponse("toonverse", metadata, chapters));
    }

    private DispatchExecutionResult completed(JsonNode payload, String summary) {
        return DispatchExecutionResult.builder()
                .object(payload)
                .structuredContent(payload)
                .status("completed")
                .summary(summary)
                .build();
    }

    private static String exactSlug(JsonNode candidates, String name) {
        if (!candidates.isArray()) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", "The Toonverse name-search payload must be an array.");
        }
        String expected = normalizeName(name);
        for (JsonNode candidate : candidates) {
            String title = candidate.path("title").asString();
            String slug = candidate.path("slug").asString();
            if (!slug.isBlank() && (normalizeName(title).equals(expected) || normalizeName(slug).equals(expected))) {
                return slug;
            }
        }
        throw new SourceException("SOURCE_WORK_NOT_FOUND", "No exact Toonverse work matched the supplied name.");
    }

    private static String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", "name must be a non-blank string.");
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s_-]+", "-");
    }

    private static String requiredText(JsonNode value, String field) {
        String text = value.path(field).asString();
        if (text.isBlank()) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", "The Toonverse metadata response is missing '" + field + "'.");
        }
        return text;
    }

    private static void validateChapterRange(Integer minChapterNumber, Integer maxChapterNumber) {
        if (minChapterNumber == null || maxChapterNumber == null || minChapterNumber < 0 || maxChapterNumber < 0) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", "minChapterNumber and maxChapterNumber must be non-negative integers.");
        }
        if (minChapterNumber > maxChapterNumber) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", "minChapterNumber must not exceed maxChapterNumber.");
        }
    }

    private static void validateDownloadRange(JsonNode readerPayload, int minChapterNumber, int maxChapterNumber) {
        JsonNode available = readerPayload.path("chapterNumbers");
        if (!available.isArray()) {
            return;
        }
        java.util.Set<Integer> availableNumbers = new java.util.HashSet<>();
        for (JsonNode number : available) {
            if (number.canConvertToInt()) {
                availableNumbers.add(number.asInt());
            }
        }
        for (long number = minChapterNumber; number <= maxChapterNumber; number++) {
            if (!availableNumbers.contains((int) number)) {
                throw new SourceException(
                        "SOURCE_RESPONSE_INVALID",
                        "The requested chapter range is unavailable on Toonverse; chapter " + number + " is not published."
                );
            }
        }
    }

    private static DispatchExecutionResult failed(String code, String message) {
        return DispatchExecutionResult.builder().error(code, message).status("failed").summary(message).build();
    }

    static String storageFailureMessage(ToolStorageException exception) {
        Throwable cause = exception.getCause();
        if (cause == null) return exception.getMessage();
        Throwable root = cause;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        if (root instanceof SQLException sqlException && sqlException.getSQLState() != null && !sqlException.getSQLState().isBlank()) {
            String message = exception.getMessage() + " [database SQLState=" + sqlException.getSQLState();
            Matcher nullColumn = H2_NULL_COLUMN.matcher(sqlException.getMessage() == null ? "" : sqlException.getMessage());
            if (nullColumn.find()) message += ", null column=" + nullColumn.group(1).toLowerCase(Locale.ROOT);
            return message + "]";
        }
        return exception.getMessage() + " [storage cause=" + root.getClass().getSimpleName() + "]";
    }

    private record ToonverseResponse(String source, JsonNode data) {
    }

    private record ToonverseBookResponse(String source, JsonNode metadata, JsonNode chapterPage) {
    }

    private record ToonverseDownloadResponse(String source, ToolStorageWorkspace workspace, String bookPath, int chapterCount, long pageCount) {
    }
}
