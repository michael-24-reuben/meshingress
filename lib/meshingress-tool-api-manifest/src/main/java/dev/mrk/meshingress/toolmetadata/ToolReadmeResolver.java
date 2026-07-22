package dev.mrk.meshingress.toolmetadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class ToolReadmeResolver {

    public String resolve(ToolReadme readme) {
        if (readme == null || readme.metadata().type() == ToolReadme.ReadmeType.None) {
            return "";
        }

        return switch (readme) {
            case ToolReadme.None ignored -> "";

            case ToolReadme.Inline inline -> inline.markdown();

            case ToolReadme.Classpath classpath -> readClasspathResource(classpath);

            case ToolReadme.File file -> readFile(file);
        };
    }

    private String readClasspathResource(ToolReadme.Classpath source) {
        try (InputStream input = source.anchor()
                .getResourceAsStream(source.resourcePath())) {

            if (input == null) {
                throw new IllegalStateException(
                        "README resource not found: " + source.resourcePath()
                );
            }

            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Failed to read README resource: " + source.resourcePath(),
                    exception
            );
        }
    }

    private String readFile(ToolReadme.File source) {
        try {
            return Files.readString(source.path(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Failed to read README file: " + source.path(),
                    exception
            );
        }
    }
}