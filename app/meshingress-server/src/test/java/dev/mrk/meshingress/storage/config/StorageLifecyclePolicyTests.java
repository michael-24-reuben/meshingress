package dev.mrk.meshingress.storage.config;

import dev.mrk.meshingress.config.MeshingressProperties;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StorageLifecyclePolicyTests {
    @Test void localLocalNeedsNoExternalTarget() { assertDoesNotThrow(() -> new StorageLifecyclePolicy(storage(MeshingressProperties.Storage.Lifecycle.LOCAL_LOCAL, Map.of()))); }

    @Test void localAsyncExternalAcceptsDirectFinalWebDav() {
        assertDoesNotThrow(() -> new StorageLifecyclePolicy(storage(MeshingressProperties.Storage.Lifecycle.LOCAL_ASYNC_EXTERNAL, Map.of("dav", webDav()))));
    }

    @Test void externalExternalRejectsDirectFinalWebDav() {
        assertThrows(IllegalStateException.class, () -> new StorageLifecyclePolicy(storage(MeshingressProperties.Storage.Lifecycle.EXTERNAL_EXTERNAL, Map.of("dav", webDav()))));
    }

    @Test void delegatedExternalAcceptsNextcloudOcsTarget() {
        assertDoesNotThrow(() -> new StorageLifecyclePolicy(storage(MeshingressProperties.Storage.Lifecycle.DELEGATED_EXTERNAL,
                Map.of("dav", new MeshingressProperties.Storage.Target(MeshingressProperties.Storage.Provider.NEXTCLOUD, true, "https://cloud.example.test", "", "", "", "", MeshingressProperties.Storage.StagingMode.PROVIDER_SESSION)))));
    }

    private MeshingressProperties.Storage storage(MeshingressProperties.Storage.Lifecycle lifecycle, Map<String, MeshingressProperties.Storage.Target> targets) {
        return new MeshingressProperties.Storage(true, lifecycle, DataSize.ofMegabytes(1),
                new MeshingressProperties.Storage.Local("target/storage", 1, new MeshingressProperties.Storage.Staging(DataSize.ofMegabytes(1), Duration.ofMinutes(1)),
                        new MeshingressProperties.Storage.Published(DataSize.ofMegabytes(1), Duration.ofMinutes(1), Duration.ofHours(1), 1, 1, 1), new MeshingressProperties.Storage.Cleanup(Duration.ofMinutes(1), 1)),
                new MeshingressProperties.Storage.External(targets.isEmpty() ? "" : "dav", MeshingressProperties.Storage.AccessMode.WRITE_ONLY, MeshingressProperties.Storage.MutationPolicy.CREATE_ONLY,
                        MeshingressProperties.Storage.ConflictPolicy.FAIL, MeshingressProperties.Storage.RetentionPolicy.PROVIDER_MANAGED, 1, Duration.ofMinutes(1),
                        new MeshingressProperties.Storage.AsyncHandoff(Duration.ofSeconds(1), Duration.ofSeconds(2), 3, Duration.ofMillis(10), Duration.ofSeconds(1)), targets),
                new MeshingressProperties.Storage.Metadata(new MeshingressProperties.Storage.Sql("storage", "storage_", new MeshingressProperties.Storage.Tables("entries", "usage", "events"), true)));
    }

    private MeshingressProperties.Storage.Target webDav() {
        return new MeshingressProperties.Storage.Target(MeshingressProperties.Storage.Provider.WEBDAV, true, "https://example.test", "/handoff", "", "", "", MeshingressProperties.Storage.StagingMode.DIRECT_FINAL_ONLY);
    }
}
