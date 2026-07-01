package dev.mrk.meshingress.toolmetadata;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public class McpToolMetadata {

    private static final String RESOURCES_DIRECTORY = "resources";
    private static final String APPLICATION_YAML = "application.yaml";
    private static final String README = "README.md";

    private final ClassMappedMcpToolArtifactDirectoryResolver resolver;
    private final Map<Class<?>, CachedToolMetadata> cache = new WeakHashMap<>();

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
                : readApplicationYaml(artifactDirectory.resolve(RESOURCES_DIRECTORY).resolve(APPLICATION_YAML));
        List<McpToolPropertyMetadata> properties = declaredProperties(toolClass, assignedValues);
        String readme = artifactDirectory == null ? "" : readReadme(artifactDirectory);
        if (readme.isBlank()) {
            readme = annotationReadme(toolClass);
        }
        return new CachedToolMetadata(properties, readme);
    }

    private List<McpToolPropertyMetadata> declaredProperties(Class<?> toolClass, Map<String, String> assignedValues) {
        Map<String, McpToolPropertyMetadata> properties = new LinkedHashMap<>();
        for (McpToolProperty property : toolClass.getAnnotationsByType(McpToolProperty.class)) {
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

    private Map<String, String> readApplicationYaml(Path applicationYaml) {
        if (!Files.isRegularFile(applicationYaml)) {
            return Map.of();
        }
        try {
            Map<String, String> values = new LinkedHashMap<>();
            for (String line : Files.readAllLines(applicationYaml, StandardCharsets.UTF_8)) {
                String trimmed = line.strip();
                if (trimmed.isBlank() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf(':');
                if (separator <= 0) {
                    continue;
                }
                String name = trimmed.substring(0, separator).strip();
                String value = trimmed.substring(separator + 1).strip();
                if (!name.isBlank()) {
                    values.put(name, unquoteYamlScalar(value));
                }
            }
            return Map.copyOf(values);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read tool application metadata from " + applicationYaml, exception);
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

    private String annotationReadme(Class<?> toolClass) {
        McpToolReadme readme = toolClass.getAnnotation(McpToolReadme.class);
        if (readme == null) {
            return "";
        }
        if (!readme.value().isBlank()) {
            return readme.value().strip();
        }
        if (readme.resource().isBlank()) {
            return "";
        }
        String resource = readme.resource().startsWith("/")
                ? readme.resource().substring(1)
                : readme.resource();
        try (InputStream input = toolClass.getClassLoader().getResourceAsStream(resource)) {
            return input == null ? "" : new String(input.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read tool README resource " + readme.resource(), exception);
        }
    }

    private String unquoteYamlScalar(String value) {
        if (value.length() < 2 || value.charAt(0) != '"' || value.charAt(value.length() - 1) != '"') {
            return value;
        }
        List<Character> characters = new ArrayList<>();
        boolean escaping = false;
        for (int index = 1; index < value.length() - 1; index++) {
            char current = value.charAt(index);
            if (escaping) {
                characters.add(current);
                escaping = false;
            } else if (current == '\\') {
                escaping = true;
            } else {
                characters.add(current);
            }
        }
        StringBuilder unquoted = new StringBuilder(characters.size());
        for (Character character : characters) {
            unquoted.append(character);
        }
        return unquoted.toString();
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
