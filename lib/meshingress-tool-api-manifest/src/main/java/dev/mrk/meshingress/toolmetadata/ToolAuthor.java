package dev.mrk.meshingress.toolmetadata;

import java.net.URI;
import java.util.Arrays;

/** Declared attribution for a tool module; it is not verified provenance. */
public record ToolAuthor(String name, String[] urls) {
    public ToolAuthor {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("tool author name must not be blank");
        }
        name = name.strip();
        urls = Arrays.stream(urls == null ? new String[0] : urls)
                .filter(url -> url != null && !url.isBlank())
                .map(String::strip)
                .peek(URI::create)
                .distinct()
                .toArray(String[]::new);
    }

    public static ToolAuthor individual(String name) {
        return new ToolAuthor(name, new String[0]);
    }

    @Override
    public String[] urls() {
        return urls.clone();
    }
}
