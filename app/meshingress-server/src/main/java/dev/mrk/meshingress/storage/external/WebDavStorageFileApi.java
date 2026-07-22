package dev.mrk.meshingress.storage.external;

import dev.mrk.meshingress.api.storage.StorageFileApi;
import dev.mrk.meshingress.config.MeshingressProperties;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Direct-final WebDAV file API. Foreign observation and mutation methods are intentionally denied.
 */
public final class WebDavStorageFileApi implements StorageFileApi {
    private final URI baseUri;
    private final Duration timeout;
    private final String authorization;
    private final HttpClient client = HttpClient.newHttpClient();
    private final Map<String, CreateFileRequest> pending = new ConcurrentHashMap<>();
    private final Map<String, FileReceipt> completed = new ConcurrentHashMap<>();

    public WebDavStorageFileApi(MeshingressProperties.Storage.Target target, Duration timeout, String authorization) {
        if (target.provider() != MeshingressProperties.Storage.Provider.WEBDAV || target.endpoint().isBlank() || target.basePath().isBlank())
            throw new IllegalArgumentException("A WebDAV endpoint and base path are required.");

        this.baseUri = URI.create(target.endpoint().replaceAll("/+$", "") + "/" + target.basePath().replaceAll("^/+|/+$", "") + "/");
        this.timeout = timeout;
        this.authorization = authorization == null ? "" : authorization.trim();
    }

    @Override
    public StorageCapabilities capabilities() {
        return new StorageCapabilities(EnumSet.of(Operation.CREATE, Operation.WRITE, Operation.COMPLETE, Operation.ABORT));
    }

    @Override
    public StorageResult<CreateFile> createFile(CreateFileRequest request) {
        if (request == null || request.key() == null) return StorageResult.failed("A storage file key is required.");
        String id = UUID.randomUUID().toString();
        pending.put(id, request);
        return StorageResult.success(new CreateFile(new WriteHandle(id)));
    }

    @Override
    public StorageResult<WriteReceipt> writeFile(WriteHandle handle, InputStream content) {
        CreateFileRequest request = pending.get(handle == null ? "" : handle.id());
        if (request == null || content == null) return StorageResult.denied("The write handle is not active.");
        try (InputStream input = content) {
            HttpRequest.Builder put = HttpRequest.newBuilder(uri(request.key())).timeout(timeout).header("If-None-Match", "*")
                    .header("Content-Type", request.mimeType() == null ? "application/octet-stream" : request.mimeType())
                    .PUT(HttpRequest.BodyPublishers.ofInputStream(() -> input));
            if (!authorization.isBlank()) put.header("Authorization", authorization);
            int status = client.send(put.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status == 409 || status == 412) return StorageResult.conflict("The foreign object already exists; no overwrite was attempted.");
            if (status < 200 || status >= 300) return StorageResult.failed("The WebDAV create was rejected with HTTP " + status + ".");
            FileReceipt receipt = new FileReceipt(request.key(), request.expectedSize(), "", "webdav:create-only");
            completed.put(handle.id(), receipt);
            return StorageResult.success(new WriteReceipt(request.expectedSize(), ""));
        } catch (Exception exception) {
            return StorageResult.failed("The WebDAV create failed without observing foreign storage.");
        }
    }

    @Override
    public StorageResult<WriteReceipt> appendFile(WriteHandle handle, InputStream content) {
        return StorageResult.unsupported("Direct-final WebDAV does not append foreign objects.");
    }

    @Override
    public StorageResult<FileReceipt> completeFile(WriteHandle handle) {
        FileReceipt receipt = completed.remove(handle == null ? "" : handle.id());
        pending.remove(handle == null ? "" : handle.id());
        return receipt == null ? StorageResult.denied("The write handle has not completed a create.") : StorageResult.success(receipt);
    }

    @Override
    public StorageResult<Void> abortFile(WriteHandle handle) {
        pending.remove(handle == null ? "" : handle.id());
        completed.remove(handle == null ? "" : handle.id());
        return StorageResult.success(null);
    }

    @Override
    public StorageResult<Boolean> containsFile(StorageFileKey key) {
        return StorageResult.denied("Write-only foreign storage does not permit existence checks.");
    }

    @Override
    public StorageResult<StorageFileMetadata> getFileMetadata(StorageFileKey key) {
        return StorageResult.denied("Write-only foreign storage does not permit metadata reads.");
    }

    @Override
    public StorageResult<Long> getFileSize(StorageFileKey key) {
        return StorageResult.denied("Write-only foreign storage does not permit size reads.");
    }

    @Override
    public StorageResult<InputStream> readFile(StorageFileKey key) {
        return StorageResult.denied("Write-only foreign storage does not permit reads.");
    }

    @Override
    public StorageResult<Void> deleteFile(StorageFileKey key) {
        return StorageResult.denied("Write-only foreign storage does not permit deletion.");
    }

    private URI uri(StorageFileKey key) {
        if (key.namespace() == null || key.path() == null || key.path().startsWith("/") || key.path().contains("..")) throw new IllegalArgumentException("The foreign storage key is invalid.");
        return baseUri.resolve(key.namespace().replace(" ", "%20") + "/" + key.path().replace(" ", "%20"));
    }
}
