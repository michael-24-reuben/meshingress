package dev.mrk.meshingress.toolmetadata;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class McpToolNativeMetadataExtractor {

    public McpToolNativeMetadata extract(Path quarantineDirectory) {
        if (quarantineDirectory == null || !Files.isDirectory(quarantineDirectory)) {
            return new McpToolNativeMetadata(List.of(), "");
        }

        Map<String, McpToolPropertyDefinition> properties = new LinkedHashMap<>();
        StringBuilder readme = new StringBuilder();
        CachingMetadataReaderFactory readerFactory = new CachingMetadataReaderFactory();
        try (var stream = Files.walk(quarantineDirectory)) {
            for (Path classFile : stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".class"))
                    .sorted()
                    .toList()) {
                MetadataReader reader = readerFactory.getMetadataReader(new FileSystemResource(classFile));
                AnnotationMetadata annotations = reader.getAnnotationMetadata();
                collectProperties(properties, annotations);
                collectReadme(readme, annotations, quarantineDirectory);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to infer native tool metadata: " + exception.getMessage(), exception);
        }
        return new McpToolNativeMetadata(List.copyOf(properties.values()), readme.toString());
    }

    private void collectProperties(Map<String, McpToolPropertyDefinition> properties, AnnotationMetadata annotations) {
        for (AnnotationAttributes attributes : annotations.getMergedRepeatableAnnotationAttributes(
                McpToolProperty.class,
                McpToolProperties.class,
                false,
                false
        )) {
            McpToolPropertyDefinition definition = new McpToolPropertyDefinition(
                    attributes.getString("name"),
                    attributes.getString("description"),
                    attributes.getString("defaultValue"),
                    attributes.getString("valueType"),
                    attributes.getBoolean("required")
            );
            properties.putIfAbsent(definition.name(), definition);
        }
    }

    private void collectReadme(StringBuilder readme, AnnotationMetadata annotations, Path quarantineDirectory) throws Exception {
        Map<String, Object> attributes = annotations.getAnnotationAttributes(McpToolReadme.class.getName(), false);
        if (attributes == null) {
            return;
        }
        String value = stringAttribute(attributes, "value");
        if (!value.isBlank()) {
            appendReadme(readme, value);
            return;
        }
        String resource = stringAttribute(attributes, "resource");
        if (!resource.isBlank()) {
            Path resourcePath = quarantineDirectory.resolve(resource).normalize();
            if (!resourcePath.startsWith(quarantineDirectory.normalize())) {
                throw new IllegalStateException("README resource escapes artifact quarantine: " + resource);
            }
            if (Files.isRegularFile(resourcePath)) {
                appendReadme(readme, Files.readString(resourcePath));
            }
        }
    }

    private void appendReadme(StringBuilder readme, String value) {
        if (readme.isEmpty()) {
            readme.append(value.strip());
        } else {
            readme.append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append(value.strip());
        }
    }

    private String stringAttribute(Map<String, Object> attributes, String name) {
        Object value = attributes.get(name);
        return value == null ? "" : value.toString();
    }
}
