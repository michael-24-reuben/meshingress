package dev.mrk.toolspace.openinklibrary.source.toonverse;

import dev.mrk.toolspace.openinklibrary.*;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public final class ToonverseMapper {

    public ImageBookMetadata toMetadata(JsonNode source) {
        String id = requiredText(source, "id");
        String slug = requiredText(source, "slug");
        return new ImageBookMetadata(
                "toonverse",
                id,
                "https://toonverse.net/series/" + slug,
                requiredText(source, "title"),
                stringList(source.path("alternativeNames")),
                firstNonBlank(text(source, "synopsis"), text(source, "description")),
                singleton(text(source, "author")),
                singleton(text(source, "artist")),
                text(source, "publisher"),
                text(source, "year"),
                PublicationType.fromSourceValue(text(source, "type")),
                PublicationStatus.fromSourceValue(text(source, "status")),
                genreNames(source.path("genres")),
                text(source, "coverUrl"),
                optionalInt(source, "chapterCount")
        );
    }

    public List<ImageBookChapterMetadata> toChapters(JsonNode source) {
        if (!source.isArray()) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", "The source chapter payload must be an array.");
        }
        List<ImageBookChapterMetadata> chapters = new ArrayList<>();
        for (JsonNode chapter : source) {
            String id = requiredText(chapter, "id");
            chapters.add(new ImageBookChapterMetadata(
                    id,
                    chapter.path("number").isNumber() ? chapter.path("number").asDouble() : null,
                    text(chapter, "title"),
                    text(chapter, "publishedAt"),
                    text(chapter, "url"),
                    chapter.path("reads").isIntegralNumber() ? chapter.path("reads").asLong() : null,
                    List.of()
            ));
        }
        return List.copyOf(chapters);
    }

    public ImageBookResult toResult(ImageBookMetadata metadata, List<ImageBookChapterMetadata> chapters) {
        List<ImageBookResult.Cover> covers = metadata.coverUrl() == null ? List.of() : List.of(new ImageBookResult.Cover(metadata.coverUrl()));
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

    private static String requiredText(JsonNode source, String field) {
        String value = text(source, field);
        if (value == null) {
            throw new SourceException("SOURCE_RESPONSE_INVALID", "The source metadata is missing '" + field + "'.");
        }
        return value;
    }

    private static String text(JsonNode source, String field) {
        JsonNode value = source.path(field);
        if (value.isMissingNode() || value.isNull() || value.asString().isBlank()) {
            return null;
        }
        return value.asString().trim();
    }

    private static Integer optionalInt(JsonNode source, String field) {
        JsonNode value = source.path(field);
        return value.isInt() || value.isLong() ? value.asInt() : null;
    }

    private static List<String> singleton(String value) {
        return value == null ? List.of() : List.of(value);
    }

    private static List<String> stringList(JsonNode values) {
        List<String> output = new ArrayList<>();
        if (values.isArray()) {
            for (JsonNode value : values) {
                if (value.isString() && !value.asString().isBlank()) {
                    output.add(value.asString().trim());
                }
            }
        }
        return List.copyOf(output);
    }

    private static List<String> genreNames(JsonNode genres) {
        List<String> output = new ArrayList<>();
        if (genres.isArray()) {
            for (JsonNode genre : genres) {
                String name = text(genre, "name");
                if (name != null) {
                    output.add(name);
                }
            }
        }
        return List.copyOf(output);
    }

    private static String firstNonBlank(String first, String second) {
        return first != null ? first : second;
    }
}
