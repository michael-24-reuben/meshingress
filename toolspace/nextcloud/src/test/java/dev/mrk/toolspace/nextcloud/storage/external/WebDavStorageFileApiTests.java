package dev.mrk.toolspace.nextcloud.storage.external;

import dev.mrk.meshingress.api.storage.StorageFileApi;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class WebDavStorageFileApiTests {
    @Test void writeOnlyTargetExposesCreateButDeniesForeignObservationAndMutation() {
        WebDavStorageFileApi.WebDavTarget target = new WebDavStorageFileApi.WebDavTarget("https://dav.example", "/handoff");
        WebDavStorageFileApi api = new WebDavStorageFileApi(target, Duration.ofSeconds(1), "");
        assertTrue(api.capabilities().allows(StorageFileApi.Operation.CREATE));
        assertFalse(api.capabilities().allows(StorageFileApi.Operation.CONTAINS));
        StorageFileApi.StorageFileKey key = new StorageFileApi.StorageFileKey("workspace", "output.txt");
        assertEquals(StorageFileApi.ResultCode.DENIED, api.containsFile(key).code());
        assertEquals(StorageFileApi.ResultCode.DENIED, api.deleteFile(key).code());
    }
}
