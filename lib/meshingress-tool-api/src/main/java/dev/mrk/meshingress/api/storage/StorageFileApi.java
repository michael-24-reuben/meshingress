package dev.mrk.meshingress.api.storage;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Provider-neutral file contract. Every backend exposes the complete method set;
 * {@link StorageCapabilities} decides which methods may be invoked for that backend
 * and object state. In particular, callers must not use observation methods against
 * a write-only foreign target.
 */
public interface StorageFileApi {
    StorageCapabilities capabilities();

    StorageResult<CreateFile> createFile(CreateFileRequest request);

    StorageResult<WriteReceipt> writeFile(WriteHandle handle, InputStream content);

    StorageResult<WriteReceipt> appendFile(WriteHandle handle, InputStream content);

    StorageResult<FileReceipt> completeFile(WriteHandle handle);

    StorageResult<Void> abortFile(WriteHandle handle);

    StorageResult<Boolean> containsFile(StorageFileKey key);

    StorageResult<StorageFileMetadata> getFileMetadata(StorageFileKey key);

    StorageResult<Long> getFileSize(StorageFileKey key);

    StorageResult<InputStream> readFile(StorageFileKey key);

    StorageResult<Void> deleteFile(StorageFileKey key);

    enum Operation {CREATE, WRITE, APPEND, COMPLETE, ABORT, CONTAINS, METADATA, SIZE, READ, DELETE}

    enum ResultCode {SUCCESS, NOT_FOUND, CONFLICT, UNSUPPORTED, DENIED, FAILED}

    record StorageCapabilities(Set<Operation> allowed) {
        public StorageCapabilities {
            allowed = allowed == null ? Set.of() : Set.copyOf(allowed);
        }

        public boolean allows(Operation operation) {
            return allowed.contains(operation);
        }

        public static StorageCapabilities of(Operation... operations) {
            return new StorageCapabilities(Set.of(operations));
        }
    }

    record StorageResult<T>(ResultCode code, T value, String message) {
        public StorageResult {
            code = code == null ? ResultCode.FAILED : code;
            message = message == null ? "" : message;
        }

        public boolean successful() {
            return code == ResultCode.SUCCESS;
        }

        public static <T> StorageResult<T> success(T value) {
            return new StorageResult<>(ResultCode.SUCCESS, value, "");
        }

        public static <T> StorageResult<T> denied(String message) {
            return new StorageResult<>(ResultCode.DENIED, null, message);
        }

        public static <T> StorageResult<T> unsupported(String message) {
            return new StorageResult<>(ResultCode.UNSUPPORTED, null, message);
        }

        public static <T> StorageResult<T> conflict(String message) {
            return new StorageResult<>(ResultCode.CONFLICT, null, message);
        }

        public static <T> StorageResult<T> failed(String message) {
            return new StorageResult<>(ResultCode.FAILED, null, message);
        }
    }

    record StorageFileKey(String namespace, String path) { }

    record CreateFileRequest(StorageFileKey key, String mimeType, long expectedSize) { }

    record CreateFile(WriteHandle handle) { }

    record WriteHandle(String id) { }

    record WriteReceipt(long byteSize, String checksumSha256) { }

    record FileReceipt(StorageFileKey key, long byteSize, String checksumSha256, String providerReceipt) { }

    record StorageFileMetadata(String mimeType, long byteSize, String checksumSha256, OffsetDateTime createdAt) { }
}
