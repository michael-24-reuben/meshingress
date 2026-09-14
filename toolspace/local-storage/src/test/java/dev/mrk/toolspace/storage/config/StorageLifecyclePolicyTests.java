package dev.mrk.toolspace.storage.config;

import dev.mrk.toolspace.localstorage.config.LocalStorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StorageLifecyclePolicyTests {
    @Test void localLocalNeedsNoExternalTarget() { assertDoesNotThrow(() -> new StorageLifecyclePolicy(storage(LocalStorageProperties.Lifecycle.LOCAL_LOCAL, Map.of()))); }

    @Test void localExternalAcceptsDirectFinalWebDav() {
        assertDoesNotThrow(() -> new StorageLifecyclePolicy(storage(LocalStorageProperties.Lifecycle.LOCAL_EXTERNAL, Map.of("dav", webDav()))));
    }

    private LocalStorageProperties storage(LocalStorageProperties.Lifecycle lifecycle, Map<String, LocalStorageProperties.Target> targets) {
        return new LocalStorageProperties(true, lifecycle, DataSize.ofMegabytes(1),
                new LocalStorageProperties.Local("target/storage", 1, new LocalStorageProperties.Staging(DataSize.ofMegabytes(1), Duration.ofMinutes(1)),
                        new LocalStorageProperties.Published(DataSize.ofMegabytes(1), Duration.ofMinutes(1), Duration.ofHours(1), 1, 1, 1), new LocalStorageProperties.Cleanup(Duration.ofMinutes(1), 1)),
                new LocalStorageProperties.External(targets.isEmpty() ? "" : "dav", LocalStorageProperties.AccessMode.WRITE_ONLY, LocalStorageProperties.MutationPolicy.CREATE_ONLY,
                        LocalStorageProperties.ConflictPolicy.FAIL, LocalStorageProperties.RetentionPolicy.PROVIDER_MANAGED, 1, Duration.ofMinutes(1),
                        new LocalStorageProperties.AsyncHandoff(Duration.ofSeconds(1), Duration.ofSeconds(2), 3, Duration.ofMillis(10), Duration.ofSeconds(1)), targets),
                new LocalStorageProperties.Metadata(new LocalStorageProperties.Sql("storage", "storage_", new LocalStorageProperties.Tables("entries", "usage", "events"), true)));
    }

    private LocalStorageProperties.Target webDav() {
        return new LocalStorageProperties.Target(LocalStorageProperties.Provider.WEBDAV, true, "https://example.test", "/handoff", "", "", "", LocalStorageProperties.StagingMode.DIRECT_FINAL_ONLY);
    }
}
