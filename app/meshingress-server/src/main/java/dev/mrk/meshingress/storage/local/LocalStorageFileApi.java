package dev.mrk.meshingress.storage.local;

import dev.mrk.meshingress.api.storage.StorageFileApi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local implementation with atomic create-new semantics.
 */
public final class LocalStorageFileApi implements StorageFileApi {
    private final Path root;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public LocalStorageFileApi(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public StorageCapabilities capabilities() {
        return new StorageCapabilities(EnumSet.allOf(Operation.class));
    }

    @Override
    public StorageResult<CreateFile> createFile(CreateFileRequest request) {
        try {
            Path path = path(request.key());
            Files.createDirectories(path.getParent());
            Files.newOutputStream(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).close();
            String id = UUID.randomUUID().toString();
            sessions.put(id, new Session(request.key(), path, request.mimeType()));
            return StorageResult.success(new CreateFile(new WriteHandle(id)));
        } catch (java.nio.file.FileAlreadyExistsException exception) {
            return StorageResult.conflict("The file already exists.");
        } catch (Exception exception) {
            return StorageResult.failed("The local file could not be created.");
        }
    }

    @Override
    public StorageResult<WriteReceipt> writeFile(WriteHandle handle, InputStream content) {
        return copy(handle, content, false);
    }

    @Override
    public StorageResult<WriteReceipt> appendFile(WriteHandle handle, InputStream content) {
        return copy(handle, content, true);
    }

    @Override
    public StorageResult<FileReceipt> completeFile(WriteHandle handle) {
        Session session = sessions.remove(handle == null ? "" : handle.id());
        if (session == null) return StorageResult.denied("The write handle is not active.");
        try {
            long size = Files.size(session.path());
            return StorageResult.success(new FileReceipt(session.key(), size, checksum(session.path()), "local"));
        } catch (IOException exception) {
            return StorageResult.failed("The local file could not be completed.");
        }
    }

    @Override
    public StorageResult<Void> abortFile(WriteHandle handle) {
        Session session = sessions.remove(handle == null ? "" : handle.id());
        if (session == null) return StorageResult.denied("The write handle is not active.");
        try {
            Files.deleteIfExists(session.path());
            return StorageResult.success(null);
        } catch (IOException exception) {
            return StorageResult.failed("The local file could not be aborted.");
        }
    }

    @Override
    public StorageResult<Boolean> containsFile(StorageFileKey key) {
        return StorageResult.success(Files.exists(path(key)));
    }

    @Override
    public StorageResult<StorageFileMetadata> getFileMetadata(StorageFileKey key) {
        try {
            Path path = path(key);
            return Files.exists(path) ? StorageResult.success(new StorageFileMetadata("application/octet-stream", Files.size(path), checksum(path), OffsetDateTime.now())) : new StorageResult<>(ResultCode.NOT_FOUND, null, "The file does not exist.");
        } catch (IOException exception) {
            return StorageResult.failed("The local file could not be inspected.");
        }
    }

    @Override
    public StorageResult<Long> getFileSize(StorageFileKey key) {
        try {
            Path path = path(key);
            return Files.exists(path) ? StorageResult.success(Files.size(path)) : new StorageResult<>(ResultCode.NOT_FOUND, null, "The file does not exist.");
        } catch (IOException exception) {
            return StorageResult.failed("The local file could not be inspected.");
        }
    }

    @Override
    public StorageResult<InputStream> readFile(StorageFileKey key) {
        try {
            return StorageResult.success(Files.newInputStream(path(key), StandardOpenOption.READ));
        } catch (java.nio.file.NoSuchFileException exception) {
            return new StorageResult<>(ResultCode.NOT_FOUND, null, "The file does not exist.");
        } catch (IOException exception) {
            return StorageResult.failed("The local file could not be read.");
        }
    }

    @Override
    public StorageResult<Void> deleteFile(StorageFileKey key) {
        try {
            return Files.deleteIfExists(path(key)) ? StorageResult.success(null) : new StorageResult<>(ResultCode.NOT_FOUND, null, "The file does not exist.");
        } catch (IOException exception) {
            return StorageResult.failed("The local file could not be deleted.");
        }
    }

    private StorageResult<WriteReceipt> copy(WriteHandle handle, InputStream input, boolean append) {
        Session session = sessions.get(handle == null ? "" : handle.id());
        if (session == null || input == null) return StorageResult.denied("The write handle is not active.");
        try (InputStream source = input; var output = Files.newOutputStream(session.path(), StandardOpenOption.WRITE, append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING)) {
            source.transferTo(output);
            return StorageResult.success(new WriteReceipt(Files.size(session.path()), checksum(session.path())));
        } catch (IOException exception) {
            return StorageResult.failed("The local file could not be written.");
        }
    }

    private Path path(StorageFileKey key) {
        if (key == null || key.namespace() == null || key.path() == null) throw new IllegalArgumentException("A storage file key is required.");
        Path result = root.resolve(key.namespace()).resolve(key.path()).normalize();
        if (!result.startsWith(root)) throw new IllegalArgumentException("Storage key escapes the local root.");
        return result;
    }

    private static String checksum(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.readAllBytes()));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IOException(exception);
        }
    }

    private record Session(StorageFileKey key, Path path, String mimeType) {
    }
}
