package dev.mrk.meshingress.toolmetadata;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.WeakHashMap;

public class McpToolMetadata {

    private static final String RESOURCES_DIRECTORY = "resources";
    private static final String APPLICATION_PROPERTIES = "application.properties";
    private static final String README = "README.md";

    private final ClassMappedMcpToolArtifactDirectoryResolver resolver;
    private final Map<Class<?>, CachedToolMetadata> cache = new WeakHashMap<>();
    private final Map<Class<?>, McpToolNativeMetadata> registeredManifests = new WeakHashMap<>();

    public McpToolMetadata() {
        this(new ClassMappedMcpToolArtifactDirectoryResolver());
    }

    public McpToolMetadata(McpToolArtifactDirectoryResolver resolver) {
        this.resolver = resolver instanceof ClassMappedMcpToolArtifactDirectoryResolver mappedResolver
                ? mappedResolver
                : new ClassMappedMcpToolArtifactDirectoryResolver(resolver);
    }

    public void registerArtifactDirectory(Class<?> toolClass, Path artifactDirectory) {
        resolver.register(toolClass, artifactDirectory);
        evict(toolClass);
    }

    public synchronized void registerManifest(Class<?> toolClass, McpToolManifestDefinition manifest) {
        registerManifest(toolClass, McpToolNativeMetadata.fromManifest(manifest));
    }

    public synchronized void registerManifest(Class<?> toolClass, McpToolNativeMetadata metadata) {
        if (toolClass == null) {
            throw new IllegalArgumentException("toolClass must not be null");
        }
        registeredManifests.put(toolClass, metadata == null ? McpToolNativeMetadata.empty() : metadata);
        evict(toolClass);
    }

    public List<McpToolPropertyMetadata> toolProperties(Class<?> toolClass) {
        return metadata(toolClass).properties();
    }

    public McpToolPropertyMetadata toolProperty(Class<?> toolClass, String name) {
        return findToolProperty(toolClass, name)
                .orElseThrow(() -> new IllegalArgumentException("No tool property registered for " + name));
    }

    public Optional<McpToolPropertyMetadata> findToolProperty(Class<?> toolClass, String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String propertyName = name.strip();
        return metadata(toolClass).properties().stream()
                .filter(property -> property.name().equals(propertyName))
                .findFirst();
    }

    public String toolReadme(Class<?> toolClass) {
        return metadata(toolClass).readme();
    }

    public synchronized void evict(Class<?> toolClass) {
        if (toolClass != null) {
            cache.remove(toolClass);
        }
    }

    public synchronized void clearCache() {
        cache.clear();
    }

    private synchronized CachedToolMetadata metadata(Class<?> toolClass) {
        if (toolClass == null) {
            throw new IllegalArgumentException("toolClass must not be null");
        }
        return cache.computeIfAbsent(toolClass, this::loadMetadata);
    }

    private CachedToolMetadata loadMetadata(Class<?> toolClass) {
        Path artifactDirectory = resolver.artifactDirectory(toolClass).orElse(null);
        Map<String, String> assignedValues = artifactDirectory == null
                ? Map.of()
                : readApplicationProperties(artifactDirectory.resolve(RESOURCES_DIRECTORY).resolve(APPLICATION_PROPERTIES));
        McpToolNativeMetadata manifest = artifactDirectory == null
                ? registeredManifest(toolClass)
                : readManifest(artifactDirectory).orElseGet(() -> registeredManifest(toolClass));
        List<McpToolPropertyMetadata> properties = declaredProperties(manifest, assignedValues);
        String readme = artifactDirectory == null ? "" : readReadme(artifactDirectory);
        if (readme.isBlank()) {
            readme = new ToolReadmeResolver().resolve(manifest.readme());
        }
        return new CachedToolMetadata(properties, readme);
    }

    private McpToolNativeMetadata registeredManifest(Class<?> toolClass) {
        synchronized (this) {
            return registeredManifests.getOrDefault(toolClass, McpToolNativeMetadata.empty());
        }
    }

    private Optional<McpToolNativeMetadata> readManifest(Path artifactDirectory) {
        for (String manifestPath : List.of(McpToolManifestJson.RESOURCES_MANIFEST_PATH, McpToolManifestJson.MANIFEST_PATH)) {
            Path candidate = artifactDirectory.resolve(manifestPath).normalize();
            if (candidate.startsWith(artifactDirectory.normalize()) && Files.isRegularFile(candidate)) {
                return Optional.of(McpToolManifestJson.read(candidate));
            }
        }
        return Optional.empty();
    }

    private List<McpToolPropertyMetadata> declaredProperties(McpToolNativeMetadata manifest, Map<String, String> assignedValues) {
        Map<String, McpToolPropertyMetadata> properties = new LinkedHashMap<>();
        for (ToolProperty property : manifest.properties()) {
            String assignedValue = assignedValues.getOrDefault(property.name(), "");
            properties.putIfAbsent(property.name(), new McpToolPropertyMetadata(
                    property.name(),
                    property.description(),
                    property.defaultValue(),
                    property.valueType(),
                    property.required(),
                    assignedValue
            ));
        }
        for (Map.Entry<String, String> entry : assignedValues.entrySet()) {
            properties.putIfAbsent(entry.getKey(), new McpToolPropertyMetadata(
                    entry.getKey(),
                    "",
                    "",
                    "string",
                    false,
                    entry.getValue()
            ));
        }
        return properties.values().stream()
                .sorted(Comparator.comparing(McpToolPropertyMetadata::name))
                .toList();
    }

    private Map<String, String> readApplicationProperties(Path applicationProperties) {
        if (!Files.isRegularFile(applicationProperties)) {
            return Map.of();
        }
        try {
            Map<String, String> values = new LinkedHashMap<>();
            Properties properties = new Properties();
            try (var reader = Files.newBufferedReader(applicationProperties, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            properties.forEach((name, value) -> values.put(name.toString(), value.toString()));
            return Map.copyOf(values);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read tool application metadata from " + applicationProperties, exception);
        }
    }

    private String readReadme(Path artifactDirectory) {
        Path resourcesReadme = artifactDirectory.resolve(RESOURCES_DIRECTORY).resolve(README);
        Path exportedReadme = artifactDirectory.resolve(README);
        try {
            if (Files.isRegularFile(resourcesReadme)) {
                return Files.readString(resourcesReadme).strip();
            }
            if (Files.isRegularFile(exportedReadme)) {
                return Files.readString(exportedReadme).strip();
            }
            return "";
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read tool README metadata from " + artifactDirectory, exception);
        }
    }

    private record CachedToolMetadata(
            List<McpToolPropertyMetadata> properties,
            String readme
    ) {
        private CachedToolMetadata {
            properties = properties == null ? List.of() : List.copyOf(properties);
            readme = readme == null ? "" : readme.strip();
        }
    }
}
