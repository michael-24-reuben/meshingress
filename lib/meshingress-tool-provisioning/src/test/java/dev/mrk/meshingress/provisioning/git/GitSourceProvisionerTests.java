package dev.mrk.meshingress.provisioning.git;

import dev.mrk.meshingress.provisioning.ProvisioningResult;
import dev.mrk.meshingress.provisioning.ProvisioningStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class GitSourceProvisionerTests {
    @TempDir
    Path tempDir;

    @Test
    void stagesPinnedLocalRepositoryUnderCanonicalVendorPathAndReusesIt() throws Exception {
        Path fixture = createFixture();
        String commit = git(fixture, "rev-parse", "HEAD");
        GitSourceProvisioner provisioner = new GitSourceProvisioner();
        GitSourceProvisioningRequest request = request(fixture, commit);

        ProvisioningResult first = provisioner.provision(request);
        ProvisioningResult second = provisioner.provision(request);

        Path expected = tempDir.resolve("repository/vendor/example.test/acme/sample");
        assertThat(first.status()).isEqualTo(ProvisioningStatus.READY);
        assertThat(first.resource().sourceRoot()).isEqualTo(expected);
        assertThat(first.resource().attributes().get("resolvedCommit")).isEqualToIgnoringCase(commit);
        assertThat(Files.readString(expected.resolve("README.md")).strip()).isEqualTo("first");
        assertThat(second.status()).isEqualTo(ProvisioningStatus.REUSED);
    }

    @Test
    void replacesPinnedCheckoutWhenTheRequestedCommitChanges() throws Exception {
        Path fixture = createFixture();
        String firstCommit = git(fixture, "rev-parse", "HEAD");
        GitSourceProvisioner provisioner = new GitSourceProvisioner();
        assertThat(provisioner.provision(request(fixture, firstCommit)).status()).isEqualTo(ProvisioningStatus.READY);

        Files.writeString(fixture.resolve("README.md"), "second\n", StandardCharsets.UTF_8);
        git(fixture, "add", "README.md");
        git(fixture, "commit", "-m", "second");
        String secondCommit = git(fixture, "rev-parse", "HEAD");

        ProvisioningResult result = provisioner.provision(request(fixture, secondCommit));

        assertThat(result.status()).isEqualTo(ProvisioningStatus.READY);
        assertThat(result.resource().attributes().get("resolvedCommit")).isEqualToIgnoringCase(secondCommit);
        assertThat(Files.readString(result.resource().sourceRoot().resolve("README.md")).strip()).isEqualTo("second");
    }

    private GitSourceProvisioningRequest request(Path fixture, String commit) {
        return new GitSourceProvisioningRequest(
                "sample-source",
                fixture.toUri().toString(),
                "example.test/acme/sample",
                commit,
                tempDir.resolve("repository"),
                null,
                Duration.ofSeconds(30)
        );
    }

    private Path createFixture() throws Exception {
        Path fixture = tempDir.resolve("fixture");
        Files.createDirectories(fixture);
        git(fixture, "init", "--quiet");
        git(fixture, "config", "user.email", "test@example.test");
        git(fixture, "config", "user.name", "Meshingress Test");
        Files.writeString(fixture.resolve("README.md"), "first\n", StandardCharsets.UTF_8);
        git(fixture, "add", "README.md");
        git(fixture, "commit", "--quiet", "-m", "first");
        return fixture;
    }

    private static String git(Path workingDirectory, String... arguments) throws IOException, InterruptedException {
        List<String> command = new java.util.ArrayList<>(List.of("git"));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true)
                .start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("git command timed out");
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (process.exitValue() != 0) {
            throw new IllegalStateException("git command failed: " + output);
        }
        return output;
    }
}
