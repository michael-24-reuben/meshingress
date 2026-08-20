package dev.mrk.meshingress.toolmetadata;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Public authored metadata for one module contribution to a tool namespace. */
public record ToolModuleMetadata(
        @NotNull String namespace,
        String title,
        String summary,
        String description,
        List<ToolAuthor> authors,
        String license,
        List<String> tags,
        List<ToolLink> links,
        ToolIcon icon
) {
    public ToolModuleMetadata {
        assert namespace != null : "Namespace cannot be null";

        if (!namespace.isEmpty() && !namespace.matches("[a-z][a-z0-9_-]*")) {
            throw new IllegalArgumentException("tool namespace must use one lower-case segment: " + namespace);
        }
        title = clean(title);
        summary = clean(summary);
        description = clean(description);
        authors = authors == null ? List.of() : List.copyOf(authors);
        license = clean(license);
        tags = tags == null ? List.of() : tags.stream().filter(tag -> tag != null && !tag.isBlank()).map(String::strip).distinct().sorted().toList();
        links = links == null ? List.of() : List.copyOf(links);
    }

    public static ToolModuleMetadata empty() {
        return new ToolModuleMetadata("", "", "", "", List.of(), "", List.of(), List.of(), null);
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
