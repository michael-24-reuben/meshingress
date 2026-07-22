package dev.mrk.toolspace.openinklibrary;

import dev.mrk.toolspace.openinklibrary.source.toonverse.ToonverseMapper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToonverseMapperTests {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ToonverseMapper mapper = new ToonverseMapper();

    @Test
    void mapsCanonicalMetadataAndFallsBackToDescription() throws Exception {
        ImageBookMetadata metadata = mapper.toMetadata(fixture("toonverse-metadata.json"));

        assertEquals("series-1", metadata.sourceWorkId());
        assertEquals("A hunter story", metadata.synopsis());
        assertEquals(PublicationType.MANHWA, metadata.publicationType());
        assertEquals(PublicationStatus.ONGOING, metadata.publicationStatus());
        assertEquals(205, metadata.chapterCount());
        assertEquals(java.util.List.of("Fantasy", "Action"), metadata.genres());
    }

    @Test
    void mapsChapterFixtureAndProducesTheRequiredCanonicalExtractShape() throws Exception {
        ImageBookMetadata metadata = mapper.toMetadata(fixture("toonverse-metadata.json"));
        List<ImageBookChapterMetadata> chapters = mapper.toChapters(fixture("toonverse-chapters.json"));
        var result = objectMapper.valueToTree(mapper.toResult(metadata, chapters));

        assertEquals(1, result.path("schemaVersion").asInt());
        assertEquals("IMAGE_BOOK", result.path("bookKind").asString());
        assertEquals("toonverse", result.path("source").path("id").asString());
        assertEquals("Solo Leveling", result.path("work").path("title").asString());
        assertEquals("MANHWA", result.path("work").path("publicationType").asString());
        assertEquals("ONGOING", result.path("work").path("status").asString());
        assertEquals("chapter-2", result.path("chapters").get(0).path("sourceChapterId").asString());
        assertEquals(true, result.path("chapters").get(0).path("pages").isArray());
    }

    private tools.jackson.databind.JsonNode fixture(String name) throws Exception {
        try (var input = getClass().getResourceAsStream("/fixtures/" + name)) {
            return objectMapper.readTree(input);
        }
    }
}
