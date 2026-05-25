package dev.mrk.meshingress.api.tools.function;

import dev.mrk.meshingress.api.tools.McpToolPatch;
import dev.mrk.meshingress.api.tools.ToolVisibility;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public record McpFunctionDescriptor(
        String name, // join tool name and function name. Like `toolName + "." + functionName`
        String title,
        String description,
        int version,
        boolean enabled,
        ToolVisibility visibility,
        String handlerKey,
        JsonNode inputSchema,
        JsonNode outputSchema,
        JsonNode annotations,
        boolean dynamic
) {

    public @NonNull ObjectNode toMcpJson(@NonNull ObjectMapper objectMapper) {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("name", name);
        if (title != null && !title.isBlank()) {
            tool.put("title", title);
        }
        tool.put("description", description == null ? "" : description);
        tool.set("inputSchema", inputSchema == null ? objectMapper.createObjectNode() : inputSchema);
        if (annotations != null && annotations.isObject()) {
            tool.set("annotations", annotations);
        }
        return tool;
    }

    @Contract("_ -> new")
    public @NonNull McpFunctionDescriptor withVersion(int nextVersion) {
        return new McpFunctionDescriptor(
                name,
                title,
                description,
                nextVersion,
                enabled,
                visibility,
                handlerKey,
                inputSchema,
                outputSchema,
                annotations,
                dynamic
        );
    }

    @Contract("_, _ -> new")
    public @NonNull McpFunctionDescriptor withPatch(@NonNull McpToolPatch patch, int nextVersion) {
        return new McpFunctionDescriptor(
                name,
                patch.title() == null ? title : patch.title(),
                patch.description() == null ? description : patch.description(),
                nextVersion,
                patch.enabled() == null ? enabled : patch.enabled(),
                patch.visibility() == null ? visibility : patch.visibility(),
                patch.handlerKey() == null ? handlerKey : patch.handlerKey(),
                patch.inputSchema() == null ? inputSchema : patch.inputSchema(),
                patch.outputSchema() == null ? outputSchema : patch.outputSchema(),
                patch.annotations() == null ? annotations : patch.annotations(),
                dynamic
        );
    }


    public @NonNull String toString() {
        return "FunctionDescriptor{" +
                "name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", version=" + version +
                ", enabled=" + enabled +
                ", visibility=" + visibility +
                ", handlerKey='" + handlerKey + '\'' +
                ", inputSchema=" + inputSchema +
                ", outputSchema=" + outputSchema +
                ", annotations=" + annotations +
                ", dynamic=" + dynamic +
                '}';
    }
}
