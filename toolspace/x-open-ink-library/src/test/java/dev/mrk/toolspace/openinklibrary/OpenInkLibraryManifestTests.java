package dev.mrk.toolspace.openinklibrary;

import dev.mrk.meshingress.scopes.McpToolScope;
import dev.mrk.meshingress.toolmetadata.ToolRequirement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenInkLibraryManifestTests {

    private final OpenInkLibraryManifest manifest = new OpenInkLibraryManifest();

    @Test
    void documentsTheInstalledPublicToonverseSourceWithoutInventingRuntimeProperties() {
        assertEquals("open-ink-library", manifest.metadata().namespace());
        assertEquals(List.of("books", "comics", "manga", "manhwa", "toonverse", "webtoon"), manifest.metadata().tags());
        assertTrue(manifest.properties().isEmpty());
        assertEquals(List.of("Toonverse public API", McpToolScope.HTTP_CLIENT.name(), McpToolScope.EXTERNAL_API_READ.name()),
                manifest.requirements().stream().map(ToolRequirement::name).toList());
        assertTrue(manifest.readme().toString().contains("download-book"));
        assertTrue(manifest.readme().toString().contains("no host-editable `application.properties` fields"));
    }
}
