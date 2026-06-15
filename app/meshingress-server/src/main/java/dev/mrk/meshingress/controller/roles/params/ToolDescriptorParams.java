package dev.mrk.meshingress.controller.roles.params;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Schema(description = "Dynamic MCP tool descriptor accepted by role-gated registry methods.")
public record ToolDescriptorParams(
        @Schema(description = "Public MCP tool name.", example = "architect.entries.copy", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "Human-readable tool title.", example = "List architect entries")
        String title,
        @Schema(description = "Human-readable tool description.", example = "List architect entries through a dynamic alias.", requiredMode = Schema.RequiredMode.REQUIRED)
        String description,
        @Schema(description = "Descriptor version. Defaults to 1 when omitted.", example = "1")
        Integer version,
        @Schema(description = "Whether the tool is enabled. Defaults to true when omitted.", example = "true")
        Boolean enabled,
        @Schema(description = "Registry visibility for the tool.", allowableValues = {"public", "private", "admin"}, example = "public")
        String visibility,
        @Schema(description = "Handler key used by the runtime registry to route calls.", example = "architect.entries.list", requiredMode = Schema.RequiredMode.REQUIRED)
        String handlerKey,
        @Schema(description = "JSON Schema object describing accepted input arguments.", type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE, example = "{\"type\":\"object\",\"properties\":{\"status\":{\"type\":\"string\"}}}")
        JsonNode inputSchema,
        @Schema(description = "Optional JSON Schema object describing structured output.", type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
        JsonNode outputSchema,
        @Schema(description = "Optional MCP annotations object such as readOnlyHint or destructiveHint.", type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
        JsonNode annotations,
        @Schema(description = "Optional function-level descriptors. If omitted, the tool descriptor is used as a single function.")
        List<ToolFunctionParams> functions
) {
}

