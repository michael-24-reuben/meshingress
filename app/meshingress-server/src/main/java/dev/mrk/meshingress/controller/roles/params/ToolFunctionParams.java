package dev.mrk.meshingress.controller.roles.params;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

@Schema(description = "Function descriptor nested inside a dynamic MCP tool descriptor.")
public record ToolFunctionParams(
        @Schema(description = "Public MCP function name.", example = "architect.entries.copy", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "Human-readable function title.", example = "List architect entries")
        String title,
        @Schema(description = "Human-readable function description.", example = "List architect entries through a dynamic alias.")
        String description,
        @Schema(description = "Function version. Defaults to the parent tool version or 1.", example = "1")
        Integer version,
        @Schema(description = "Whether the function is enabled. Defaults to true when omitted.", example = "true")
        Boolean enabled,
        @Schema(description = "Function visibility. Defaults to the parent tool visibility when omitted.", allowableValues = {"public", "private", "admin"}, example = "public")
        String visibility,
        @Schema(description = "Handler key used by the runtime registry to route calls.", example = "architect.entries.list", requiredMode = Schema.RequiredMode.REQUIRED)
        String handlerKey,
        @Schema(description = "JSON Schema object describing accepted input arguments.", type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE, example = "{\"type\":\"object\"}")
        JsonNode inputSchema,
        @Schema(description = "Optional JSON Schema object describing structured output.", type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
        JsonNode outputSchema,
        @Schema(description = "Optional MCP annotations object such as readOnlyHint or destructiveHint.", type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
        JsonNode annotations
) {
}

