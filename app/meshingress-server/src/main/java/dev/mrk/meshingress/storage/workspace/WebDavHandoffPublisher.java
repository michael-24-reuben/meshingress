package dev.mrk.meshingress.storage.workspace;

import dev.mrk.meshingress.api.storage.ToolStorageException;
import dev.mrk.meshingress.api.storage.ToolStorageWorkspace;
import dev.mrk.meshingress.config.MeshingressProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * WebDAV handoff using collection and conditional-file create requests; it never probes, lists, reads, moves, or deletes remote files.
 */
public final class WebDavHandoffPublisher implements ExternalHandoffPublisher {
    private final String target;
    private final URI baseUri;
    private final Duration timeout;
    private final HttpClient client;
    private final String authorization;

    public WebDavHandoffPublisher(String target, MeshingressProperties.Storage.Target configuration, Duration timeout) {
        this(target, configuration, timeout, "");
    }

    public WebDavHandoffPublisher(String target, MeshingressProperties.Storage.Target configuration, Duration timeout, String authorization) {
        this.target = target;
        this.timeout = timeout;
        String endpoint = configuration.endpoint();
        String basePath = configuration.basePath();
        if (endpoint.isBlank() || basePath.isBlank()) throw new IllegalArgumentException("WebDAV endpoint and base path are required for the selected external storage target.");
        this.baseUri = URI.create(endpoint.replaceAll("/+$", "") + "/" + basePath.replaceAll("^/+|/+$", "") + "/");
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
        this.authorization = authorization == null ? "" : authorization.trim();
    }

    @Override
    public String target() {
        return target;
    }

    @Override
    public boolean supportsExternalStaging() {
        return false;
    }

    @Override
    public HandoffReceipt publish(ToolStorageWorkspace workspace, List<WorkspaceFileRecord> files, WorkspaceFiles stagingFiles) {
        return publish(workspace, files, stagingFiles, ignored -> { });
    }

    @Override
    public HandoffReceipt publish(ToolStorageWorkspace workspace, List<WorkspaceFileRecord> files, WorkspaceFiles stagingFiles,
                                  HandoffProgress progress) {
        Set<String> collections = new LinkedHashSet<>();
        String workspacePath = encode(workspace.sessionId()) + "/" + encode(workspace.requestId());
        createCollection(collections, encode(workspace.sessionId()));
        createCollection(collections, workspacePath);
        createCollection(collections, workspacePath + "/files");
        for (WorkspaceFileRecord file : files) {
            createParentCollections(collections, workspacePath + "/files", file.relativePath());
            put(workspace, file.relativePath(), file.mimeType(), stagingFiles);
            progress.accepted(file.relativePath());
        }
        put(workspace, "manifest.json", "application/json", stagingFiles);
        progress.accepted("manifest.json");
        return new HandoffReceipt(target, "webdav:create-only");
    }

    private void createParentCollections(Set<String> collections, String filesPath, String relativePath) {
        int lastSlash = relativePath.lastIndexOf('/');
        if (lastSlash < 1) return;

        String collectionPath = filesPath;
        for (String segment : relativePath.substring(0, lastSlash).split("/")) {
            if (!segment.isBlank()) {
                collectionPath += "/" + encode(segment);
                createCollection(collections, collectionPath);
            }
        }
    }

    private void createCollection(Set<String> collections, String collectionPath) {
        if (!collections.add(collectionPath)) return;

        URI uri = baseUri.resolve(collectionPath + "/");
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .method("MKCOL", HttpRequest.BodyPublishers.noBody());
            if (!authorization.isBlank()) request.header("Authorization", authorization);
            int status = client.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();

            if (status == 201 || status == 405) return;
            if (status == 409)
                throw new ToolStorageException("External handoff target '" + target + "' rejected the configured base path (HTTP 409). Verify that the remote folder exists and that base-path is correct.");
            throw new ToolStorageException("External handoff target '" + target + "' could not create generated collection '" + collectionPath + "' (HTTP " + status + ").");
        } catch (ToolStorageException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ToolStorageException("External handoff could not create a generated collection.", exception);
        }
    }

    private void put(ToolStorageWorkspace workspace, String path, String mimeType, WorkspaceFiles stagingFiles) {
        URI uri = baseUri.resolve(encode(workspace.sessionId()) + "/" + encode(workspace.requestId()) + "/files/" + encodePath(path));
        try (InputStream content = stagingFiles.openStaging(workspace.sessionId(), workspace.requestId(), path)) {
            HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .header("If-None-Match", "*")
                    .header("Content-Type", mimeType)
                    .PUT(HttpRequest.BodyPublishers.ofInputStream(() -> content));
            if (!authorization.isBlank()) request.header("Authorization", authorization);
            int status = client.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();

            if (status == 412)
                throw new ToolStorageException("External handoff conflicted with an existing object; no overwrite was attempted.");
            if (status == 404 || status == 409)
                throw new ToolStorageException("External handoff target '" + target + "' rejected the configured base path (HTTP " + status + "). Verify that the remote folder exists and that base-path is correct; Meshingress did not probe foreign storage.");
            if (status < 200 || status >= 300)
                throw new ToolStorageException("External handoff target '" + target + "' was rejected with HTTP " + status + ".");
        } catch (ToolStorageException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ToolStorageException("External handoff failed without reading or modifying foreign storage.", exception);
        }
    }

    private static String encodePath(String path) {
        return String.join("/", path.split("/")).replace(" ", "%20");
    }

    private static String encode(String value) {
        return value.replace(" ", "%20");
    }
}
