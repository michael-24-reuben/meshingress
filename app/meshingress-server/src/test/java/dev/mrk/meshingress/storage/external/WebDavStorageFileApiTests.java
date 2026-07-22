package dev.mrk.meshingress.storage.external;

import dev.mrk.meshingress.api.storage.StorageFileApi;
import dev.mrk.meshingress.config.MeshingressProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebDavStorageFileApiTests {
    @Test void writeOnlyTargetExposesCreateButDeniesForeignObservationAndMutation() {
        MeshingressProperties.Storage.Target target = new MeshingressProperties.Storage.Target(MeshingressProperties.Storage.Provider.WEBDAV, true, "https://dav.example", "/handoff", "", "", "", MeshingressProperties.Storage.StagingMode.DIRECT_FINAL_ONLY);
        WebDavStorageFileApi api = new WebDavStorageFileApi(target, Duration.ofSeconds(1), "");
        assertTrue(api.capabilities().allows(StorageFileApi.Operation.CREATE));
        assertFalse(api.capabilities().allows(StorageFileApi.Operation.CONTAINS));
        StorageFileApi.StorageFileKey key = new StorageFileApi.StorageFileKey("workspace", "output.txt");
        assertEquals(StorageFileApi.ResultCode.DENIED, api.containsFile(key).code());
        assertEquals(StorageFileApi.ResultCode.DENIED, api.deleteFile(key).code());
    }
}
