package dev.mrk.meshingress.server.install;

import dev.mrk.meshingress.artifact.model.ArtifactPublicationRecord;
import dev.mrk.meshingress.artifact.model.ArtifactCoordinate;
import dev.mrk.meshingress.config.MeshingressProperties;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcErrorCodes;
import dev.mrk.meshingress.mcp.jsonrpc.JsonRpcException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Comparator;

@Service
public class RepositoryArtifactFetcher {

    private final MeshingressProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public RepositoryArtifactFetcher(MeshingressProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newHttpClient());
    }

    RepositoryArtifactFetcher(MeshingressProperties properties) {
        this(properties, new ObjectMapper(), HttpClient.newHttpClient());
    }

    RepositoryArtifactFetcher(MeshingressProperties properties, HttpClient httpClient) {
        this(properties, new ObjectMapper(), httpClient);
    }

    RepositoryArtifactFetcher(MeshingressProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.httpClient = httpClient;
    }

    public void fetch(ArtifactPublicationRecord publication, Path target) {
        String apiBaseUrl = properties.repository().apiBaseUrl();
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            copyLocalRepositoryArtifact(publication, target);
            return;
        }
        downloadRepositoryArtifact(publication, target, apiBaseUrl);
    }

    public void fetchResources(ArtifactPublicationRecord publication, Path targetDirectory) {
        String apiBaseUrl = properties.repository().apiBaseUrl();
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            copyLocalRepositoryResources(publication, targetDirectory);
            return;
        }
        downloadRepositoryResources(publication, targetDirectory, apiBaseUrl);
    }

    public String artifactFileName(ArtifactPublicationRecord publication) {
        URI uri = repositoryArtifactUri(publication);
        String path = uri.getPath() == null ? "" : uri.getPath();
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == path.length() - 1) {
            throw invalidParams("Publication artifactUri is missing an artifact file name.");
        }
        String fileName = path.substring(lastSlash + 1);
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..") || !fileName.endsWith(".jar")) {
            throw invalidParams("Publication artifact file name is invalid.");
        }
        return fileName;
    }

    public ArtifactPublicationRecord fetchPublication(ArtifactCoordinate coordinate) {
        String apiBaseUrl = properties.repository().apiBaseUrl();
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            throw invalidParams("Repository API base URL is required to fetch publication records.");
        }
        URI uri = publicationUri(coordinate, apiBaseUrl);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .header("X-Repository-Role", properties.repository().apiRole())
                .header("X-Repository-Actor", "meshingress-server")
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 403) {
                throw new JsonRpcException(JsonRpcErrorCodes.FORBIDDEN, "repository API denied publication download");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw invalidParams("repository API publication download failed: HTTP " + response.statusCode());
            }
            try (InputStream input = response.body()) {
                return objectMapper.readValue(input, ArtifactPublicationRecord.class);
            }
        } catch (JsonRpcException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "Unable to fetch repository publication record.");
        }
    }

    private void copyLocalRepositoryArtifact(ArtifactPublicationRecord publication, Path target) {
        String fileName = artifactFileName(publication);
        Path root = Path.of(properties.repository().root()).toAbsolutePath().normalize();
        Path artifactsRoot = root.resolve("artifacts").normalize();
        Path artifact = artifactsRoot
                .resolve(publication.coordinate().groupId().replace('.', '/'))
                .resolve(publication.coordinate().artifactId())
                .resolve(publication.coordinate().version())
                .resolve(fileName)
                .normalize();
        if (!artifact.startsWith(artifactsRoot)) {
            throw invalidParams("Publication artifact path escapes repository root.");
        }
        if (!Files.isRegularFile(artifact)) {
            throw invalidParams("Publication artifact does not exist in repository storage.");
        }
        try {
            Files.copy(artifact, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception exception) {
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "Unable to fetch repository artifact.");
        }
    }

    private void copyLocalRepositoryResources(ArtifactPublicationRecord publication, Path targetDirectory) {
        Path root = Path.of(properties.repository().root()).toAbsolutePath().normalize();
        Path artifactsRoot = root.resolve("artifacts").normalize();
        Path resources = artifactsRoot
                .resolve(publication.coordinate().groupId().replace('.', '/'))
                .resolve(publication.coordinate().artifactId())
                .resolve(publication.coordinate().version())
                .resolve("resources")
                .normalize();
        Path targetRoot = targetDirectory.toAbsolutePath().normalize();
        if (!resources.startsWith(artifactsRoot)) {
            throw invalidParams("Publication resource path escapes repository root.");
        }
        if (!Files.isDirectory(resources)) {
            return;
        }
        try {
            if (Files.exists(targetRoot)) {
                try (var stream = Files.walk(targetRoot)) {
                    for (Path item : stream.sorted(Comparator.reverseOrder()).toList()) {
                        Files.deleteIfExists(item);
                    }
                }
            }
            Files.createDirectories(targetRoot);
            try (var stream = Files.walk(resources)) {
                for (Path source : stream.sorted().toList()) {
                    Path relative = resources.relativize(source);
                    Path target = targetRoot.resolve(relative).normalize();
                    if (!target.startsWith(targetRoot)) {
                        throw invalidParams("Publication resource copy escapes runtime cache.");
                    }
                    if (Files.isDirectory(source)) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (JsonRpcException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "Unable to fetch repository artifact resources.");
        }
    }

    private void downloadRepositoryArtifact(ArtifactPublicationRecord publication, Path target, String apiBaseUrl) {
        URI uri = downloadUri(publication, apiBaseUrl);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", "application/octet-stream")
                .header("X-Repository-Role", properties.repository().apiRole())
                .header("X-Repository-Actor", "meshingress-server")
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 403) {
                throw new JsonRpcException(JsonRpcErrorCodes.FORBIDDEN, "repository API denied artifact download");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw invalidParams("repository API artifact download failed: HTTP " + response.statusCode());
            }
            try (InputStream input = response.body();
                 OutputStream output = Files.newOutputStream(target)) {
                input.transferTo(output);
            }
        } catch (JsonRpcException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "Unable to fetch repository artifact.");
        }
    }

    private void downloadRepositoryResources(ArtifactPublicationRecord publication, Path targetDirectory, String apiBaseUrl) {
        try {
            Files.createDirectories(targetDirectory);
            for (String resourceName : List.of("application.yaml", "README.md")) {
                downloadRepositoryResource(publication, targetDirectory.resolve(resourceName), apiBaseUrl, resourceName);
            }
        } catch (JsonRpcException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "Unable to fetch repository artifact resources.");
        }
    }

    private void downloadRepositoryResource(
            ArtifactPublicationRecord publication,
            Path target,
            String apiBaseUrl,
            String resourceName
    ) {
        URI uri = resourceUri(publication, apiBaseUrl, resourceName);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", "text/plain")
                .header("X-Repository-Role", properties.repository().apiRole())
                .header("X-Repository-Actor", "meshingress-server")
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 404) {
                return;
            }
            if (response.statusCode() == 403) {
                throw new JsonRpcException(JsonRpcErrorCodes.FORBIDDEN, "repository API denied artifact resource download");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw invalidParams("repository API artifact resource download failed: HTTP " + response.statusCode());
            }
            Path targetRoot = target.getParent().toAbsolutePath().normalize();
            Path normalizedTarget = target.toAbsolutePath().normalize();
            if (!normalizedTarget.startsWith(targetRoot)) {
                throw invalidParams("Publication resource copy escapes runtime cache.");
            }
            try (InputStream input = response.body();
                 OutputStream output = Files.newOutputStream(normalizedTarget)) {
                input.transferTo(output);
            }
        } catch (JsonRpcException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new JsonRpcException(JsonRpcErrorCodes.INTERNAL_ERROR, "Unable to fetch repository artifact resource.");
        }
    }

    private URI downloadUri(ArtifactPublicationRecord publication, String apiBaseUrl) {
        URI base = URI.create(apiBaseUrl.endsWith("/") ? apiBaseUrl : apiBaseUrl + "/");
        String path = "artifact/%s/%s/%s/file".formatted(
                segment(publication.coordinate().groupId()),
                segment(publication.coordinate().artifactId()),
                segment(publication.coordinate().version())
        );
        return base.resolve(path);
    }

    private URI resourceUri(ArtifactPublicationRecord publication, String apiBaseUrl, String resourceName) {
        URI base = URI.create(apiBaseUrl.endsWith("/") ? apiBaseUrl : apiBaseUrl + "/");
        String path = "artifact/%s/%s/%s/resources/%s".formatted(
                segment(publication.coordinate().groupId()),
                segment(publication.coordinate().artifactId()),
                segment(publication.coordinate().version()),
                segment(resourceName)
        );
        return base.resolve(path);
    }

    private URI publicationUri(ArtifactCoordinate coordinate, String apiBaseUrl) {
        if (coordinate == null) {
            throw invalidParams("Publication coordinate is required.");
        }
        URI base = URI.create(apiBaseUrl.endsWith("/") ? apiBaseUrl : apiBaseUrl + "/");
        String path = "artifact/%s/%s/%s/publication".formatted(
                segment(coordinate.groupId()),
                segment(coordinate.artifactId()),
                segment(coordinate.version())
        );
        return base.resolve(path);
    }

    private URI repositoryArtifactUri(ArtifactPublicationRecord publication) {
        URI uri = URI.create(publication.artifactUri());
        if (!"meshingress-repository".equals(uri.getScheme()) || !"artifact".equals(uri.getAuthority())) {
            throw invalidParams("Publication artifactUri must use meshingress-repository://artifact/.");
        }
        return uri;
    }

    private String segment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private JsonRpcException invalidParams(String message) {
        return new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS, message);
    }
}
