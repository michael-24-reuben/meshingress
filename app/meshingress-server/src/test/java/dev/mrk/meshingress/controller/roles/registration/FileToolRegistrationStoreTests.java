package dev.mrk.meshingress.controller.roles.registration;

import dev.mrk.meshingress.config.MeshingressProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FileToolRegistrationStoreTests {

    @TempDir
    Path tempDir;

    @Test
    void registrationRecordsSurviveStoreRestart() {
        ObjectMapper objectMapper = new ObjectMapper();
        MeshingressProperties properties = meshingressProperties();
        ToolRegistrationRecord record = record("reg-1", "active");

        FileToolRegistrationStore firstStore = new FileToolRegistrationStore(objectMapper, properties);
        firstStore.saveActive(record);

        FileToolRegistrationStore restartedStore = new FileToolRegistrationStore(objectMapper, properties);
        ToolRegistrationRecord reloaded = restartedStore.list().getFirst();

        assertThat(restartedStore.list()).hasSize(1);
        assertThat(reloaded.registrationId()).isEqualTo(record.registrationId());
        assertThat(reloaded.toolId()).isEqualTo(record.toolId());
        assertThat(reloaded.sourceKind()).isEqualTo(record.sourceKind());
        assertThat(reloaded.status()).isEqualTo(record.status());
        assertThat(reloaded.source()).isEqualTo(record.source());
        assertThat(reloaded.runtimeModuleId()).isEqualTo(record.runtimeModuleId());
        assertThat(reloaded.registeredFunctions()).isEqualTo(record.registeredFunctions());
        assertThat(reloaded.registeredAt().toInstant()).isEqualTo(record.registeredAt().toInstant());
        assertThat(restartedStore.findActive("sample.echo", ToolRegistrationPhase.STAGING))
                .contains(reloaded);
        assertThat(Files.isRegularFile(registrationStorePath())).isTrue();
    }

    @Test
    void statusChangesSurviveStoreRestart() {
        ObjectMapper objectMapper = new ObjectMapper();
        MeshingressProperties properties = meshingressProperties();
        FileToolRegistrationStore firstStore = new FileToolRegistrationStore(objectMapper, properties);
        firstStore.saveActive(record("reg-2", "active"));

        ToolRegistrationRecord updated = firstStore.markStatus("reg-2", "install-rolled-back");
        FileToolRegistrationStore restartedStore = new FileToolRegistrationStore(objectMapper, properties);
        ToolRegistrationRecord reloaded = restartedStore.list().getFirst();

        assertThat(updated.status()).isEqualTo("install-rolled-back");
        assertThat(restartedStore.list()).hasSize(1);
        assertThat(reloaded.registrationId()).isEqualTo(updated.registrationId());
        assertThat(reloaded.status()).isEqualTo(updated.status());
        assertThat(reloaded.registeredAt().toInstant()).isEqualTo(updated.registeredAt().toInstant());
        assertThat(restartedStore.findActive("sample.echo")).isEmpty();
    }

    private ToolRegistrationRecord record(String registrationId, String status) {
        return new ToolRegistrationRecord(
                registrationId,
                "sample.echo",
                ToolRegistrationPhase.STAGING,
                ToolSourceKind.PUBLICATION_RECORD,
                status,
                Map.of(
                        "coordinate", "dev.mrk.tools:sample-module:0.0.1-SNAPSHOT",
                        "artifactUri", "meshingress-repository://artifact/dev/mrk/tools/sample-module/0.0.1-SNAPSHOT/sample-module.jar",
                        "runtimeCachePath", tempDir.resolve("runtime-cache").resolve("sample-module.jar").toString()
                ),
                "test",
                "req-1",
                OffsetDateTime.parse("2026-06-21T09:30:00-04:00"),
                null,
                "dev.mrk.tools:sample-module:0.0.1-SNAPSHOT",
                List.of("sample.echo")
        );
    }

    private MeshingressProperties meshingressProperties() {
        MeshingressProperties defaults = new MeshingressProperties(null, null, null, null, null, null, null, null, null, null);
        MeshingressProperties.Repository defaultsRepository = defaults.repository();
        MeshingressProperties.Repository repository = new MeshingressProperties.Repository(
                tempDir.resolve("repository").toString(),
                tempDir.resolve("runtime-cache").toString(),
                "runtime/tool-registrations.json",
                defaultsRepository.apiBaseUrl(),
                defaultsRepository.apiRole(),
                defaultsRepository.signingKeyId(),
                defaultsRepository.signingSecret(),
                defaultsRepository.verificationKeys()
        );
        return new MeshingressProperties(
                defaults.identity(),
                defaults.mcp(),
                defaults.tools(),
                defaults.dispatch(),
                defaults.cache(),
                defaults.security(),
                defaults.scopes(),
                defaults.audit(),
                defaults.secrets(),
                repository
        );
    }

    private Path registrationStorePath() {
        return tempDir.resolve("repository").resolve("runtime").resolve("tool-registrations.json");
    }
}
