package dev.mrk.toolspace.openinklibrary;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.tools.annotation.McpConfigureMapping;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.McpToolMapping;
import dev.mrk.meshingress.api.tools.annotation.McpToolScopes;
import dev.mrk.meshingress.scopes.McpToolScope;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

@McpTool(
        value = "book.image",
        title = "Open Ink Library: Image Books",
        description = "Retrieve normalized metadata for installed manga, manhwa, webtoon, comic, and graphic-novel sources."
)
@McpToolScopes({McpToolScope.HTTP_CLIENT, McpToolScope.EXTERNAL_API_READ})
@McpToolMapping("tools")
public final class ImageBookTool {
    private final BookSourceRegistry<ImageBookSource> sources;
    private final ObjectMapper objectMapper;

    public ImageBookTool(BookSourceRegistry<ImageBookSource> sources, ObjectMapper objectMapper) {
        this.sources = Objects.requireNonNull(sources, "sources must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @McpConfigureMapping(timeoutMs = 20_000, audit = true)
    @McpFunction(value = "sources", title = "List image-book sources", description = "List installed image-book source adapters and capabilities.")
    public DispatchExecutionResult sources(McpCallContext context) {
        JsonNode payload = objectMapper.valueToTree(sources.list());
        return DispatchExecutionResult.builder()
                .array(payload)
                .structuredContent(payload)
                .status("completed")
                .summary("Listed installed image-book sources.")
                .build();
    }

    @McpConfigureMapping(timeoutMs = 30_000, audit = true)
    @McpFunction(value = "metadata", title = "Get image-book metadata", description = "Retrieve normalized metadata from an installed image-book source.")
    public DispatchExecutionResult metadata(ImageBookMetadataArgs arguments, McpCallContext context) {
        try {
            if (arguments == null) {
                throw new SourceException("SOURCE_RESPONSE_INVALID", "Tool arguments are required.");
            }
            ImageBookMetadataSource source = sources.requireCapability(arguments.source(), ImageBookMetadataSource.class);
            ImageBookMetadata metadata = source.fetchMetadata(new ImageBookMetadataRequest(arguments.slug(), arguments.authToken()));
            JsonNode payload = objectMapper.valueToTree(metadata);
            return DispatchExecutionResult.builder()
                    .object(payload)
                    .structuredContent(payload)
                    .status("completed")
                    .summary("Retrieved image-book metadata from " + source.displayName() + ".")
                    .build();
        } catch (SourceException exception) {
            return failed(exception.code(), exception.getMessage());
        } catch (RuntimeException exception) {
            return failed("SOURCE_REQUEST_FAILED", "The image-book source request could not be completed.");
        }
    }

    @McpConfigureMapping(timeoutMs = 30_000, audit = true)
    @McpFunction(value = "chapters", title = "List image-book chapters", description = "Retrieve normalized chapter metadata from an installed image-book source.")
    public DispatchExecutionResult chapters(ImageBookChaptersArgs arguments, McpCallContext context) {
        try {
            if (arguments == null) {
                throw new SourceException("SOURCE_RESPONSE_INVALID", "Tool arguments are required.");
            }
            ImageBookChapterSource source = sources.requireCapability(arguments.source(), ImageBookChapterSource.class);
            JsonNode payload = objectMapper.valueToTree(source.fetchChapters(new ImageBookChapterListRequest(
                    arguments.sourceBookId(), arguments.limit(), arguments.order(), arguments.authToken()
            )));
            return DispatchExecutionResult.builder()
                    .array(payload)
                    .structuredContent(payload)
                    .status("completed")
                    .summary("Retrieved image-book chapters from " + source.displayName() + ".")
                    .build();
        } catch (SourceException exception) {
            return failed(exception.code(), exception.getMessage());
        } catch (RuntimeException exception) {
            return failed("SOURCE_REQUEST_FAILED", "The image-book source request could not be completed.");
        }
    }

    @McpConfigureMapping(timeoutMs = 45_000, audit = true)
    @McpFunction(value = "extract", title = "Extract image book", description = "Retrieve canonical metadata and chapter index in one source-owned request sequence.")
    public DispatchExecutionResult extract(ImageBookExtractArgs arguments, McpCallContext context) {
        try {
            if (arguments == null) {
                throw new SourceException("SOURCE_RESPONSE_INVALID", "Tool arguments are required.");
            }
            ImageBookMetadataSource metadataSource = sources.requireCapability(arguments.source(), ImageBookMetadataSource.class);
            ImageBookChapterSource chapterSource = sources.requireCapability(arguments.source(), ImageBookChapterSource.class);
            ImageBookMetadata metadata = metadataSource.fetchMetadata(new ImageBookMetadataRequest(arguments.slug(), arguments.authToken()));
            var chapters = chapterSource.fetchChapters(new ImageBookChapterListRequest(
                    metadata.sourceWorkId(), arguments.limit(), arguments.order(), arguments.authToken()
            ));
            ImageBookResult extraction = toResult(metadata, chapters);
            return DispatchExecutionResult.builder()
                    .object(objectMapper.valueToTree(extraction))
                    .structuredContent(objectMapper.valueToTree(extraction))
                    .status("completed")
                    .summary("Extracted image-book metadata and chapters from " + metadataSource.displayName() + ".")
                    .build();
        } catch (SourceException exception) {
            return failed(exception.code(), exception.getMessage());
        } catch (RuntimeException exception) {
            return failed("SOURCE_REQUEST_FAILED", "The image-book source request could not be completed.");
        }
    }

    private static ImageBookResult toResult(ImageBookMetadata metadata, java.util.List<ImageBookChapterMetadata> chapters) {
        java.util.List<ImageBookResult.Cover> covers = metadata.coverUrl() == null
                ? java.util.List.of()
                : java.util.List.of(new ImageBookResult.Cover(metadata.coverUrl()));
        return new ImageBookResult(
                1,
                BookKind.IMAGE_BOOK,
                new ImageBookResult.Source(metadata.sourceId(), metadata.sourceWorkId(), metadata.sourceUrl()),
                new ImageBookResult.Work(
                        metadata.title(), metadata.alternativeTitles(), metadata.synopsis(), metadata.authors(), metadata.artists(),
                        metadata.publisher(), metadata.publicationYear(), metadata.publicationType(), metadata.publicationStatus(),
                        metadata.genres(), covers
                ),
                chapters,
                new ImageBookResult.Metrics(metadata.chapterCount())
        );
    }

    private static DispatchExecutionResult failed(String code, String message) {
        return DispatchExecutionResult.builder()
                .error(code, message)
                .status("failed")
                .summary(message)
                .build();
    }
}
