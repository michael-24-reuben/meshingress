package dev.mrk.toolspace.openinklibrary.source.toonverse;

import dev.mrk.toolspace.openinklibrary.SourceException;
import dev.mrk.toolspace.openinklibrary.Utility;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ToonverseSourceConfigurationLoader {

    public ToonverseSourceConfiguration load(InputStream input) {
        Objects.requireNonNull(input, "input must not be null");
        Object loaded = new Yaml(new SafeConstructor(new LoaderOptions())).load(input);
        if (!(loaded instanceof Map<?, ?> raw)) {
            throw new SourceException("INVALID_SOURCE_CONFIGURATION", "Toonverse source configuration must be a YAML mapping.");
        }

        String id = required(raw, "id");
        String displayName = required(raw, "displayName");
        URI apiBaseUrl = absoluteHttpsUri(required(raw, "apiBaseUrl"));
        String metadataPath = required(raw, "metadataPath");
        String metadataSelector = required(raw, "metadataSelector");
        String nameSearchPath = required(raw, "nameSearchPath");
        String nameSearchSelector = required(raw, "nameSearchSelector");
        String searchPath = required(raw, "searchPath");
        String searchSelector = required(raw, "searchSelector");
        String readingChapterPath = required(raw, "readingChapterPath");
        String readingChapterSelector = required(raw, "readingChapterSelector");
        String chaptersPath = required(raw, "chaptersPath");
        String chaptersSelector = required(raw, "chaptersSelector");
        long timeoutMs = positiveLong(raw, "timeoutMs");
        ToonverseSourceConfiguration.AuthorizationMode authorizationMode = authorizationMode(required(raw, "authMode"));
        int defaultSearchLimit = positiveInt(raw, "defaultSearchLimit");
        int maximumSearchLimit = positiveInt(raw, "maximumSearchLimit");
        int defaultChapterLimit = positiveInt(raw, "defaultChapterLimit");
        int maximumChapterLimit = positiveInt(raw, "maximumChapterLimit");
        String defaultChapterOrder = chapterOrder(required(raw, "defaultChapterOrder"));

        if (!metadataPath.startsWith("/") || !metadataPath.contains("{slug}") || hasUnknownPlaceholder(metadataPath)) {
            throw new SourceException("INVALID_SOURCE_CONFIGURATION", "metadataPath must be absolute and use only the {slug} placeholder.");
        }
        if (!metadataSelector.startsWith("$.")) {
            throw new SourceException("INVALID_SOURCE_CONFIGURATION", "metadataSelector must start with $.");
        }
        if (!absolutePath(nameSearchPath) || !nameSearchSelector.startsWith("$.")) {
            throw new SourceException("INVALID_SOURCE_CONFIGURATION", "nameSearchPath must be absolute and nameSearchSelector must start with $.");
        }
        if (!absolutePath(searchPath) || !searchSelector.startsWith("$.")) {
            throw new SourceException("INVALID_SOURCE_CONFIGURATION", "searchPath must be absolute and searchSelector must start with $.");
        }
        if (!readingChapterPath.startsWith("/") || !readingChapterPath.contains("{slug}") || !readingChapterPath.contains("{chapterNumber}")
                || hasUnknownPlaceholders(readingChapterPath, "{slug}", "{chapterNumber}") || !readingChapterSelector.startsWith("$.")) {
            throw new SourceException("INVALID_SOURCE_CONFIGURATION", "readingChapterPath must use only the {slug} and {chapterNumber} placeholders, and readingChapterSelector must start with $.");
        }
        if (!chaptersPath.startsWith("/") || !chaptersPath.contains("{seriesId}") || hasUnknownPlaceholder(chaptersPath, "{seriesId}")) {
            throw new SourceException("INVALID_SOURCE_CONFIGURATION", "chaptersPath must be absolute and use only the {seriesId} placeholder.");
        }
        if (!chaptersSelector.startsWith("$.")) {
            throw new SourceException("INVALID_SOURCE_CONFIGURATION", "chaptersSelector must start with $.");
        }
        if (defaultChapterLimit > maximumChapterLimit) {
            throw new SourceException("INVALID_SOURCE_CONFIGURATION", "defaultChapterLimit must not exceed maximumChapterLimit.");
        }
        if (defaultSearchLimit > maximumSearchLimit) {
            throw new SourceException("INVALID_SOURCE_CONFIGURATION", "defaultSearchLimit must not exceed maximumSearchLimit.");
        }

        return new ToonverseSourceConfiguration(
                Utility.normalize(id),
                displayName,
                apiBaseUrl,
                metadataPath,
                metadataSelector,
                nameSearchPath,
                nameSearchSelector,
                searchPath,
                searchSelector,
                readingChapterPath,
                readingChapterSelector,
                chaptersPath,
                chaptersSelector,
                Duration.ofMillis(timeoutMs),
                authorizationMode,
                defaultSearchLimit,
                maximumSearchLimit,
                defaultChapterLimit,
                maximumChapterLimit,
                defaultChapterOrder
        );
    }

    private static String required(Map<?, ?> values, String key) {
        Object value = values.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new SourceException("INVALID_SOURCE_CONFIGURATION", key + " must be a non-blank string.");
        }
        return text.trim();
    }

    private static long positiveLong(Map<?, ?> values, String key) {
        Object value = values.get(key);
        if (!(value instanceof Number number) || number.longValue() <= 0) {
            throw new SourceException("INVALID_SOURCE_CONFIGURATION", key + " must be a positive number.");
        }
        return number.longValue();
    }

    private static int positiveInt(Map<?, ?> values, String key) {
        long value = positiveLong(values, key);
        if (value > Integer.MAX_VALUE) {
            throw new SourceException("INVALID_SOURCE_CONFIGURATION", key + " is too large.");
        }
        return (int) value;
    }

    private static URI absoluteHttpsUri(String value) {
        try {
            URI uri = URI.create(value);
            if (!uri.isAbsolute() || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw new SourceException("INVALID_SOURCE_CONFIGURATION", "apiBaseUrl must be an absolute HTTPS URL.", exception);
        }
    }

    private static ToonverseSourceConfiguration.AuthorizationMode authorizationMode(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "none" -> ToonverseSourceConfiguration.AuthorizationMode.NONE;
            case "optional-bearer" -> ToonverseSourceConfiguration.AuthorizationMode.OPTIONAL_BEARER;
            case "bearer" -> ToonverseSourceConfiguration.AuthorizationMode.BEARER;
            default -> throw new SourceException("INVALID_SOURCE_CONFIGURATION", "authMode must be none, optional-bearer, or bearer.");
        };
    }

    private static String chapterOrder(String value) {
        String order = value.trim().toLowerCase(Locale.ROOT);
        if (!order.equals("asc") && !order.equals("desc")) {
            throw new SourceException("INVALID_SOURCE_CONFIGURATION", "defaultChapterOrder must be asc or desc.");
        }
        return order;
    }

    private static boolean hasUnknownPlaceholder(String path) {
        return hasUnknownPlaceholder(path, "{slug}");
    }

    private static boolean hasUnknownPlaceholder(String path, String allowedPlaceholder) {
        String remaining = path.replace(allowedPlaceholder, "");
        return remaining.contains("{") || remaining.contains("}");
    }

    private static boolean absolutePath(String path) {
        return path.startsWith("/") && !path.contains("{") && !path.contains("}");
    }

    private static boolean hasUnknownPlaceholders(String path, String... allowed) {
        String remaining = path;
        for (String placeholder : allowed) {
            remaining = remaining.replace(placeholder, "");
        }
        return remaining.contains("{") || remaining.contains("}");
    }
}
