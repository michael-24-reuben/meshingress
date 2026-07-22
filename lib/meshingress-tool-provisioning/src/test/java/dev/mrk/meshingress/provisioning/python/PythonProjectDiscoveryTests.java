package dev.mrk.meshingress.provisioning.python;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PythonProjectDiscoveryTests {

    @TempDir
    Path tempDirectory;

    @Test
    void recognizesASetuptoolsProjectAndItsImportSmokeTarget() throws Exception {
        Files.writeString(tempDirectory.resolve("setup.py"), "from setuptools import setup");
        Path packageDirectory = Files.createDirectories(tempDirectory.resolve("sample_package"));
        Files.writeString(packageDirectory.resolve("__init__.py"), "");
        Files.writeString(tempDirectory.resolve("requirements.txt"), "example-dependency\n");
        Files.writeString(tempDirectory.resolve("uv.lock"), "version = 1\n");

        PythonProjectDescriptor descriptor = PythonProjectDiscovery.discover(tempDirectory).orElseThrow();

        assertThat(descriptor.installProject()).isTrue();
        assertThat(descriptor.requirementsFile()).isNull();
        assertThat(descriptor.lockFile().getFileName().toString()).isEqualTo("uv.lock");
        assertThat(descriptor.requiredImports()).containsExactly("sample_package");
    }

    @Test
    void recognizesRequirementsOnlyScriptProjectsWithoutAttemptingAProjectInstall() throws Exception {
        Files.writeString(tempDirectory.resolve("requirements.txt"), "requests==2.0\n");

        PythonProjectDescriptor descriptor = PythonProjectDiscovery.discover(tempDirectory).orElseThrow();

        assertThat(descriptor.installProject()).isFalse();
        assertThat(descriptor.requirementsFile().getFileName().toString()).isEqualTo("requirements.txt");
        assertThat(descriptor.requiredImports()).isEmpty();
    }
}
