package dev.mrk.meshingress.api.services;

import dev.mrk.meshingress.api.tools.ToolVisibility;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

/** Public discovery metadata for a non-callable typed service provider. */
public record McpServiceDescriptor(
        String name,
        String title,
        String description,
        int version,
        boolean enabled,
        ToolVisibility visibility,
        List<String> provides,
        List<McpServiceRequirementDescriptor> requires,
        JsonNode inputSchema
) {
    public McpServiceDescriptor {
        provides = provides == null ? List.of() : List.copyOf(provides);
        requires = requires == null ? List.of() : List.copyOf(requires);
    }

    public ObjectNode toMcpJson(ObjectMapper objectMapper) {
        ObjectNode service = objectMapper.createObjectNode();
        service.put("name", name);
        if (title != null && !title.isBlank()) {
            service.put("title", title);
        }
        service.put("description", description == null ? "" : description);
        service.put("version", version);
        service.put("visibility", visibility.toWire());
        ArrayNode providedContracts = objectMapper.createArrayNode();
        provides.forEach(providedContracts::add);
        service.set("provides", providedContracts);
        service.set("requires", requirementsJson(objectMapper));
        service.set("inputSchema", inputSchema == null ? objectMapper.createObjectNode() : inputSchema);
        return service;
    }

    private ArrayNode requirementsJson(ObjectMapper objectMapper) {
        ArrayNode requirements = objectMapper.createArrayNode();
        for (McpServiceRequirementDescriptor requirement : requires) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("type", requirement.type());
            ArrayNode selectedProviders = objectMapper.createArrayNode();
            requirement.oneOf().forEach(selectedProviders::add);
            node.set("oneOf", selectedProviders);
            requirements.add(node);
        }
        return requirements;
    }
}
