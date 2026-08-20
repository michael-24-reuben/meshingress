package dev.mrk.meshingress.api.tools;

import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

public record McpToolDescriptor(
        String name,
        String label,
        String description,
        int version,
        boolean enabled,
        ToolVisibility visibility,
        List<McpFunctionDescriptor> functions,
        JsonNode annotations,
        boolean dynamic
) {
    public McpToolDescriptor {
        functions = functions == null ? List.of() : List.copyOf(functions);
    }

    public @NonNull ObjectNode toMcpJson(@NonNull ObjectMapper objectMapper) {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("name", name);
        if (label != null && !label.isBlank()) {
            tool.put("title", label);
        }
        tool.put("description", description == null ? "" : description);
        ArrayNode functionNodes = objectMapper.createArrayNode();
        for (McpFunctionDescriptor function : functions) {
            functionNodes.add(function.toMcpJson(objectMapper));
        }
        tool.set("functions", functionNodes);
        if (annotations != null && annotations.isObject()) {
            tool.set("annotations", annotations);
        }
        return tool;
    }

    @Contract("_ -> new")
    public @NonNull McpToolDescriptor withVersion(int nextVersion) {
        return new McpToolDescriptor(
                name,
                label,
                description,
                nextVersion,
                enabled,
                visibility,
                functions,
                annotations,
                dynamic
        );
    }

    @Contract("_, _ -> new")
    public @NonNull McpToolDescriptor withPatch(@NonNull McpToolPatch patch, int nextVersion) {
        return new McpToolDescriptor(
                name,
                patch.title() == null ? label : patch.title(),
                patch.description() == null ? description : patch.description(),
                nextVersion,
                patch.enabled() == null ? enabled : patch.enabled(),
                patch.visibility() == null ? visibility : patch.visibility(),
                functions,
                patch.annotations() == null ? annotations : patch.annotations(),
                dynamic
        );
    }

    @Contract("_ -> new")
    public @NonNull McpToolDescriptor withFunctions(@NonNull List<McpFunctionDescriptor> nextFunctions) {
        return new McpToolDescriptor(
                name,
                label,
                description,
                version,
                enabled,
                visibility,
                nextFunctions,
                annotations,
                dynamic
        );
    }

    public @NonNull String toString() {
        return "ToolDescriptor{" +
                "name='" + name + '\'' +
                ", title='" + label + '\'' +
                ", description='" + description + '\'' +
                ", version=" + version +
                ", enabled=" + enabled +
                ", visibility=" + visibility +
                ", functions=" + functions +
                ", annotations=" + annotations +
                ", dynamic=" + dynamic +
                '}';
    }
}
