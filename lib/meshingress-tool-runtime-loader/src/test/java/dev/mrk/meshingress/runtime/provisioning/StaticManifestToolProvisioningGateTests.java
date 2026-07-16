package dev.mrk.meshingress.runtime.provisioning;

import dev.mrk.meshingress.provisioning.ProvisionedResource;
import dev.mrk.meshingress.provisioning.ProvisioningResult;
import dev.mrk.meshingress.provisioning.ProvisioningStatus;
import dev.mrk.meshingress.provisioning.ToolProvisioner;
import dev.mrk.meshingress.provisioning.ToolProvisioningService;
import dev.mrk.meshingress.provisioning.git.GitSourceProvisioningRequest;
import dev.mrk.meshingress.provisioning.python.PythonVenvProvisioningRequest;
import dev.mrk.meshingress.runtime.artifacts.ResolvedToolArtifact;
import dev.mrk.meshingress.runtime.lifecycle.ToolModuleId;
import dev.mrk.meshingress.toolmetadata.McpToolManifestJson;
import dev.mrk.meshingress.toolmetadata.McpToolNativeMetadata;
import dev.mrk.meshingress.toolmetadata.ToolReadme;
import dev.mrk.meshingress.toolmetadata.ToolRequirement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StaticManifestToolProvisioningGateTests {

    @TempDir
    Path tempDirectory;

    @Test
    void provisionsPinnedRequiredSourceBeforeActivation() throws Exception {
        ResolvedToolArtifact artifact = artifactWithManifest(ToolRequirement.sourceRepository("https://github.com/SYSTRAN/faster-whisper")
                .cloneUrl("https://github.com/SYSTRAN/faster-whisper.git")
                .checkoutRef("0123456789abcdef0123456789abcdef01234567"));
        AtomicReference<GitSourceProvisioningRequest> captured = new AtomicReference<>();
        StaticManifestToolProvisioningGate gate = new StaticManifestToolProvisioningGate(
                tempDirectory.resolve("repository"),
                new ToolProvisioningService(List.of(readyGitProvisioner(captured)))
        );

        gate.requireReady(artifact);

        assertEquals("github.com/SYSTRAN/faster-whisper", captured.get().canonicalIdentity());
        assertEquals("https://github.com/SYSTRAN/faster-whisper.git", captured.get().cloneUrl());
        assertEquals(tempDirectory.resolve("repository"), captured.get().repositoryRoot());
    }

    @Test
    void rejectsMutableRefBeforeInvokingProvisioner() throws Exception {
        ResolvedToolArtifact artifact = artifactWithManifest(ToolRequirement.sourceRepository("https://github.com/SYSTRAN/faster-whisper")
                .checkoutRef("main"));
        AtomicReference<GitSourceProvisioningRequest> captured = new AtomicReference<>();
        StaticManifestToolProvisioningGate gate = new StaticManifestToolProvisioningGate(
                tempDirectory.resolve("repository"),
                new ToolProvisioningService(List.of(readyGitProvisioner(captured)))
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> gate.requireReady(artifact));

        assertEquals("Required source repository for example.tool must declare an immutable commit checkoutRef: https://github.com/SYSTRAN/faster-whisper", exception.getMessage());
        assertFalse(captured.get() != null);
    }

    @Test
    void leavesLegacyArtifactsWithoutStaticManifestAlone() throws Exception {
        Path jar = tempDirectory.resolve("legacy.jar");
        Files.writeString(jar, "placeholder");
        AtomicReference<GitSourceProvisioningRequest> captured = new AtomicReference<>();
        StaticManifestToolProvisioningGate gate = new StaticManifestToolProvisioningGate(
                tempDirectory.resolve("repository"),
                new ToolProvisioningService(List.of(readyGitProvisioner(captured)))
        );

        assertDoesNotThrow(() -> gate.requireReady(artifact(jar)));
        assertEquals(null, captured.get());
    }

    @Test
    void provisionsSupportedPythonSourceAndExposesItsInterpreterToTheToolContext() throws Exception {
        Path source = Files.createDirectories(tempDirectory.resolve("vendor-source").resolve("faster_whisper"));
        Files.writeString(source.getParent().resolve("setup.py"), "from setuptools import setup");
        Files.writeString(source.resolve("__init__.py"), "");
        AtomicReference<PythonVenvProvisioningRequest> capturedPython = new AtomicReference<>();
        Path interpreter = tempDirectory.resolve("runtime").resolve("python.exe");
        StaticManifestToolProvisioningGate gate = new StaticManifestToolProvisioningGate(
                tempDirectory.resolve("repository"),
                new ToolProvisioningService(List.of(
                        readyGitProvisioner(new AtomicReference<>(), source.getParent()),
                        readyPythonProvisioner(capturedPython, interpreter)
                ))
        );
        ResolvedToolArtifact artifact = artifactWithManifest(
                ToolRequirement.sourceRepository("https://github.com/SYSTRAN/faster-whisper")
                        .cloneUrl("https://github.com/SYSTRAN/faster-whisper.git")
                        .checkoutRef("0123456789abcdef0123456789abcdef01234567"),
                List.of(new dev.mrk.meshingress.toolmetadata.ToolProperty(
                        "meshingress.faster-whisper.python-executable", "", "", "string", true, false))
        );

        Map<String, String> properties = gate.requireReady(artifact);

        assertThat(capturedPython.get().sourceRoot()).isEqualTo(source.getParent().toAbsolutePath().normalize());
        assertThat(capturedPython.get().installProject()).isTrue();
        assertThat(capturedPython.get().requiredImports()).containsExactly("faster_whisper");
        assertThat(properties).containsEntry("meshingress.faster-whisper.python-executable", interpreter.toString());
    }

    private ResolvedToolArtifact artifactWithManifest(ToolRequirement requirement) throws Exception {
        return artifactWithManifest(requirement, List.of());
    }

    private ResolvedToolArtifact artifactWithManifest(
            ToolRequirement requirement,
            List<dev.mrk.meshingress.toolmetadata.ToolProperty> properties
    ) throws Exception {
        Path jar = tempDirectory.resolve("cached").resolve("tool.jar");
        Files.createDirectories(jar.getParent());
        Files.writeString(jar, "placeholder");
        McpToolManifestJson.write(new McpToolNativeMetadata(
                McpToolManifestJson.SCHEMA_VERSION,
                "example.tool",
                properties,
                List.of(requirement),
                List.of(),
                ToolReadme.none()
        ), jar.getParent().resolve(McpToolManifestJson.RESOURCES_MANIFEST_PATH));
        return artifact(jar);
    }

    private static ResolvedToolArtifact artifact(Path jar) {
        return new ResolvedToolArtifact(new ToolModuleId("example.tool"), jar, List.of(jar), null, null, null);
    }

    private static ToolProvisioner<GitSourceProvisioningRequest> readyGitProvisioner(
            AtomicReference<GitSourceProvisioningRequest> captured
    ) {
        return readyGitProvisioner(captured, null);
    }

    private static ToolProvisioner<GitSourceProvisioningRequest> readyGitProvisioner(
            AtomicReference<GitSourceProvisioningRequest> captured,
            Path sourceRoot
    ) {
        return new ToolProvisioner<>() {
            @Override
            public Class<GitSourceProvisioningRequest> requestType() {
                return GitSourceProvisioningRequest.class;
            }

            @Override
            public ProvisioningResult provision(GitSourceProvisioningRequest request) {
                captured.set(request);
                ProvisionedResource resource = sourceRoot == null ? null : new ProvisionedResource(
                        sourceRoot, sourceRoot, null, request.canonicalIdentity(), Map.of());
                return new ProvisioningResult(ProvisioningStatus.READY, resource, List.of(), List.of(), "ready");
            }
        };
    }

    private static ToolProvisioner<PythonVenvProvisioningRequest> readyPythonProvisioner(
            AtomicReference<PythonVenvProvisioningRequest> captured,
            Path interpreter
    ) {
        return new ToolProvisioner<>() {
            @Override
            public Class<PythonVenvProvisioningRequest> requestType() {
                return PythonVenvProvisioningRequest.class;
            }

            @Override
            public ProvisioningResult provision(PythonVenvProvisioningRequest request) {
                captured.set(request);
                return new ProvisioningResult(ProvisioningStatus.READY,
                        new ProvisionedResource(request.sourceRoot(), request.virtualEnvironmentRoot(), interpreter,
                                request.requirementId(), Map.of()), List.of(), List.of(), "ready");
            }
        };
    }
}
