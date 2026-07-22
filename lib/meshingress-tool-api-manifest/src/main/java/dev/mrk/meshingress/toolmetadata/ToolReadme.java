package dev.mrk.meshingress.toolmetadata;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;
import java.util.Objects;

public sealed interface ToolReadme permits ToolReadme.None, ToolReadme.Inline, ToolReadme.Classpath, ToolReadme.File {

    enum ReadmeType {
        None,
        Inline,
        Classpath,
        File
    }

    record Metadata(ReadmeType type) {
        public Metadata {
            Objects.requireNonNull(type, "type must not be null");
        }
    }

    Metadata metadata();

    record None() implements ToolReadme {
        @Contract(value = " -> new", pure = true)
        @Override
        public @NonNull Metadata metadata() {
            return new Metadata(ReadmeType.None);
        }
    }

    record Inline(String markdown) implements ToolReadme {
        public Inline {
            Objects.requireNonNull(markdown, "markdown must not be null");
        }

        @Contract(value = " -> new", pure = true)
        @Override
        public @NonNull Metadata metadata() {
            return new Metadata(markdown.isBlank() ? ReadmeType.None : ReadmeType.Inline);
        }
    }

    record Classpath(Class<?> anchor, String resourcePath) implements ToolReadme {

        public Classpath {
            Objects.requireNonNull(anchor, "anchor must not be null");
            Objects.requireNonNull(resourcePath, "resourcePath must not be null");
        }

        @Contract(value = " -> new", pure = true)
        @Override
        public @NonNull Metadata metadata() {
            return new Metadata(resourcePath.isBlank() ? ReadmeType.None : ReadmeType.Classpath);
        }
    }

    record File(Path path) implements ToolReadme {
        @Contract(value = " -> new", pure = true)
        @Override
        public @NonNull Metadata metadata() {
            return new Metadata(path == null ? ReadmeType.None : ReadmeType.File);
        }
    }

    @Contract(" -> new")
    static @NonNull ToolReadme none() {
        return new None();
    }

    @Contract(value = "_ -> new", pure = true)
    static @NonNull ToolReadme inline(String markdown) {
        return new Inline(markdown);
    }

    @Contract(value = "_, _ -> new", pure = true)
    static @NonNull ToolReadme classpath(Class<?> anchor, String resourcePath) {
        return new Classpath(anchor, resourcePath);
    }

    @Contract("_ -> new")
    static @NonNull ToolReadme file(Path path) {
        return new File(path);
    }

    default Metadata getMetadata() {
        return metadata();
    }
}
