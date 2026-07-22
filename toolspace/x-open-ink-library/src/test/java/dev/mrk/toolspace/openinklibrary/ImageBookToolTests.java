package dev.mrk.toolspace.openinklibrary;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageBookToolTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returnsAStructuredMetadataObjectThroughTheGeneralTool() {
        ImageBookTool tool = new ImageBookTool(
                new BookSourceRegistry<>(List.of(new FixedSource())),
                objectMapper
        );

        var result = tool.metadata(new ImageBookMetadataArgs("toonverse", "solo-leveling", null), null);

        assertEquals(false, result.isError());
        assertEquals("Solo Leveling", result.content().getFirst().value().path("title").asString());
        assertEquals("completed", result.toJson(objectMapper).path("_meta").path("status").asString());
    }

    @Test
    void returnsAStableErrorForUnknownSources() {
        ImageBookTool tool = new ImageBookTool(new BookSourceRegistry<>(List.of()), objectMapper);

        var result = tool.metadata(new ImageBookMetadataArgs("missing", "anything", null), null);

        assertEquals(true, result.isError());
        assertEquals("UNKNOWN_BOOK_SOURCE", result.toJson(objectMapper).path("_meta").path("errorCode").asString());
    }

    @Test
    void extractKeepsTheMetadataIdInsideTheSourceOwnedChapterRequest() {
        ImageBookTool tool = new ImageBookTool(
                new BookSourceRegistry<>(List.of(new FixedSource())),
                objectMapper
        );

        var result = tool.extract(new ImageBookExtractArgs("toonverse", "solo-leveling", 10, "asc", null), null);

        assertEquals(false, result.isError());
        assertEquals("series-1", result.content().getFirst().value().path("source").path("workId").asString());
        assertEquals("chapter-1", result.content().getFirst().value().path("chapters").get(0).path("sourceChapterId").asString());
    }

    private static final class FixedSource implements ImageBookMetadataSource, ImageBookChapterSource {
        @Override
        public String sourceId() {
            return "toonverse";
        }

        @Override
        public String displayName() {
            return "Toonverse";
        }

        @Override
        public BookSourceDescriptor descriptor() {
            return new BookSourceDescriptor(sourceId(), displayName(), bookKind(), Set.of("metadata"));
        }

        @Override
        public ImageBookMetadata fetchMetadata(ImageBookMetadataRequest request) {
            return new ImageBookMetadata(
                    "toonverse", "series-1", "https://toonverse.net/series/solo-leveling", "Solo Leveling",
                    List.of(), "Synopsis", List.of("Chugong"), List.of(), "Daum", "2018",
                    PublicationType.MANHWA, PublicationStatus.ONGOING, List.of("Fantasy"), null, 205
            );
        }

        @Override
        public List<ImageBookChapterMetadata> fetchChapters(ImageBookChapterListRequest request) {
            assertEquals("series-1", request.sourceWorkId());
            return List.of(new ImageBookChapterMetadata("chapter-1", 1.0, "Chapter 1", null, null, null, List.of()));
        }
    }
}
