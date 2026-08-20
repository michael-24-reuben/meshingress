package dev.mrk.meshingress.storage.workspace;

import dev.mrk.meshingress.api.storage.ToolStorageException;
import dev.mrk.meshingress.config.MeshingressProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/** Minimal OCS client for the Meshingress-owned Nextcloud reservation API. */
public final class NextcloudDelegatedWorkspaceClient {
    private static final String API = "/ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces";
    private final URI baseUri;
    private final String storageBasePath;
    private final Duration timeout;
    private final String authorization;
    private final HttpClient http;
    private final ObjectMapper json;

    public NextcloudDelegatedWorkspaceClient(MeshingressProperties.Storage.Target target, Duration timeout, String authorization, ObjectMapper json) {
        this.baseUri = URI.create(target.endpoint().replaceAll("/+$", "") + "/");
        this.storageBasePath = target.basePath().replaceAll("^/+|/+$", "");
        this.timeout = timeout;
        this.authorization = authorization == null ? "" : authorization.trim();
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
        this.json = json;
    }

    JsonNode reserve(String toolId, String sessionId, String requestId) {
        return request("POST", API, toolId, Map.of("sessionId", sessionId == null ? "" : sessionId, "requestId", requestId));
    }

    JsonNode upload(String workspaceId, String toolId, String relativePath, InputStream content, String mimeType) {
        try {
            return request("PUT", API + "/" + segment(workspaceId) + "/files", toolId, Map.of(
                    "path", relativePath,
                    "contentBase64", Base64.getEncoder().encodeToString(content.readAllBytes()),
                    "contentType", mimeType));
        } catch (Exception exception) {
            throw new ToolStorageException("Native workspace file could not be read for Nextcloud upload.", exception);
        }
    }

    JsonNode appendSources(String workspaceId, String toolId, List<Map<String, String>> sources) {
        return request("POST", API + "/" + segment(workspaceId) + "/sources", toolId, Map.of("sources", sources));
    }

    JsonNode seal(String workspaceId, String toolId) {
        return request("POST", API + "/" + segment(workspaceId) + "/seal", toolId, Map.of());
    }

    JsonNode status(String requestId) {
        return request("GET", API + "/by-request/" + segment(requestId), "", null);
    }

    RemoteFile download(String workspaceId, String toolId, String relativePath) {
        String path = filePath(relativePath);
        try {
            URI uri = baseUri.resolve(storagePath(toolId, workspaceId, path));
            HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(timeout).header("Accept", "*/*").GET();
            if (!authorization.isBlank()) request.header("Authorization", authorization);
            HttpResponse<InputStream> response = http.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                response.body().close();
                throw new ToolStorageException("The delegated viewer file is unavailable.");
            }
            String mimeType = response.headers().firstValue("Content-Type").orElse("application/octet-stream").split(";", 2)[0];
            long byteSize = response.headers().firstValueAsLong("Content-Length").orElse(-1);
            return new RemoteFile(response.body(), mimeType, byteSize);
        } catch (ToolStorageException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ToolStorageException("The delegated viewer file could not be read.", exception);
        }
    }

    private JsonNode request(String method, String path, String toolId, Object body) {
        try {
            HttpRequest.BodyPublisher publisher;
            if (body == null) publisher = HttpRequest.BodyPublishers.noBody();
            else publisher = HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body), StandardCharsets.UTF_8);
            HttpRequest.Builder request = HttpRequest.newBuilder(baseUri.resolve(path.replaceFirst("^/", "")))
                    .timeout(timeout)
                    .header("Accept", "application/json")
                    .header("OCS-APIRequest", "true")
                    .method(method, publisher);
            if (!toolId.isBlank()) request.header("X-Meshingress-Tool-Id", toolId);
            if (!authorization.isBlank()) request.header("Authorization", authorization);
            if (body != null) request.header("Content-Type", "application/json");
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode envelope = response.body().isBlank() ? json.createObjectNode() : json.readTree(response.body());
            JsonNode data = envelope.path("ocs").has("data") ? envelope.path("ocs").path("data") : envelope;
            int ocsCode = envelope.path("ocs").path("meta").path("statuscode").asInt(response.statusCode());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || ocsCode >= 400) {
                String message = data.path("message").asString(envelope.path("ocs").path("meta").path("message").asString("Nextcloud request failed."));
                throw new ToolStorageException("Nextcloud delegated workspace request was rejected: " + message);
            }
            return data;
        } catch (ToolStorageException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ToolStorageException("Nextcloud delegated workspace request could not be completed.", exception);
        }
    }

    private static String segment(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }

    private String storagePath(String toolId, String workspaceId, String relativePath) {
        if (storageBasePath.isBlank()) throw new ToolStorageException("The Nextcloud storage base path is not configured.");
        return storageBasePath + "/" + segment(toolId) + "/" + segment(workspaceId) + "/" + relativePath;
    }

    private static String filePath(String value) {
        if (value == null || value.isBlank()) throw new ToolStorageException("A delegated viewer file path is required.");
        String normalized = value.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.endsWith("/")) throw new ToolStorageException("The delegated viewer file path is invalid.");
        String[] segments = normalized.split("/", -1);
        for (String segment : segments) if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) throw new ToolStorageException("The delegated viewer file path is invalid.");
        return String.join("/", java.util.Arrays.stream(segments).map(NextcloudDelegatedWorkspaceClient::segment).toList());
    }

    record RemoteFile(InputStream input, String mimeType, long byteSize) { }
}
