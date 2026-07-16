package dev.mrk.meshingress.mcp.tools.runtime;

import dev.mrk.meshingress.artifact.storage.FileSystemArtifactStorage;
import dev.mrk.meshingress.runtime.artifacts.ToolArtifactResolutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ToolRuntimeLoaderConfigurationTests {

    @TempDir
    Path temporaryDirectory;

    @Test
    void usesTheArtifactRepositoryAsTheLocalMavenRoot() {
        FileSystemArtifactStorage artifactStorage = new FileSystemArtifactStorage(temporaryDirectory.resolve("repository"));

        ToolArtifactResolutionContext context = new ToolRuntimeLoaderConfiguration()
                .toolArtifactResolutionContext(artifactStorage);

        assertThat(context.localRepository())
                .isEqualTo(temporaryDirectory.resolve("repository/artifacts").toAbsolutePath().normalize());
    }
}
