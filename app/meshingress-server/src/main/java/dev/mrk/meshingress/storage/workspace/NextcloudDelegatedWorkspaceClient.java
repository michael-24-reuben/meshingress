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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal OCS client for the Meshingress-owned Nextcloud reservation API. */
public final class NextcloudDelegatedWorkspaceClient {
    private static final String API = "/ocs/v2.php/apps/meshingress/api/v1/delegated-workspaces";
    private final URI baseUri;
    private final Duration timeout;
    private final String authorization;
    private final HttpClient http;
    private final ObjectMapper json;

    public NextcloudDelegatedWorkspaceClient(MeshingressProperties.Storage.Target target, Duration timeout, String authorization, ObjectMapper json) {
        this.baseUri = URI.create(target.endpoint().replaceAll("/+$", "") + "/");
        this.timeout = timeout;
        this.authorization = authorization == null ? "" : authorization.trim();
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
        this.json = json;
    }

    JsonNode reserve(String toolId, String sessionId, String requestId) {
        return request("POST", API, toolId, Map.of("sessionId", sessionId == null ? "" : sessionId, "requestId", requestId), null, null);
    }

    JsonNode upload(String workspaceId, String toolId, String relativePath, InputStream content, String mimeType) {
        return request("PUT", API + "/" + segment(workspaceId) + "/files?path=" + query(relativePath), toolId, null, content, mimeType);
    }

    JsonNode appendSources(String workspaceId, String toolId, List<Map<String, String>> sources) {
        return request("POST", API + "/" + segment(workspaceId) + "/sources", toolId, Map.of("sources", sources), null, null);
    }

    JsonNode seal(String workspaceId, String toolId) {
        return request("POST", API + "/" + segment(workspaceId) + "/seal", toolId, Map.of(), null, null);
    }

    JsonNode status(String requestId) {
        return request("GET", API + "/by-request/" + segment(requestId), "", null, null, null);
    }

    private JsonNode request(String method, String path, String toolId, Object body, InputStream stream, String mimeType) {
        try {
            HttpRequest.BodyPublisher publisher;
            if (stream != null) publisher = HttpRequest.BodyPublishers.ofInputStream(() -> stream);
            else if (body == null) publisher = HttpRequest.BodyPublishers.noBody();
            else publisher = HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body), StandardCharsets.UTF_8);
            HttpRequest.Builder request = HttpRequest.newBuilder(baseUri.resolve(path.replaceFirst("^/", "")))
                    .timeout(timeout)
                    .header("Accept", "application/json")
                    .header("OCS-APIRequest", "true")
                    .method(method, publisher);
            if (!toolId.isBlank()) request.header("X-Meshingress-Tool-Id", toolId);
            if (!authorization.isBlank()) request.header("Authorization", authorization);
            if (stream != null) request.header("Content-Type", mimeType);
            else if (body != null) request.header("Content-Type", "application/json");
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode envelope = response.body().isBlank() ? json.createObjectNode() : json.readTree(response.body());
            JsonNode data = envelope.path("ocs").has("data") ? envelope.path("ocs").path("data") : envelope;
            int ocsCode = envelope.path("ocs").path("meta").path("statuscode").asInt(response.statusCode());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || ocsCode >= 400) {
                String message = data.path("message").asText(envelope.path("ocs").path("meta").path("message").asText("Nextcloud request failed."));
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
    private static String query(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
}
