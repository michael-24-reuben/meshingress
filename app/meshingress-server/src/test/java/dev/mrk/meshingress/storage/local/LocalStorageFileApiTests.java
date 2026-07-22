package dev.mrk.meshingress.storage.local;

import dev.mrk.meshingress.api.storage.StorageFileApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalStorageFileApiTests {
    @TempDir Path temp;

    @Test void createIsAtomicAndTheFullLocalApiCanObserveAndDeleteCompletedFiles() throws Exception {
        LocalStorageFileApi api = new LocalStorageFileApi(temp);
        StorageFileApi.StorageFileKey key = new StorageFileApi.StorageFileKey("workspace", "output.txt");
        StorageFileApi.CreateFile created = api.createFile(new StorageFileApi.CreateFileRequest(key, "text/plain", -1)).value();
        assertEquals(StorageFileApi.ResultCode.CONFLICT, api.createFile(new StorageFileApi.CreateFileRequest(key, "text/plain", -1)).code());
        assertTrue(api.writeFile(created.handle(), new ByteArrayInputStream("one".getBytes(StandardCharsets.UTF_8))).successful());
        assertTrue(api.appendFile(created.handle(), new ByteArrayInputStream(" two".getBytes(StandardCharsets.UTF_8))).successful());
        assertEquals(7, api.completeFile(created.handle()).value().byteSize());
        assertEquals(true, api.containsFile(key).value());
        assertEquals("one two", new String(api.readFile(key).value().readAllBytes(), StandardCharsets.UTF_8));
        assertTrue(api.deleteFile(key).successful());
    }
}
