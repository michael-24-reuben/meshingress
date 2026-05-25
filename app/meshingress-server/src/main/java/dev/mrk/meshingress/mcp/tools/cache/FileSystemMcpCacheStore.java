package dev.mrk.meshingress.mcp.tools.cache;

import dev.mrk.meshingress.config.MeshingressProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Optional;

@Component
public class FileSystemMcpCacheStore implements McpCacheStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemMcpCacheStore.class);
    private static final String SCHEMA_VERSION = "mcp-cache-v1";

    private final ObjectMapper objectMapper;
    private final MeshingressProperties properties;
    private final Path rootDirectory;

    public FileSystemMcpCacheStore(ObjectMapper objectMapper, MeshingressProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.rootDirectory = Path.of(properties.cache().directory());
        if (properties.cache().createDirectories()) {
            createDirectories(rootDirectory);
        }
        if (properties.cache().cleanupOnStartup()) {
            cleanupExpired();
        }
    }

    @Override
    public Optional<McpCachedValue> get(McpCacheKey key) {
        Path path = pathFor(key);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try {
            JsonNode node = objectMapper.readTree(Files.readString(path, StandardCharsets.UTF_8));
            if (!node.isObject() || !SCHEMA_VERSION.equals(node.path("schemaVersion").asString(""))) {
                return Optional.empty();
            }
            JsonNode result = node.path("result");
            if (!result.isObject()) {
                return Optional.empty();
            }
            Instant createdAt = Instant.parse(node.path("createdAt").asString());
            Instant expiresAt = Instant.parse(node.path("expiresAt").asString());
            return Optional.of(new McpCachedValue(createdAt, expiresAt, ((ObjectNode) result).deepCopy()));
        } catch (Exception exception) {
            LOGGER.warn("Ignoring corrupt MCP cache entry: path={}", path);
            LOGGER.debug("Corrupt MCP cache entry details: path={}", path, exception);
            delete(key);
            return Optional.empty();
        }
    }

    @Override
    public void put(McpCacheKey key, McpCachedValue value) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("schemaVersion", SCHEMA_VERSION);
        node.put("createdAt", value.createdAt().toString());
        node.put("expiresAt", value.expiresAt().toString());
        node.put("namespace", key.namespace());
        node.put("key", key.hash());
        node.set("result", value.result());

        Path path = pathFor(key);
        createDirectories(path.getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.writeString(temporary, node.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailure) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.warn("Unable to write MCP cache entry: path={}", path, exception);
        }
    }

    @Override
    public void delete(McpCacheKey key) {
        try {
            Files.deleteIfExists(pathFor(key));
        } catch (IOException exception) {
            LOGGER.debug("Unable to delete MCP cache entry: key={}", key, exception);
        }
    }

    @Override
    public void cleanupExpired() {
        if (!Files.isDirectory(rootDirectory)) {
            return;
        }
        Instant now = Instant.now();
        try (var paths = Files.walk(rootDirectory)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .forEach(path -> deleteIfExpired(path, now));
        } catch (IOException exception) {
            LOGGER.debug("Unable to cleanup MCP cache directory: path={}", rootDirectory, exception);
        }
    }

    private void deleteIfExpired(Path path, Instant now) {
        try {
            JsonNode node = objectMapper.readTree(Files.readString(path, StandardCharsets.UTF_8));
            String expiresAt = node.path("expiresAt").asString("");
            if (!expiresAt.isBlank() && !Instant.parse(expiresAt).isAfter(now)) {
                Files.deleteIfExists(path);
            }
        } catch (Exception exception) {
            LOGGER.debug("Deleting unreadable MCP cache entry during cleanup: path={}", path, exception);
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                LOGGER.trace("Unable to delete unreadable MCP cache entry: path={}", path, ignored);
            }
        }
    }

    private Path pathFor(McpCacheKey key) {
        return rootDirectory
                .resolve("tools")
                .resolve(key.namespace())
                .resolve(key.hashPrefix())
                .resolve(key.hash() + ".json");
    }

    private void createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException exception) {
            LOGGER.warn("Unable to create MCP cache directory: path={}", path, exception);
        }
    }
}
