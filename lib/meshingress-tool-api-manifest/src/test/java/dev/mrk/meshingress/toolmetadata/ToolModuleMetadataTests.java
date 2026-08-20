package dev.mrk.meshingress.toolmetadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolModuleMetadataTests {

    @TempDir
    Path tempDir;

    @Test
    void readsSchemaOneManifestIntoStructuredMetadata() throws Exception {
        Path manifest = tempDir.resolve("tool-manifest.json");
        Files.writeString(manifest, """
                {"schemaVersion":1,"toolId":"youtube","links":[{"kind":"documentation","url":"https://example.test/docs","label":"Docs"}],"properties":[],"requirements":[],"readme":{"markdown":"# Legacy"}}
                """);

        McpToolNativeMetadata metadata = McpToolManifestJson.read(manifest);

        assertThat(metadata.schemaVersion()).isEqualTo(2);
        assertThat(metadata.metadata().namespace()).isEqualTo("youtube");
        assertThat(metadata.metadata().links()).hasSize(1);
        assertThat(metadata.readme()).isEqualTo(ToolReadme.inline("# Legacy"));
    }

    @Test
    void iconMustStayInsideResourcesAndRespectTheSizeLimit() throws Exception {
        ToolIcon icon = new ToolIcon("icons/youtube.svg", ToolIcon.MimeType.SVG_IMAGE, "YouTube");
        Path iconPath = tempDir.resolve("icons/youtube.svg");
        Files.createDirectories(iconPath.getParent());
        Files.writeString(iconPath, "<svg/>");

        assertThat(icon.requireValidResource(tempDir)).isEqualTo(iconPath);
        assertThatThrownBy(() -> new ToolIcon("../outside.svg", ToolIcon.MimeType.SVG_IMAGE, ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void iconFilenameMatchesNamespaceWithoutPrescribingItsDirectory() {
        assertThat(new ToolIcon("youtube.svg", ToolIcon.MimeType.SVG_IMAGE, "YouTube").hasNamespaceFileName("youtube")).isTrue();
        assertThat(new ToolIcon("branding/youtube.svg", ToolIcon.MimeType.SVG_IMAGE, "YouTube").hasNamespaceFileName("youtube")).isTrue();
        assertThat(new ToolIcon("logo.svg", ToolIcon.MimeType.SVG_IMAGE, "YouTube").hasNamespaceFileName("youtube")).isFalse();
        assertThat(new ToolIcon("youtube.png", ToolIcon.MimeType.SVG_IMAGE, "YouTube").hasNamespaceFileName("youtube")).isFalse();
    }

    @Test
    void normalizesMetadataWithoutAllowingDottedNamespaces() {
        ToolModuleMetadata metadata = new ToolModuleMetadata("youtube", " YouTube ", "", "", List.of(ToolAuthor.individual("Example")),
                "", List.of("media", "media", " youtube "), List.of(), null);

        assertThat(metadata.tags()).containsExactly("media", "youtube");
        assertThatThrownBy(() -> new ToolModuleMetadata("cli.powershell", "", "", "", List.of(), "", List.of(), List.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void authorSupportsNormalizedIndependentUrlReferences() throws Exception {
        ToolAuthor author = new ToolAuthor(" Example ", new String[] {
                " https://example.test/profile ",
                "mailto:example@example.test",
                "https://example.test/profile",
                " "
        });
        Path manifest = tempDir.resolve("tool-manifest.json");
        McpToolManifestJson.write(new McpToolNativeMetadata(2,
                new ToolModuleMetadata("example", "", "", "", List.of(author), "", List.of(), List.of(), null),
                List.of(), List.of(), ToolReadme.none()), manifest);

        String[] returnedUrls = author.urls();
        returnedUrls[0] = "https://mutated.test";
        McpToolNativeMetadata read = McpToolManifestJson.read(manifest);

        assertThat(author.name()).isEqualTo("Example");
        assertThat(author.urls()).containsExactly("https://example.test/profile", "mailto:example@example.test");
        assertThat(read.metadata().authors()).singleElement().satisfies(parsed ->
                assertThat(parsed.urls()).containsExactly("https://example.test/profile", "mailto:example@example.test"));
    }
}
