package dev.mrk.meshingress.artifact.scope;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record CycloneDxSbom(
        String bomFormat,
        String specVersion,
        int version,
        Component metadataComponent,
        List<Component> components
) {
    public static final String DEFAULT_BOM_FORMAT = "CycloneDX";
    public static final String DEFAULT_SPEC_VERSION = "1.6";

    public CycloneDxSbom(Component metadataComponent, List<Component> components) {
        this(DEFAULT_BOM_FORMAT, DEFAULT_SPEC_VERSION, 1, metadataComponent, components);
    }

    public CycloneDxSbom {
        bomFormat = bomFormat == null || bomFormat.isBlank() ? DEFAULT_BOM_FORMAT : bomFormat;
        specVersion = specVersion == null || specVersion.isBlank() ? DEFAULT_SPEC_VERSION : specVersion;
        if (version < 1) {
            version = 1;
        }
        components = components == null ? List.of() : List.copyOf(components);
    }

    public int componentCount() {
        return components.size();
    }

    public String artifactName() {
        return metadataComponent == null ? "" : metadataComponent.name();
    }

    public String artifactSha256() {
        return metadataComponent == null ? "" : metadataComponent.sha256();
    }

    public Map<String, Object> summary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("format", bomFormat);
        summary.put("specVersion", specVersion);
        summary.put("componentCount", componentCount());
        summary.put("dependencyComponentCount", dependencyComponentCount());
        summary.put("artifactName", artifactName());
        summary.put("artifactSha256", artifactSha256());
        return summary;
    }

    public long dependencyComponentCount() {
        return components.stream()
                .filter(Component::isDependency)
                .count();
    }

    public Map<String, Object> toDocument() {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("bomFormat", bomFormat);
        document.put("specVersion", specVersion);
        document.put("version", version);
        document.put("metadata", metadataDocument());
        document.put("components", components.stream()
                .map(Component::toDocument)
                .toList());
        return document;
    }

    public String toJson(ObjectMapper objectMapper) throws IOException {
        return objectMapper.writeValueAsString(toDocument());
    }

    private Map<String, Object> metadataDocument() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        Map<String, Object> tools = new LinkedHashMap<>();
        List<Map<String, Object>> toolComponents = new ArrayList<>();
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "application");
        tool.put("name", "meshingress-artifact-scope-scanner");
        tool.put("version", "0.0.1-SNAPSHOT");
        toolComponents.add(tool);
        tools.put("components", toolComponents);
        metadata.put("tools", tools);
        if (metadataComponent != null) {
            metadata.put("component", metadataComponent.toDocument());
        }
        return metadata;
    }

    public record Component(
            String type,
            String bomRef,
            String group,
            String name,
            String version,
            String purl,
            String scope,
            Long size,
            String sha256,
            List<Property> properties
    ) {
        public Component(String type, String bomRef, String name, Long size, String sha256, List<Property> properties) {
            this(type, bomRef, null, name, null, null, null, size, sha256, properties);
        }

        public Component {
            type = type == null || type.isBlank() ? "file" : type;
            bomRef = bomRef == null ? "" : bomRef;
            group = group == null ? "" : group;
            name = name == null ? "" : name;
            version = version == null ? "" : version;
            purl = purl == null ? "" : purl;
            scope = scope == null ? "" : scope;
            properties = properties == null ? List.of() : List.copyOf(properties);
        }

        public Map<String, Object> toDocument() {
            Map<String, Object> component = new LinkedHashMap<>();
            component.put("type", type);
            component.put("bom-ref", bomRef);
            if (!group.isBlank()) {
                component.put("group", group);
            }
            component.put("name", name);
            if (!version.isBlank()) {
                component.put("version", version);
            }
            if (!purl.isBlank()) {
                component.put("purl", purl);
            }
            if (!scope.isBlank()) {
                component.put("scope", scope);
            }
            if (size != null && size >= 0) {
                component.put("size", size);
            }
            if (sha256 != null && !sha256.isBlank()) {
                component.put("hashes", List.of(hashDocument(sha256)));
            }
            if (!properties.isEmpty()) {
                component.put("properties", properties.stream()
                        .map(Property::toDocument)
                        .toList());
            }
            return component;
        }

        private Map<String, Object> hashDocument(String value) {
            Map<String, Object> hash = new LinkedHashMap<>();
            hash.put("alg", "SHA-256");
            hash.put("content", value);
            return hash;
        }

        boolean isDependency() {
            return properties.stream()
                    .anyMatch(property -> property.name().equals("meshingress:component-role")
                            && property.value().equals("maven-dependency"));
        }
    }

    public record Property(String name, String value) {
        public Property {
            name = name == null ? "" : name;
            value = value == null ? "" : value;
        }

        public Map<String, Object> toDocument() {
            Map<String, Object> property = new LinkedHashMap<>();
            property.put("name", name);
            property.put("value", value);
            return property;
        }
    }
}
