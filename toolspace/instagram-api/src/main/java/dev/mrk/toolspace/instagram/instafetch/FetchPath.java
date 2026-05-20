package dev.mrk.toolspace.instagram.instafetch;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FetchPath {
    private static final Pattern SHORTCODE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final Pattern INSTAGRAM_MEDIA_URL_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?instagram\\.com/(?:p|reel|tv)/([^/?#]+)",
            Pattern.CASE_INSENSITIVE
    );

    private final FetchPathKind kind;
    private final String value;
    private final String resolvedShortcode;

    private FetchPath(FetchPathKind kind, String value, String resolvedShortcode) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.value = requireText(value, "value");
        this.resolvedShortcode = requireText(resolvedShortcode, "resolvedShortcode");
    }

    public static FetchPath asUrl(String url) {
        String shortcode = getPostShortcode(url);
        if (shortcode == null) {
            throw new InstaFetchInvalidUrlException();
        }
        return new FetchPath(FetchPathKind.URL, url, shortcode);
    }

    public static FetchPath asShortcode(String shortcode) {
        String normalized = requireText(shortcode, "shortcode");
        if (!SHORTCODE_PATTERN.matcher(normalized).matches()) {
            throw new InstaFetchInvalidUrlException("Instagram shortcode contains invalid characters");
        }
        return new FetchPath(FetchPathKind.SHORTCODE, normalized, normalized);
    }

    public static FetchPath from(String input) {
        String normalized = requireText(input, "input");
        String shortcode = getPostShortcode(normalized);
        if (shortcode != null) {
            return asUrl(normalized);
        }
        return asShortcode(normalized);
    }

    public static FetchPath from(FetchPath input) {
        return Objects.requireNonNull(input, "input");
    }

    public FetchPathKind kind() {
        return kind;
    }

    public String value() {
        return value;
    }

    public String shortcode() {
        return resolvedShortcode;
    }

    @Override
    public String toString() {
        return value;
    }

    private static String getPostShortcode(String input) {
        if (input == null) {
            return null;
        }
        Matcher matcher = INSTAGRAM_MEDIA_URL_PATTERN.matcher(input);
        if (!matcher.find()) {
            return null;
        }
        String shortcode = matcher.group(1);
        return SHORTCODE_PATTERN.matcher(shortcode).matches() ? shortcode : null;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
