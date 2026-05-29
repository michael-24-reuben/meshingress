package dev.mrk.meshingress.repository.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "meshingress.repository")
public record MeshingressRepositoryProperties(
        @NotNull Path root,
        @NotBlank String signingSecret,
        boolean fakeScannerEnabled
) {
    public MeshingressRepositoryProperties {
        root = root == null ? Path.of("repository") : root;
        signingSecret = signingSecret == null || signingSecret.isBlank() ? "dev-repository-signing-key" : signingSecret;
    }
}
