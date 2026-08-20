package dev.mrk.meshingress.controller.roles.params;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Schema(description = "Dynamic MCP tool descriptor accepted by role-gated registry methods.")
public record ToolDescriptorParams(
        @Schema(description = "Public MCP tool-family name: <namespace>.<tool>.", example = "architect.entries", requiredMode = Schema.RequiredMode.REQUIRED)
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
        @Schema(description = "Handler key for a single-function shorthand descriptor. Multi-function descriptors declare handler keys on their functions.", example = "architect.entries.list")
        String handlerKey,
        @Schema(description = "JSON Schema object describing accepted input arguments.", type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE, example = "{\"type\":\"object\",\"properties\":{\"status\":{\"type\":\"string\"}}}")
        JsonNode inputSchema,
        @Schema(description = "Optional JSON Schema object describing structured output.", type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
        JsonNode outputSchema,
        @Schema(description = "Optional MCP annotations object such as readOnlyHint or destructiveHint.", type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
        JsonNode annotations,
        @Schema(description = "Function-level descriptors. Each function name must be <namespace>.<tool>.<function>.")
        List<ToolFunctionParams> functions
) {
}

